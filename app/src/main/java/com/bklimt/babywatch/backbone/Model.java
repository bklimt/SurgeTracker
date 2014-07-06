package com.bklimt.babywatch.backbone;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.parse.ParseObject;

import bolts.Capture;

public class Model {
    protected Object lock = new Object();
    private Logger log = Logger.getLogger(getClass().getName());

    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    private ArrayList<ModelListener<? extends Model>> listeners = new ArrayList<ModelListener<? extends Model>>();

    private static final DateFormat iso8601DateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    static {
        iso8601DateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
    }

    public Model() {
    }

    public Object get(String key) {
        synchronized (lock) {
            return attributes.get(key);
        }
    }

    public boolean getBoolean(String key) {
        Boolean value = (Boolean) get(key);
        return value != null && value.booleanValue();
    }

    public Number getNumber(String key) {
        return (Number) get(key);
    }

    public int getInt(String key) {
        Number value = getNumber(key);
        return value != null ? value.intValue() : 0;
    }

    public double getDouble(String key) {
        Number value = getNumber(key);
        return value != null ? value.doubleValue() : 0;
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public Date getDate(String key) {
        return (Date) get(key);
    }

    public Model getModel(String key) {
        return (Model) get(key);
    }

    public Collection getCollection(String key) {
        return (Collection) get(key);
    }

    private void set(String key, Object value, boolean unset) {
        synchronized (lock) {
            if (!(value == null || value instanceof Number || value instanceof Boolean
                    || value instanceof String || value instanceof Date || value instanceof Model
                    || value instanceof Collection || value == JSONObject.NULL)) {
                throw new RuntimeException("Tried to set invalid type on model.");
            }

            if (value instanceof JSONObject) {
                String type = ((JSONObject) value).optString("__type");
                if ("Date".equals("__type")) {
                    try {
                        value = iso8601DateFormat.parse(((JSONObject) value).optString("iso"));
                    } catch (ParseException e) {
                        throw new RuntimeException("Tried to parse an invalid date:" +
                                value.toString());
                    }
                }
            }

            if (value == JSONObject.NULL) {
                value = null;
            }

            Object oldValue = attributes.get(key);
            if (oldValue == value) {
                return;
            }
            if (oldValue != null && oldValue.equals(value)) {
                return;
            }
            if (unset) {
                attributes.remove(key);
            } else {
                attributes.put(key, value);
            }
            notifyChanged(key, oldValue, value);
        }
    }

    public void set(String key, Object value) {
        set(key, value, false);
    }

    public void unset(String key) {
        set(key, null, true);
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        synchronized (lock) {
            for (String key : attributes.keySet()) {
                Object value = attributes.get(key);
                try {
                    if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                        object.put(key, value);
                    } else if (value == null) {
                        object.put(key, JSONObject.NULL);
                    } else if (value instanceof Date) {
                        JSONObject json = new JSONObject();
                        json.putOpt("__type", "Date");
                        json.putOpt("iso", iso8601DateFormat.format(value));
                        object.put(key, json);
                    } else if (value instanceof Model) {
                        object.put(key, ((Model) value).toJSON());
                    } else if (value instanceof Collection) {
                        object.put(key, ((Collection<?>) value).toJSON());
                    } else {
                        throw new RuntimeException("Invalid attribute value in model: " + value);
                    }
                } catch (JSONException jse) {
                    // This is dumb.
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to create JSON.", jse);
                }
            }
        }
        return object;
    }

