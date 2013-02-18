package ru0xdc.rtkgps;

import java.util.Timer;
import java.util.TimerTask;

import ru0xdc.rtklib.RtkServerStreamStatus;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MainActivity extends FragmentActivity implements
ActionBar.OnNavigationListener {

	@SuppressWarnings("unused")
	private static final boolean DBG = BuildConfig.DEBUG & true;

	static final String TAG = MainActivity.class.getSimpleName();

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	RtkNaviService mRtkService;
	boolean mRtkServiceBound = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
					getString(R.string.title_section1),
					getString(R.string.title_section2),
					getString(R.string.title_section3), }), this);

		startRtkService();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mRtkServiceBound) {
			final Intent intent = new Intent(this, RtkNaviService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
	}

	/**
	 * Backward-compatible version of {@link ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			RtkNaviService.RtkNaviServiceBinder binder = (RtkNaviService.RtkNaviServiceBinder) service;
			mRtkService = binder.getService();
			mRtkServiceBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mRtkServiceBound = false;
			mRtkService = null;
		}
	};

	@Override
	protected void onStop() {
		super.onStop();

		// Unbind from the service
		if (mRtkServiceBound) {
			unbindService(mConnection);
			mRtkServiceBound = false;
			mRtkService = null;
		}

	}

	private void startRtkService() {
		if (!mRtkServiceBound) {
			final Intent intent = new Intent(this, RtkNaviService.class);
			startService(intent);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
	}

	private void stopRtkService() {
		if (mRtkServiceBound) {
			mRtkService.stopSelf();
			unbindService(mConnection);
			mRtkServiceBound = false;
			mRtkService = null;
		}
	}

	public RtkNaviService getRtkService() {
		return mRtkService;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean serviceActive = mRtkServiceBound;
		menu.findItem(R.id.menu_start_service).setVisible(!serviceActive);
		menu.findItem(R.id.menu_stop_service).setVisible(serviceActive);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			// TODO
			break;
		case R.id.menu_start_service:
			startRtkService();
			break;
		case R.id.menu_stop_service:
			stopRtkService();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		Fragment fragment;

		// When the given dropdown item is selected, show its contents in the
		// container view.
		switch (position) {
		case 0:
			fragment = new StreamStatusFragment();
			break;
		default:
			fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);
			break;
		}

		getSupportFragmentManager().beginTransaction()
			.replace(R.id.container, fragment).commit();

		return true;
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			// Create a new TextView and set its text to the fragment's section
			// number argument value.
			TextView textView = new TextView(getActivity());
			textView.setGravity(Gravity.CENTER);
			textView.setText(Integer.toString(getArguments().getInt(
					ARG_SECTION_NUMBER)));
			return textView;
		}
	}

	public static class StreamStatusFragment extends Fragment {

		private TextView mTextView;

		private Timer mStreamStatusUpdateTimer;
		private RtkServerStreamStatus mStreamStatus;
		private String mLastStreamStatusStr;

		public StreamStatusFragment() {
			mStreamStatus = new RtkServerStreamStatus();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			// Create a new TextView and set its text to the fragment's section
			// number argument value.
			mTextView = new TextView(getActivity());
			mTextView.setGravity(Gravity.CENTER);
			return mTextView;
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
								StreamStatusFragment.this.updateStreamStatus();
							}
						};
						@Override
						public void run() {
							Activity a = getActivity();
							if (a == null) return;
							a.runOnUiThread(updateStatusRunnable);
						}
					}, 200, 500);
		}


		@Override
		public void onStop() {
			mStreamStatusUpdateTimer.cancel();

			super.onStop();
		}

		void updateStreamStatus() {
			MainActivity ma;
			RtkNaviService rtks;
			String s;

			ma = (MainActivity)getActivity();

			if (ma == null) return;

			rtks = ma.getRtkService();
			if (rtks == null) return;


			rtks.getStreamStatus(mStreamStatus);

			s = mStreamStatus.toString();

			if ( ! TextUtils.equals(mLastStreamStatusStr, s) ) {
				mLastStreamStatusStr = s;
				Log.v(TAG, s);
				mTextView.setText(s);
			}

		}
	}

	static {
		System.loadLibrary("rtkgps");
	}
}
