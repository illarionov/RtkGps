package ru0xdc.rtkgps;

import static junit.framework.Assert.assertNotNull;

import java.util.Timer;
import java.util.TimerTask;

import ru0xdc.rtkgps.view.GTimeView;
import ru0xdc.rtkgps.view.GpsSkyView;
import ru0xdc.rtkgps.view.SnrView;
import ru0xdc.rtkgps.view.SolutionView;
import ru0xdc.rtkgps.view.SolutionView.Format;
import ru0xdc.rtkgps.view.StreamIndicatorsView;
import ru0xdc.rtklib.RtkControlResult;
import ru0xdc.rtklib.RtkServerObservationStatus;
import ru0xdc.rtklib.RtkServerStreamStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;

public class StatusFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;

    static final String TAG = StatusFragment.class.getSimpleName();

    private Timer mStreamStatusUpdateTimer;
    private RtkServerStreamStatus mStreamStatus;
    private RtkServerObservationStatus mRoverObservationStatus;
    private RtkControlResult mRtkStatus;

    @InjectView(R.id.Sky) GpsSkyView mSkyView;
    @InjectView(R.id.Snr) SnrView mSnrView;
    @InjectView(R.id.streamIndicatorsView) StreamIndicatorsView mStreamIndicatorsView;
    @InjectView(R.id.gtimeView) GTimeView mGTimeView;
    @InjectView(R.id.solutionView) SolutionView mSolutionView;
    @InjectView(R.id.streamStatus) TextView mStreamStatusView;

    public static final String PREF_TIME_FORMAT = "StatusFragment.PREF_TIME_FORMAT";

    public static final String PREF_SOLUTION_FORMAT = "StatusFragment.PREF_SOLUTION_FORMAT";

    public StatusFragment() {
        mStreamStatus = new RtkServerStreamStatus();
        mRoverObservationStatus = new RtkServerObservationStatus();
        mRtkStatus = new RtkControlResult();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Create a new TextView and set its text to the fragment's section
        // number argument value.
        View v = inflater.inflate(R.layout.fragment_status, container, false);
        Views.inject(this, v);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // long click listeners
        mGTimeView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSelectTimeFormatDialog();
                return true;
            }
        });

        mSolutionView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSelectSolutionViewDialog();
                return true;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        mStreamStatusUpdateTimer = new Timer();
        mStreamStatusUpdateTimer.scheduleAtFixedRate(
                new TimerTask() {
                    Runnable updateStatusRunnable = new Runnable() {
                        @Override
                        public void run() {
                            StatusFragment.this.updateStatus();
                        }
                    };
                    @Override
                    public void run() {
                        Activity a = getActivity();
                        if (a == null) return;
                        a.runOnUiThread(updateStatusRunnable);
                    }
                }, 200, 250);
    }

    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        updateGTimeFormat(prefs);
        updateSolutionFormat(prefs);

        prefs.registerOnSharedPreferenceChangeListener(mPrefsChangedListener);
    }

    @Override
    public void onPause() {
        final SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsChangedListener);

        super.onPause();
    }

    @Override
    public void onStop() {
        mStreamStatusUpdateTimer.cancel();
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_status, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_select_solution_format:
            showSelectSolutionViewDialog();
            break;
        case R.id.menu_select_gtime_format:
            showSelectTimeFormatDialog();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private final OnSharedPreferenceChangeListener mPrefsChangedListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (PREF_TIME_FORMAT.equals(key)) {
                updateGTimeFormat(sharedPreferences);
            }else if (PREF_SOLUTION_FORMAT.equals(key)) {
                updateSolutionFormat(sharedPreferences);
            }
        }
    };

    private void showSelectSolutionViewDialog() {
        SelectSolutionViewFormatDialog.newInstance(mSolutionView.getFormat())
        .show(getActivity().getFragmentManager(),
                "Select Solution View Format Dialog");
    }

    private void showSelectTimeFormatDialog() {
        SelectTimeFormatDialog.newInstance(mGTimeView.getTimeFormat())
        .show(getActivity().getFragmentManager(),
                "Select Time Format Dialog");
    }

    void updateStatus() {
        MainActivity ma;
        RtkNaviService rtks;
        int serverStatus;

        ma = (MainActivity)getActivity();

        if (ma == null) return;

        rtks = ma.getRtkService();
        if (rtks == null) {
            serverStatus = RtkServerStreamStatus.STATE_CLOSE;
            // mRoverObservationStatus.clear();
            mStreamStatus.clear();
        }else {
            rtks.getStreamStatus(mStreamStatus);
            rtks.getRoverObservationStatus(mRoverObservationStatus);
            rtks.getRtkStatus(mRtkStatus);
            serverStatus = rtks.getServerStatus();
        }

        assertNotNull(mStreamStatus.mMsg);
        mStreamStatusView.setText(mStreamStatus.mMsg);
        mSkyView.setStats(mRoverObservationStatus);
        mSnrView.setStats(mRoverObservationStatus);
        mStreamIndicatorsView.setStats(mStreamStatus, serverStatus);
        mSolutionView.setStats(mRtkStatus);
        mGTimeView.setTime(mRoverObservationStatus.time);
    }

    void updateGTimeFormat(SharedPreferences prefs) {
        try {
            String timeFormat = prefs.getString(PREF_TIME_FORMAT, null);
            if (timeFormat != null) {
                mGTimeView.setTimeFormat(GTimeView.Format.valueOf(timeFormat));
            }
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }
    }

    void updateSolutionFormat(SharedPreferences prefs) {
        try {
            final String solutionFormat = prefs.getString(PREF_SOLUTION_FORMAT, null);
            if (solutionFormat != null) {
                mSolutionView.setFormat(Format.valueOf(solutionFormat));
            }
        }catch(ClassCastException cce) {
            cce.printStackTrace();
        }catch(IllegalArgumentException iae) {
            iae.printStackTrace();
        }
    }

    public static class SelectTimeFormatDialog extends DialogFragment {

        public static SelectTimeFormatDialog newInstance(GTimeView.Format selectedFormat) {
            SelectTimeFormatDialog f = new SelectTimeFormatDialog();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putString("selectedFormat", selectedFormat.name());
            f.setArguments(args);

            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final GTimeView.Format formatValues[];
            final CharSequence items[];
            final AlertDialog.Builder builder;
            final GTimeView.Format selected;

            formatValues = GTimeView.Format.values();
            items = new CharSequence[formatValues.length];

            for (int i=0; i<formatValues.length; ++i) {
                items[i] = getString(formatValues[i].getDescriptionResId());
            }

            selected = GTimeView.Format.valueOf(getArguments().getString("selectedFormat"));

            // Use the Builder class for convenient dialog construction
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.menu_select_gtime_format);
            builder.setSingleChoiceItems(items, selected.ordinal(),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setNewFormat(formatValues[which]);
                }
            });

            // Create the AlertDialog object and return it
            return builder.create();
        }

        void setNewFormat(GTimeView.Format newFormat) {
            final SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_TIME_FORMAT, newFormat.name());
            editor.commit();
            dismiss();
        }
    }

    public static class SelectSolutionViewFormatDialog extends DialogFragment {

        public static SelectSolutionViewFormatDialog newInstance(SolutionView.Format selectedFormat) {
            SelectSolutionViewFormatDialog f = new SelectSolutionViewFormatDialog();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putString("selectedFormat", selectedFormat.name());
            f.setArguments(args);

            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SolutionView.Format formatValues[];
            final CharSequence items[];
            final AlertDialog.Builder builder;
            final SolutionView.Format selected;

            formatValues = SolutionView.Format.values();
            items = new CharSequence[formatValues.length];

            for (int i=0; i<formatValues.length; ++i) {
                items[i] = getString(formatValues[i].getDescriptionResId());
            }

            selected = SolutionView.Format.valueOf(getArguments().getString("selectedFormat"));

            // Use the Builder class for convenient dialog construction
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.menu_select_solution_format);
            builder.setSingleChoiceItems(items, selected.ordinal(),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setNewFormat(formatValues[which]);
                }
            });

            // Create the AlertDialog object and return it
            return builder.create();
        }

        void setNewFormat(SolutionView.Format newFormat) {
            final SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SOLUTION_FORMAT, newFormat.name());
            editor.commit();
            dismiss();
        }
    }

}