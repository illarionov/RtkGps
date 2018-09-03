package gpsplus.rtkgps.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.BindView;
import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.constants.StationPositionType;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;


public class StationPositionActivity extends Activity {

    private static final boolean DBG = BuildConfig.DEBUG & false;
    static final String TAG = StationPositionActivity.class.getSimpleName();

    public static final String ARG_SHARED_PREFS_NAME = "shared_prefs_name";

    public static final String ARG_HIDE_USE_RTCM = "hide_use_rtcm";

    public static final String SHARED_PREFS_KEY_POSITION_FORMAT = "station_position_format";

    public static final String SHARED_PREFS_KEY_POSITION_TYPE = "station_position_type";

    public static final String SHARED_PREFS_KEY_STATION_X = "station_position_x";

    public static final String SHARED_PREFS_KEY_STATION_Y = "station_position_y";

    public static final String SHARED_PREFS_KEY_STATION_Z = "station_position_z";

    private static final double SHARED_PREFS_XYZ_MULT = 0.0001;

    private static final Pattern sLlhDmsPattern = Pattern.compile(
            "([+-]?\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{1,2}(?:\\.\\d+)?)");

    public static class Value {

        private StationPositionType mType;

        private final Position3d mPosition;

        public Value() {
            mType = StationPositionType.RTCM_POS;
            mPosition = new Position3d();
        }

        public Value(StationPositionType type, Position3d position) {
            this();
            mType = type;
            mPosition.setValues(position);
        }

        public StationPositionType getType() {
            return mType;
        }

        public Position3d getPosition() {
            return new Position3d(mPosition);
        }

        public Value copy() {
            return new Value(mType, mPosition);
        }
    }

    private String mSharedPrefsName;
    private boolean mHideUseRtcm;

    private ArrayAdapter<PositionFormat> mPositionFormatAdapter;

    private PositionFormat mCurrentFormat;

    private StationPositionType mPositionType;

