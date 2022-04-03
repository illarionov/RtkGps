package gpsplus.rtkgps;

import static junit.framework.Assert.assertNotNull;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import gpsplus.rtkgps.view.GTimeView;
import gpsplus.rtkgps.view.GpsSkyView;
import gpsplus.rtkgps.view.SnrView;
import gpsplus.rtkgps.view.SolutionView;
import gpsplus.rtkgps.view.SolutionView.Format;
import gpsplus.rtkgps.view.StreamIndicatorsView;
import gpsplus.rtklib.RtkControlResult;
import gpsplus.rtklib.RtkServerObservationStatus;
import gpsplus.rtklib.RtkServerStreamStatus;

import java.util.Timer;
import java.util.TimerTask;

public class StatusFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;

    static final String TAG = StatusFragment.class.getSimpleName();

    public static final String PREF_TIME_FORMAT = "StatusFragment.PREF_TIME_FORMAT";

    public static final String PREF_SOLUTION_FORMAT = "StatusFragment.PREF_SOLUTION_FORMAT";

    private static final String KEY_CURRENT_STATUS_VIEW = "StatusFragment.currentStatusView";

    private Timer mStreamStatusUpdateTimer;
    private RtkServerStreamStatus mStreamStatus;
    private final RtkServerObservationStatus mRoverObservationStatus, mBaseObservationStatus;
    private RtkControlResult mRtkStatus;

    @BindView(R.id.streamIndicatorsView) StreamIndicatorsView mStreamIndicatorsView;
    @BindView(R.id.gtimeView) GTimeView mGTimeView;
    @BindView(R.id.solutionView) SolutionView mSolutionView;
    @BindView(R.id.streamStatus) TextView mStreamStatusView;

    @BindView(R.id.status_view_spinner) Spinner mStatusViewSpinner;
    @BindView(R.id.status_view_container) ViewGroup mStatusViewContainer;
    @BindView(R.id.Sky) GpsSkyView mSkyView;
    @BindView(R.id.Snr1) SnrView mSnr1View;
    @BindView(R.id.Snr2) SnrView mSnr2View;

    private ArrayAdapter<StatusView> mStatusViewSpinnerAdapter;

    private StatusView mCurrentStatusView;

    public StatusFragment() {
        mStreamStatus = new RtkServerStreamStatus();
        mRoverObservationStatus = new RtkServerObservationStatus();
        mBaseObservationStatus = new RtkServerObservationStatus();
        mRtkStatus = new RtkControlResult();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_status, container, false);
        ButterKnife.bind(this, v);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
      //  ButterKnife.reset(this);
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

        mStatusViewSpinnerAdapter = new ArrayAdapter<StatusView>(getActivity(),
                R.layout.select_solution_view_item) {

            @Override
            public View getDropDownView(int position, View convertView,
                    ViewGroup parent) {
                final View v = super.getDropDownView(position, convertView, parent);
                ((TextView)v).setText(getItem(position).mTitleId);
                return v;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final View v = super.getView(position, convertView, parent);
                ((TextView)v).setText(getItem(position).mTitleId);
                return v;
            }
        };
        mStatusViewSpinnerAdapter.addAll(StatusView.values());
        mStatusViewSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStatusViewSpinner.setAdapter(mStatusViewSpinnerAdapter);

        mStatusViewSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                if (DBG) Log.v(TAG, "onItemSelected() " + position);
                StatusView newView = mStatusViewSpinnerAdapter.getItem(position);
                if (mCurrentStatusView != newView) {
                    setStatusView(newView);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        if (savedInstanceState == null) {
            setStatusView(StatusView.SNR);
        }else {
            mCurrentStatusView = StatusView.valueOf(savedInstanceState.getString(KEY_CURRENT_STATUS_VIEW));
        }
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_STATUS_VIEW, mCurrentStatusView.name());
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

    private void setStatusView(StatusView statusView) {
        mCurrentStatusView = statusView;

        switch (statusView) {
        //case BASELINE:
        case SKYPLOT_BASE_L1:
        case SKYPLOT_BASE_L2:
        case SKYPLOT_BASE_L5:
        case SKYPLOT_ROVER_L1:
        case SKYPLOT_ROVER_L2:
        case SKYPLOT_ROVER_L5:
            mSkyView.setVisibility(View.VISIBLE);
            mSnr1View.setVisibility(View.GONE);
            mSnr2View.setVisibility(View.GONE);
            break;
        case SNR:
        case SNR_L1:
        case SNR_L2:
        case SNR_L5:
            mSkyView.setVisibility(View.GONE);
            mSnr1View.setVisibility(View.VISIBLE);
            mSnr2View.setVisibility(View.VISIBLE);
            break;
        default:
            throw new IllegalStateException();

        }

        switch (statusView) {
        // case BASELINE:
            //break;
        case SKYPLOT_BASE_L1:
        case SKYPLOT_ROVER_L1:
            mSkyView.setFreqBand(GpsSkyView.BAND_L1);
            break;
        case SKYPLOT_BASE_L2:
        case SKYPLOT_ROVER_L2:
            mSkyView.setFreqBand(GpsSkyView.BAND_L2);
            break;
        case SKYPLOT_BASE_L5:
        case SKYPLOT_ROVER_L5:
            mSkyView.setFreqBand(GpsSkyView.BAND_L5);
            break;
        case SNR:
            mSnr1View.setFreqBand(SnrView.BAND_ANY);
            mSnr2View.setFreqBand(SnrView.BAND_ANY);
            break;
        case SNR_L1:
            mSnr1View.setFreqBand(SnrView.BAND_L1);
            mSnr2View.setFreqBand(SnrView.BAND_L1);
            break;
        case SNR_L2:
            mSnr1View.setFreqBand(SnrView.BAND_L2);
            mSnr2View.setFreqBand(SnrView.BAND_L2);
            break;
        case SNR_L5:
            mSnr1View.setFreqBand(SnrView.BAND_L5);
            mSnr2View.setFreqBand(SnrView.BAND_L5);
            break;
        default:
            break;
        }
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
            rtks.getBaseObservationStatus(mBaseObservationStatus);
            rtks.getRtkStatus(mRtkStatus);
            serverStatus = rtks.getServerStatus();
        }

        assertNotNull(mStreamStatus.mMsg);
        mStreamStatusView.setText(mStreamStatus.mMsg);

        mStreamIndicatorsView.setStats(mStreamStatus, serverStatus);
        mSolutionView.setStats(mRtkStatus);
        mGTimeView.setTime(mRoverObservationStatus.getTime());

        switch (mCurrentStatusView) {
        //case BASELINE:
        case SKYPLOT_BASE_L1:
        case SKYPLOT_BASE_L2:
        case SKYPLOT_BASE_L5:
            mSkyView.setStats(mBaseObservationStatus);
            break;
        case SKYPLOT_ROVER_L1:
        case SKYPLOT_ROVER_L2:
        case SKYPLOT_ROVER_L5:
            mSkyView.setStats(mRoverObservationStatus);
            break;
        case SNR:
        case SNR_L1:
        case SNR_L2:
        case SNR_L5:
            mSnr1View.setStats(mRoverObservationStatus);
            mSnr2View.setStats(mBaseObservationStatus);
            break;
        default:
            throw new IllegalStateException();
        }

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

    public static enum StatusView {

        SNR(R.string.status_view_snr),

        SNR_L1(R.string.status_view_snr_l1),

        SNR_L2(R.string.status_view_snr_l2),

        SNR_L5(R.string.status_view_snr_l5),

        SKYPLOT_ROVER_L1(R.string.status_view_skyplot_rover_l1),

        SKYPLOT_ROVER_L2(R.string.status_view_skyplot_rover_l2),

        SKYPLOT_ROVER_L5(R.string.status_view_skyplot_rover_l5),

        SKYPLOT_BASE_L1(R.string.status_view_skyplot_base_l1),

        SKYPLOT_BASE_L2(R.string.status_view_skyplot_base_l2),

        SKYPLOT_BASE_L5(R.string.status_view_skyplot_base_l5),

        //BASELINE(R.string.status_view_baseline)

        ;

        final int mTitleId;

        private StatusView(int titleId) {
            mTitleId = titleId;
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