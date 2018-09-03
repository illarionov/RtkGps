package gpsplus.rtkgps.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import gpsplus.rtkgps.R;

public class StreamSettingsActivity extends Activity implements
ActionBar.TabListener {

    public static final String ARG_STEAM =  "stream";

    public static final int STREAM_INPUT_SETTINGS = 0;
    public static final int STREAM_OUTPUT_SETTINGS = 1;
    public static final int STREAM_LOG_SETTINGS = 2;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    FragmentPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_stream_settings);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // Show the Up button in the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true);

        final int stream = getIntent().getIntExtra(ARG_STEAM, STREAM_INPUT_SETTINGS);
        switch (stream) {
        case STREAM_INPUT_SETTINGS:
            mSectionsPagerAdapter = new InputStreamSettingsPagerAdapter(
                    getFragmentManager(), getResources());
            setTitle(R.string.title_activity_input_stream_settings);
            break;
        case STREAM_OUTPUT_SETTINGS:
            mSectionsPagerAdapter = new OutputStreamSettingsPagerAdapter(
                    getFragmentManager(), getResources());
            setTitle(R.string.title_activity_output_stream_settings);
            break;
        case STREAM_LOG_SETTINGS:
            mSectionsPagerAdapter = new LogStreamSettingsPagerAdapter(
                    getFragmentManager(), getResources());
            setTitle(R.string.title_activity_log_stream_settings);
            break;
        default:
            throw new IllegalArgumentException("Wrong ARG_STEAM");
        }

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager
        .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab,
            FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab,
            FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab,
            FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private static class InputStreamSettingsPagerAdapter extends FragmentPagerAdapter {

        private final Resources mResources;

        public InputStreamSettingsPagerAdapter(FragmentManager fm, Resources r) {
            super(fm);
            mResources = r;
        }

        @Override
        public Fragment getItem(int position) {
            final Fragment fragment;

            switch (position) {
            case 0:
                fragment = new InputRoverFragment();
                break;
            case 1:
                fragment = new InputBaseFragment();
                break;
            case 2:
                fragment = new InputCorrectionFragment();
                break;
            default:
                throw new IllegalStateException();
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return mResources.getString(R.string.input_streams_settings_rover_tab_title);
            case 1:
                return mResources.getString(R.string.input_streams_settings_base_tab_title);
            case 2:
                return mResources.getString(R.string.input_streams_settings_correction_tab_title);
            }
            return null;
        }
    }

    private static class OutputStreamSettingsPagerAdapter extends FragmentPagerAdapter {

        private final Resources mResources;

        public OutputStreamSettingsPagerAdapter(FragmentManager fm, Resources r) {
            super(fm);
            mResources = r;
        }

        @Override
        public Fragment getItem(int position) {
            final Fragment fragment;

            switch (position) {
            case 0:
                fragment = new OutputSolution1Fragment();
                break;
            case 1:
                fragment = new OutputSolution2Fragment();
                break;
            case 2:
                fragment = new OutputGPXTraceFragment();
                break;
            default:
                throw new IllegalStateException();
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            switch (position) {
            case 0:
                return mResources.getString(R.string.output_streams_settings_solution1_tab_title);
            case 1:
                return mResources.getString(R.string.output_streams_settings_solution2_tab_title);
            case 2:
                return mResources.getString(R.string.output_streams_settings_gpxtrace_tab_title);
            }
            return null;
        }
    }

    private static class LogStreamSettingsPagerAdapter extends FragmentPagerAdapter {

        private final Resources mResources;

        public LogStreamSettingsPagerAdapter(FragmentManager fm, Resources r) {
            super(fm);
            mResources = r;
        }

        @Override
        public Fragment getItem(int position) {
            final Fragment fragment;

            switch (position) {
            case 0:
                fragment = new LogRoverFragment();
                break;
            case 1:
                fragment = new LogBaseFragment();
                break;
            case 2:
                fragment = new LogCorrectionFragment();
                break;
            default:
                throw new IllegalStateException();
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return mResources.getString(R.string.log_stream_settings_rover_tab_title);
            case 1:
                return mResources.getString(R.string.log_stream_settings_base_tab_title);
            case 2:
                return mResources.getString(R.string.log_stream_settings_correction_tab_title);
            }
            return null;
        }
    }

}
