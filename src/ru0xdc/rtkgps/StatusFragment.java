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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

public class StatusFragment extends Fragment {

	@SuppressWarnings("unused")
	private static final boolean DBG = BuildConfig.DEBUG & true;

	static final String TAG = StatusFragment.class.getSimpleName();

	private Timer mStreamStatusUpdateTimer;
	private RtkServerStreamStatus mStreamStatus;
	private RtkServerObservationStatus mRoverObservationStatus;
	private RtkControlResult mRtkStatus;

	private GpsSkyView mSkyView;
	private SnrView mSnrView;
	private StreamIndicatorsView mStreamIndicatorsView;
	private GTimeView mGTimeView;
	private SolutionView mSolutionView;

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
		mSkyView = (GpsSkyView)v.findViewById(R.id.Sky);
		mSnrView = (SnrView)v.findViewById(R.id.Snr);
		mStreamIndicatorsView = (StreamIndicatorsView)v.findViewById(R.id.streamIndicatorsView);
		mGTimeView = (GTimeView)v.findViewById(R.id.gtimeView);
		mSolutionView = (SolutionView)v.findViewById(R.id.solutionView);
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

		final SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

		// GTimeView format
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

		// SolutionView format
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

	private void showSelectSolutionViewDialog() {
    	SelectSolutionViewFormatDialog.newInstance(mSolutionView.getFormat())
	 	.show(getChildFragmentManager(),
			 "Select Solution View Format Dialog");
	}

	private void showSelectTimeFormatDialog() {
    	SelectTimeFormatDialog.newInstance(mGTimeView.getTimeFormat())
	 	.show(getChildFragmentManager(),
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
		mSkyView.setStats(mRoverObservationStatus);
		mSnrView.setStats(mRoverObservationStatus);
		mStreamIndicatorsView.setStats(mStreamStatus, serverStatus);
		mSolutionView.setStats(mRtkStatus);
		mGTimeView.setTime(mRoverObservationStatus.time);
	}

	public void setGTimeFormat(GTimeView.Format newFormat) {
		mGTimeView.setTimeFormat(newFormat);
	}

	public void setSolutionFormat(SolutionView.Format newFormat) {
		mSolutionView.setFormat(newFormat);
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

			final StatusFragment parent = (StatusFragment)getParentFragment();
			if (parent != null) {
				parent.setGTimeFormat(newFormat);
			}
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

			final StatusFragment parent = (StatusFragment)getParentFragment();
			if (parent != null) {
				parent.setSolutionFormat(newFormat);
			}
			dismiss();
		}
	}

}