    public <T extends Model> void addListener(ModelListener<T> listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    public void removeListener(ModelListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    protected void notifyChanged(final String key, final Object oldValue, final Object newValue) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyChanged(key, oldValue, newValue);
                }
            });
            return;
        }

        synchronized (lock) {
            log.info("Firing model change event for " + key + ": " + oldValue + " -> " + newValue);
            ArrayList<ModelListener> listenersCopy = new ArrayList<ModelListener>(listeners);
            for (ModelListener listener : listenersCopy) {
                listener.onChanged(this, key, oldValue, newValue);
            }
        }
    }

    public void bindToEditText(final View view, int id, final String key) {
        synchronized (lock) {
            EditText editText = (EditText) view.findViewById(id);
            editText.setText((String) get(key));

            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    set(key, s.toString());
                }
            };

            editText.addTextChangedListener(textWatcher);

            final WeakReference<EditText> weakEditText = new WeakReference<EditText>(editText);
            final Capture<ModelListener<Model>> listener = new Capture<ModelListener<Model>>();
            listener.set(new ModelListener<Model>() {
                @Override
                public void onChanged(Model model, String key, Object oldValue, Object newValue) {
                    EditText editText = weakEditText.get();
                    if (editText == null) {
                        removeListener(listener.get());
                        return;
                    }

                    if (!editText.getText().toString().equals(newValue)) {
                        editText.setText((String) newValue);
                    }
                }
            });
            addListener(listener.get());
        }
    }

    public void bindToTextView(final View view, int id, final String key) {
        synchronized (lock) {
            TextView textView = (TextView) view.findViewById(id);
            textView.setText((String) get(key));

            final WeakReference<TextView> weakTextView = new WeakReference<TextView>(textView);
            final Capture<ModelListener<Model>> listener = new Capture<ModelListener<Model>>();
            listener.set(new ModelListener<Model>() {
                @Override
                public void onChanged(Model model, String key, Object oldValue, Object newValue) {
                    TextView textView = weakTextView.get();
                    if (textView == null) {
                        removeListener(listener.get());
                        return;
                    }

                    if (!textView.getText().toString().equals(newValue)) {
                        textView.setText((String) newValue);
                    }
                }
            });
            addListener(listener.get());
        }
    }

    public void bindToToggleButton(final View view, int id, final String key) {
        ToggleButton toggleButton = (ToggleButton) view.findViewById(id);
        toggleButton.setChecked(getBoolean(key));
        toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                set(key, isChecked);
            }
        });

        final WeakReference<ToggleButton> weakToggleButton = new WeakReference<ToggleButton>(toggleButton);
        final Capture<ModelListener<Model>> listener = new Capture<ModelListener<Model>>();
        listener.set(new ModelListener<Model>() {
            @Override
            public void onChanged(Model model, String key, Object oldValue, Object newValue) {
                ToggleButton toggleButton = weakToggleButton.get();
                if (toggleButton == null) {
                    removeListener(listener.get());
                    return;
                }

                toggleButton.setChecked(newValue != null && ((Boolean) newValue).booleanValue());
            }
        });
        addListener(listener.get());
    }

    /*
    public void bindToNumberPicker(final View view, int id, final String key) {
        NumberPicker numberPicker = (NumberPicker) view.findViewById(id);
        numberPicker.setValue(getInt(key));
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                set(key, newVal);
            }
        });

        final WeakReference<NumberPicker> weakNumberPicker = new WeakReference<NumberPicker>(numberPicker);
        final Capture<ModelListener<Model>> listener = new Capture<ModelListener<Model>>();
        listener.set(new ModelListener<Model>() {
            @Override
            public void onChanged(Model model, String key, Object oldValue, Object newValue) {
                NumberPicker numberPicker = weakNumberPicker.get();
                if (numberPicker == null) {
                    removeListener(listener.get());
                    return;
                }

                numberPicker.setValue(((Number) newValue).intValue());
            }
        });
        addListener(listener.get());
    }

    public void bindToDatePicker(final View view, int id, final String key) {
        Date date = getDate(key);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        DatePicker datePicker = (DatePicker) view.findViewById(id);
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Date date = getDate(key);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(date);
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        date = calendar.getTime();
                        set(key, date);
                    }
                });

        final WeakReference<DatePicker> weakDatePicker = new WeakReference<DatePicker>(datePicker);
        final Capture<ModelListener<Model>> listener = new Capture<ModelListener<Model>>();
        listener.set(new ModelListener<Model>() {
            @Override
            public void onChanged(Model model, String key, Object oldValue, Object newValue) {
                DatePicker datePicker = weakDatePicker.get();
                if (datePicker == null) {
                    removeListener(listener.get());
                    return;
                }

                Date date = (Date) newValue;
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int monthOfYear = calendar.get(Calendar.MONTH);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                datePicker.init(year, monthOfYear, dayOfMonth, null);
            }
        });
        addListener(listener.get());
    }

    public void bindToTimePicker(final View view, int id, final String key) {
        Date date = getDate(key);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        TimePicker timePicker = (TimePicker) view.findViewById(id);
        timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Date date = getDate(key);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                date = calendar.getTime();
                set(key, date);
            }
        });

        final WeakReference<TimePicker> weakTimePicker = new WeakReference<TimePicker>(timePicker);
        final Capture<ModelListener<Model>> listener = new Capture<ModelListener<Model>>();
        listener.set(new ModelListener<Model>() {
            @Override
            public void onChanged(Model model, String key, Object oldValue, Object newValue) {
                TimePicker timePicker = weakTimePicker.get();
                if (timePicker == null) {
                    removeListener(listener.get());
                    return;
                }

                Date date = (Date) newValue;
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
                timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
            }
        });
        addListener(listener.get());
    }
    */
}
