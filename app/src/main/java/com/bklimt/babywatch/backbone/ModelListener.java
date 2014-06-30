package com.bklimt.babywatch.backbone;

public interface ModelListener<T extends Model> {
    void onChanged(T model, String key, Object oldValue, Object newValue);
}
