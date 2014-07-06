package com.bklimt.surgetracker.view;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bklimt.surgetracker.R;
import com.bklimt.surgetracker.model.RootViewModel;
import com.bklimt.surgetracker.model.SurgeParseObject;
import com.parse.ParseAnalytics;

import bolts.Continuation;
import bolts.Task;


public class MainActivity extends Activity implements ActionBar.TabListener {
    private Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter sectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ParseAnalytics.trackAppOpened(getIntent());

        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(sectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        logger.log(Level.INFO, "About to save instance state for bundle: " + outState);
        try {
            super.onSaveInstanceState(outState);
        } catch (NullPointerException npe) {
            logger.log(Level.SEVERE, "NullPointerException in onSaveInstanceState.", npe);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_email) {
            try {
                RootViewModel.get().sendEmail(this);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unable to send email.", e);
                Toast.makeText(MainActivity.this, "Unable to send email.\n" + e, Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_sync) {
            logger.info("Syncing data to the cloud...");
            Toast.makeText(MainActivity.this, "Syncing data to the cloud...", Toast.LENGTH_LONG).show();
            SurgeParseObject.syncAsync().continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        logger.log(Level.SEVERE, "Unable to sync data", task.getError());
                        Toast.makeText(MainActivity.this, "Unable to sync data.\n" + task.getError(), Toast.LENGTH_LONG).show();
                    } else if (task.isCancelled()) {
                        logger.info("Cancelled data sync.");
                        Toast.makeText(MainActivity.this, "Cancelled data sync.", Toast.LENGTH_LONG).show();
                    } else {
                        logger.info("Sync complete.");
                        Toast.makeText(MainActivity.this, "Sync complete.", Toast.LENGTH_LONG).show();
                    }
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return SurgeListFragment.newInstance();
            } else if (position == 1) {
                return AggregateListFragment.newInstance();
            } else if (position == 2) {
                return SurgeGraphFragment.newInstance(false);
            } else if (position == 3) {
                return SurgeGraphFragment.newInstance(true);
            } else {
                throw new RuntimeException("Invalid fragment position: " + position);
            }
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_surges).toUpperCase(l);
                case 1:
                    return getString(R.string.title_overview).toUpperCase(l);
                case 2:
                    return getString(R.string.title_duration).toUpperCase(l);
                case 3:
                    return getString(R.string.title_frequency).toUpperCase(l);
            }
            return null;
        }
    }

}