    @BindView(R.id.use_rtcm_antenna_position) CheckBox mUseRtcmPosition;
    @BindView(R.id.position_format) Spinner mPositionFormatView;
    @BindView(R.id.coord1_title) TextView mCoord1TitleView;
    @BindView(R.id.coord2_title) TextView mCoord2TitleView;
    @BindView(R.id.coord3_title) TextView mCoord3TitleView;
    @BindView(R.id.coord1) EditText mCoord1View;
    @BindView(R.id.coord2) EditText mCoord2View;
    @BindView(R.id.coord3) EditText mCoord3View;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_station_position);

        final Intent intent = getIntent();
        mSharedPrefsName = intent.getStringExtra(ARG_SHARED_PREFS_NAME);
        if (mSharedPrefsName == null) {
            throw new IllegalArgumentException("ARG_SHARED_PREFS_NAME not defined");
        }
        mHideUseRtcm = intent.getBooleanExtra(ARG_HIDE_USE_RTCM, false);

        ButterKnife.bind(this);

        createPositionFormatAdapter();
        mPositionFormatView.setAdapter(mPositionFormatAdapter);
        loadSettings();
        mPositionFormatView.setOnItemSelectedListener(mOnFormatSelectedListener);
        if (!mHideUseRtcm) {
            mUseRtcmPosition.setOnCheckedChangeListener(mOnCheckedChangeListener);
        }else {
            mUseRtcmPosition.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCurrentFormat = (PositionFormat)mPositionFormatView.getSelectedItem();
        mPositionType = mUseRtcmPosition.isChecked() ? StationPositionType.RTCM_POS
                : StationPositionType.POS_IN_PRCOPT;
    }

    public void onCancelButtonClicked(View v) {
        finish();
    }

    public void onOkButtonClicked(View v) {
        saveSettings();
        finish();
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        prefs.edit()
            .putString(SHARED_PREFS_KEY_POSITION_FORMAT, PositionFormat.LLH.toString())
            .putString(SHARED_PREFS_KEY_POSITION_TYPE, value.mType.toString())
            .putLong(SHARED_PREFS_KEY_STATION_X, Math.round(value.mPosition.getX()/SHARED_PREFS_XYZ_MULT))
            .putLong(SHARED_PREFS_KEY_STATION_Y, Math.round(value.mPosition.getY()/SHARED_PREFS_XYZ_MULT))
            .putLong(SHARED_PREFS_KEY_STATION_Z, Math.round(value.mPosition.getZ()/SHARED_PREFS_XYZ_MULT))
        .commit();

    }

    @Nonnull
    public static Value readSettings(SharedPreferences prefs) {
        Position3d position;
        StationPositionType type;

        try {
            position = new Position3d(
                    prefs.getLong(SHARED_PREFS_KEY_STATION_X, 0)*SHARED_PREFS_XYZ_MULT,
                    prefs.getLong(SHARED_PREFS_KEY_STATION_Y, 0)*SHARED_PREFS_XYZ_MULT,
                    prefs.getLong(SHARED_PREFS_KEY_STATION_Z, 0)*SHARED_PREFS_XYZ_MULT
                    );
            type = StationPositionType.valueOf(prefs.getString(SHARED_PREFS_KEY_POSITION_TYPE,
                    StationPositionType.POS_IN_PRCOPT.name()));
        }catch (ClassCastException cce) {
            position = new Position3d(0, 0, 0);
            type = StationPositionType.POS_IN_PRCOPT;
        }

        return new Value(type, position);
    }

    public static String readSummary(Resources r, SharedPreferences prefs) {
        final Value settings;
        final Position3d pos;

        settings = readSettings(prefs);
        if (settings.mType == StationPositionType.RTCM_POS) {
            return r.getString(StationPositionType.RTCM_POS.getNameResId());
        }else {
            pos = RtkCommon.ecef2pos(settings.mPosition);
            return String.format(Locale.US, "%s, %s, %.3f",
                    RtkCommon.Deg2Dms.toString(Math.toDegrees(pos.getLat()), true),
                    RtkCommon.Deg2Dms.toString(Math.toDegrees(pos.getLon()), false),
                    pos.getHeight());
        }
    }


    private void loadSettings() {
        final SharedPreferences prefs;
        Value settings;
        PositionFormat format;

        prefs = getSharedPreferences(mSharedPrefsName, MODE_PRIVATE);

        try {
            format = PositionFormat.valueOf(prefs.getString(SHARED_PREFS_KEY_POSITION_FORMAT,
                    PositionFormat.LLH.name()));
        }catch (IllegalArgumentException iae) {
            iae.printStackTrace();
            format = PositionFormat.LLH;
        }

        settings = readSettings(prefs);

        mPositionFormatView.setSelection(mPositionFormatAdapter.getPosition(format));
        setCoordinates(settings.mPosition, format);

        mUseRtcmPosition.setChecked(!mHideUseRtcm && (settings.mType == StationPositionType.RTCM_POS));
        onUseRtcmChanged(settings.mType == StationPositionType.RTCM_POS);
    }

    private void saveSettings() {
        final Position3d ecefPos;

        ecefPos = readEcefPosition();

        getSharedPreferences(mSharedPrefsName, MODE_PRIVATE)
            .edit()
            .putString(SHARED_PREFS_KEY_POSITION_FORMAT, mCurrentFormat.name())
            .putString(SHARED_PREFS_KEY_POSITION_TYPE, mPositionType.name())
            .putLong(SHARED_PREFS_KEY_STATION_X, Math.round(ecefPos.getX()/SHARED_PREFS_XYZ_MULT))
            .putLong(SHARED_PREFS_KEY_STATION_Y, Math.round(ecefPos.getY()/SHARED_PREFS_XYZ_MULT))
            .putLong(SHARED_PREFS_KEY_STATION_Z, Math.round(ecefPos.getZ()/SHARED_PREFS_XYZ_MULT))
            .commit();
    }

    private void createPositionFormatAdapter() {
        mPositionFormatAdapter = new ArrayAdapter<PositionFormat>(this, android.R.layout.simple_dropdown_item_1line) {

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
        mPositionFormatAdapter.addAll(PositionFormat.values());
        mPositionFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private void onUseRtcmChanged(boolean checked) {
        if (checked && !mHideUseRtcm) {
            mPositionType = StationPositionType.RTCM_POS;
        }else {
            mPositionType = StationPositionType.POS_IN_PRCOPT;
        }

        mPositionFormatView.setEnabled(!checked);
        mCoord1TitleView.setEnabled(!checked);
        mCoord2TitleView.setEnabled(!checked);
        mCoord3TitleView.setEnabled(!checked);
        mCoord1View.setEnabled(!checked);
        mCoord2View.setEnabled(!checked);
        mCoord3View.setEnabled(!checked);
    }

    private void setCoordinates(Position3d ecefPos, PositionFormat format) {
        final int coordsArrId;
        Position3d newPos;
        final String coordsArr[];

        switch (format) {
        case ECEF:
            coordsArrId = R.array.solution_view_coordinates_ecef;
            newPos = ecefPos;
            mCoord1View.setText(String.format(Locale.US, "%.4f", newPos.getX()));
            mCoord2View.setText(String.format(Locale.US, "%.4f",newPos.getY()));
            mCoord3View.setText(String.format(Locale.US, "%.4f",newPos.getZ()));
            break;
        case LLH:
            coordsArrId = R.array.solution_view_coordinates_wgs84;
            newPos = RtkCommon.ecef2pos(ecefPos);
            mCoord1View.setText(String.format(Locale.US, "%.9f", Math.toDegrees(newPos.getLat())));
            mCoord2View.setText(String.format(Locale.US, "%.9f", Math.toDegrees(newPos.getLon())));
            mCoord3View.setText(String.format(Locale.US, "%.4f", newPos.getHeight()));
            break;
        case LLH_DMS:
            coordsArrId = R.array.solution_view_coordinates_wgs84;
            newPos = RtkCommon.ecef2pos(ecefPos);
            mCoord1View.setText(formatLlhDdms(newPos.getLat()));
            mCoord2View.setText(formatLlhDdms(newPos.getLon()));
            mCoord3View.setText(String.format(Locale.US, "%.4f", newPos.getHeight()));
            break;
        default:
            throw new IllegalStateException();
        }

        coordsArr = getResources().getStringArray(coordsArrId);
        mCoord1TitleView.setText(coordsArr[0]);
        mCoord2TitleView.setText(coordsArr[1]);
        mCoord3TitleView.setText(coordsArr[2]);

        mCurrentFormat = format;
    }

    /**
     * @return ECEF prosition from fields
     */
    private Position3d readEcefPosition() {
        Position3d res;
        double x,y,z;
        double lat,lon,height;
        String cord1, cord2, cord3;

        cord1 = mCoord1View.getText().toString();
        cord2 = mCoord2View.getText().toString();
        cord3 = mCoord3View.getText().toString();

        switch (mCurrentFormat) {
        case ECEF:
            try {
                x=Double.valueOf(cord1);
            }catch (NumberFormatException nfe) {
                x=0;
            }
            try {
                y=Double.valueOf(cord2);
            }catch (NumberFormatException nfe) {
                y=0;
            }
            try {
                z=Double.valueOf(cord3);
            }catch (NumberFormatException nfe) {
                z=0;
            }

            res = new Position3d(x, y, z);
            break;
        case LLH:
            try {
                lat=Math.toRadians(Double.valueOf(cord1));
            }catch (NumberFormatException nfe) {
                lat=0;
            }
            try {
                lon=Math.toRadians(Double.valueOf(cord2));
            }catch (NumberFormatException nfe) {
                lon=0;
            }
            try {
                height=Double.valueOf(cord3);
            }catch (NumberFormatException nfe) {
                height=0;
            }

            res = RtkCommon.pos2ecef(lat, lon, height, null);
            break;
        case LLH_DMS:
            lat = scanLlhDdms(cord1);
            lon = scanLlhDdms(cord2);
            try {
                height = Double.valueOf(cord3);
            }catch (NumberFormatException nfe) {
                height=0;
            }
            res = RtkCommon.pos2ecef(lat, lon, height, null);
            break;
        default:
            throw new IllegalStateException();
        }
        return res;
    }

    /**
     *
     * @param llhDms
     * @return radians
     */
    private double scanLlhDdms(String llhDms) {
        double res;
        double deg, min, sec, sign;

        Matcher m = sLlhDmsPattern.matcher(llhDms);
        if (!m.matches()) return 0.0f;

        deg = Double.valueOf(m.group(1));
        min = Double.valueOf(m.group(2));
        sec = Double.valueOf(m.group(3));

        sign = Math.signum(deg);
        res = sign * (Math.abs(deg) + min/60.0 + sec/3600.0);

        return Math.toRadians(res);
    }

    /**
     * @param val radians
     * @return
     */
    private String formatLlhDdms(double val) {
        RtkCommon.Deg2Dms dms;
        val = Math.toDegrees(val);
        val += Math.signum(val)*1.0E-12;
        dms = new RtkCommon.Deg2Dms(val);
        return String.format(Locale.US, "%.0f %02.0f %09.6f", dms.degree, dms.minute, dms.second);
    }

    private OnItemSelectedListener mOnFormatSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position,
                long id) {
            final PositionFormat newFormat;

            newFormat = (PositionFormat)parent.getItemAtPosition(position);

            if (DBG) Log.v(TAG, "onItemSelected() " + mCurrentFormat + " -> " + newFormat);

            setCoordinates(readEcefPosition(), newFormat);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            onUseRtcmChanged(isChecked);
        }
    };

    private static enum PositionFormat {

        LLH(R.string.station_position_format_llh),

        LLH_DMS(R.string.station_position_format_llh_dms),

        ECEF(R.string.station_position_format_ecef);

        private final int mTitleId;

        private PositionFormat(int titleId) {
            mTitleId = titleId;
        }
    }

}
