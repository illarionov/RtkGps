package ru0xdc.rtkgps.view;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtklib.RtkCommon;
import ru0xdc.rtklib.RtkCommon.Deg2Dms;
import ru0xdc.rtklib.RtkCommon.Position3d;
import ru0xdc.rtklib.RtkControlResult;
import ru0xdc.rtklib.Solution;
import ru0xdc.rtklib.constants.SolutionStatus;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

public class SolutionView extends TableLayout {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = SolutionView.class.getSimpleName();


    public enum Format {
        WGS84(0,
                R.string.solution_view_format_wgs84,
                R.array.solution_view_coordinates_wgs84),

        WGS84_FLOAT(1,
                R.string.solution_view_format_wgs84_float,
                R.array.solution_view_coordinates_wgs84),

        ECEF(2,
                R.string.solution_view_format_ecef,
                R.array.solution_view_coordinates_ecef),

        ENU_BASELINE(3,
                R.string.solution_view_format_enu_baseline,
                R.array.solution_view_coordinates_baseline_enu),

        PYL_BASELINE(4,
                R.string.solution_view_format_pyl_baseline,
                R.array.solution_view_coordinates_baseline_pyl)

        ;

        final int mStyledAttributeValue;
        final int mHeadersArrayId;
        final int mDescriptionId;

        private Format(int styledAttributeValue, int descriptionId, int HeadersArrayId) {
            mStyledAttributeValue = styledAttributeValue;
            mHeadersArrayId = HeadersArrayId;
            mDescriptionId = descriptionId;
        }

        static Format valueOfStyledAttr(int val) {
            for (Format f: Format.values()) {
                if (f.mStyledAttributeValue == val)
                    return f;
            }
            throw new IllegalArgumentException();
        }

        public int getDescriptionResId() {
            return mDescriptionId;
        }
    }

    public static final Format DEFAULT_SOLUTION_FORMAT = Format.WGS84;

    private final static int DEFAULT_COLOR_STATE_CLOSE = Color.DKGRAY;

    private Format mSolutionFormat;

    private final NumberFormat mCoordEcefFormatter;

    private final TextView mTextViewSolutionStatus;

    private final SolutionIndicatorView mSolutionIndicatorView;

    private final TextView mTextViewCoord1Name, mTextViewCoord1Value;
    private final TextView mTextViewCoord2Name, mTextViewCoord2Value;
    private final TextView mTextViewCoord3Name, mTextViewCoord3Value;

    private final TextView mTextViewCovariance;
    private final TextView mTextViewAge;

    public SolutionView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // process style attributes
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SolutionView, 0, 0);

        try {
            final int formatVal = a.getInt(R.styleable.SolutionView_format,
                    DEFAULT_SOLUTION_FORMAT.mStyledAttributeValue
                    );
            mSolutionFormat = Format.valueOfStyledAttr(formatVal);
        }finally {
            a.recycle();
        }

        inflate(context, R.layout.view_solution, this);

        mTextViewSolutionStatus = (TextView)findViewById(R.id.solution_status);
        mSolutionIndicatorView = (SolutionIndicatorView)findViewById(R.id.solution_indicator_view);
        mTextViewCoord1Name = (TextView)findViewById(R.id.coord1_name);
        mTextViewCoord1Value = (TextView)findViewById(R.id.coord1_value);
        mTextViewCoord2Name = (TextView)findViewById(R.id.coord2_name);
        mTextViewCoord2Value = (TextView)findViewById(R.id.coord2_value);
        mTextViewCoord3Name = (TextView)findViewById(R.id.coord3_name);
        mTextViewCoord3Value = (TextView)findViewById(R.id.coord3_value);
        mTextViewCovariance = (TextView)findViewById(R.id.covariance_text);
        mTextViewAge = (TextView)findViewById(R.id.age_text);

        mCoordEcefFormatter = new DecimalFormat("0.000",
                DecimalFormatSymbols.getInstance(Locale.US));

        updateCoordinatesHeader();
        clearCoordinates();
    }

    public void setStats(RtkControlResult status) {
        final Solution sol = status.getSolution();
        mTextViewSolutionStatus.setText(sol.getSolutionStatus().getNameResId());
        mSolutionIndicatorView.setStatus(sol.getSolutionStatus());
        updateCoordinates(status);
        updateAgeText(sol);
    }

    public void setFormat(Format format) {
        if (format != mSolutionFormat) {
            mSolutionFormat = format;
            updateCoordinatesHeader();
            clearCoordinates();
        }
    }

    public Format getFormat() {
        return mSolutionFormat;
    }

    private void updateCoordinates(RtkControlResult rtk) {
        RtkCommon.Position3d roverPos;
        double Qe[];
        final Solution sol = rtk.getSolution();
        final Position3d roverEcefPos;

        roverEcefPos = sol.getPosition();

        switch (mSolutionFormat) {
        case WGS84:
        case WGS84_FLOAT:
            String strLat, strLon, strHeight;
            RtkCommon.Matrix3x3 cov;

            if (RtkCommon.norm(roverEcefPos.getValues()) <= 0.0) {
                break;
            }

            roverPos = RtkCommon.ecef2pos(roverEcefPos);
            cov = sol.getQrMatrix();
            Qe = RtkCommon.covenu(roverPos.getLat(), roverPos.getLon(), cov).getValues();
            if (mSolutionFormat == Format.WGS84) {
                strLat = Deg2Dms.toString(Math.toDegrees(roverPos.getLat()), true);
                strLon = Deg2Dms.toString(Math.toDegrees(roverPos.getLon()), false);
            }else {
                strLat = String.format(Locale.US, "%11.8f°", Math.toDegrees(roverPos.getLat()));
                strLon = String.format(Locale.US, "%11.8f°", Math.toDegrees(roverPos.getLon()));
            }
            strHeight = String.format(Locale.US, "%.3fm el.", roverPos.getHeight());

            mTextViewCoord1Value.setText(strLat);
            mTextViewCoord2Value.setText(strLon);
            mTextViewCoord3Value.setText(strHeight);
            mTextViewCovariance.setText(String.format(
                    Locale.US,
                    "N:%6.3f\nE:%6.3f\nU:%6.3f m",
                    Math.sqrt(Qe[4] < 0 ? 0 : Qe[4]),
                    Math.sqrt(Qe[0] < 0 ? 0 : Qe[0]),
                    Math.sqrt(Qe[8] < 0 ? 0 : Qe[8])
                    ));
            break;
        case ECEF:
            final float[] qr = sol.getPositionVariance();
            mTextViewCoord1Value.setText(mCoordEcefFormatter.format(roverEcefPos.getX()));
            mTextViewCoord2Value.setText(mCoordEcefFormatter.format(roverEcefPos.getY()));
            mTextViewCoord3Value.setText(mCoordEcefFormatter.format(roverEcefPos.getZ()));
            mTextViewCovariance.setText(String.format(
                    Locale.US,
                    "X:%6.3f\nY:%6.3f\nZ:%6.3f m",
                    Math.sqrt(qr[0] < 0 ? 0 : qr[0]),
                    Math.sqrt(qr[1] < 0 ? 0 : qr[1]),
                    Math.sqrt(qr[2] < 0 ? 0 : qr[2])
                    ));
            break;
        case ENU_BASELINE:
        case PYL_BASELINE:
            final Position3d baseEcefPos;
            final Position3d baselineVector;
            final Position3d enu;
            final Position3d basePos;
            double baselineLen;
            String v1, v2, v3;

            baseEcefPos = rtk.getBasePosition();

            baselineVector = new Position3d(
                    roverEcefPos.getX() - baseEcefPos.getX(),
                    roverEcefPos.getY() - baseEcefPos.getY(),
                    roverEcefPos.getZ() - baseEcefPos.getZ()
                    );

            baselineLen = baselineVector.getNorm();
            if (baselineLen <= 0.0) {
                break;
            }

            basePos = RtkCommon.ecef2pos(baseEcefPos);
            enu = RtkCommon.ecef2enu(basePos.getLat(), basePos.getLon(), baselineVector);
            cov = sol.getQrMatrix();
            Qe = RtkCommon.covenu(basePos.getLat(), basePos.getLon(), cov).getValues();

            if (mSolutionFormat == Format.ENU_BASELINE) {
                v1 = String.format(Locale.US, "%.3f m", enu.getLat());
                v2 = String.format(Locale.US, "%.3f m", enu.getLon());
                v3 = String.format(Locale.US, "%.3f m", enu.getHeight());
            }else {
                double pitch, yaw;
                pitch = Math.asin(enu.getHeight()/baselineLen);
                yaw = Math.atan2(enu.getLat(), enu.getLon());
                if (yaw < 0.0) yaw += 2.0 * Math.PI;

                v1 = String.format(Locale.US, "%.3f °", Math.toDegrees(pitch));
                v2 = String.format(Locale.US, "%.3f °", Math.toDegrees(yaw));
                v3 = String.format(Locale.US, "%.3f m", baselineLen);
            }

            mTextViewCoord1Value.setText(v1);
            mTextViewCoord2Value.setText(v2);
            mTextViewCoord3Value.setText(v3);
            mTextViewCovariance.setText(String.format(
                    Locale.US,
                    "E:%6.3f\nN:%6.3f\nU:%6.3f m",
                    Math.sqrt(Qe[0] < 0 ? 0 : Qe[0]),
                    Math.sqrt(Qe[4] < 0 ? 0 : Qe[4]),
                    Math.sqrt(Qe[8] < 0 ? 0 : Qe[8])
                    ));
            break;
        default:
            throw new IllegalStateException();
        }
    }

    private void clearCoordinates() {
        mTextViewCoord1Value.setText("");
        mTextViewCoord2Value.setText("");
        mTextViewCoord3Value.setText("");
    }

    private void updateAgeText(Solution sol) {
        String format = getResources().getString(R.string.solution_view_age_text_format);

        mTextViewAge.setText(String.format(
                Locale.US,
                format,
                sol.getAge(),
                sol.getRatio(),
                sol.getNs()
                ));

    }

    private void updateCoordinatesHeader() {
        final String headers[];

        if (isInEditMode()) {
            headers = new String[] {"Lat:", "Lon:", "height:"};
        }else {
            headers = getResources().getStringArray(mSolutionFormat.mHeadersArrayId);
        }

        mTextViewCoord1Name.setText(headers[0]);
        mTextViewCoord2Name.setText(headers[1]);
        mTextViewCoord3Name.setText(headers[2]);
    }

    public static class SolutionIndicatorView extends View {

        private final static int DEFAULT_INDICATOR_WIDTH = 9;

        private final static int DEFAULT_INDICATOR_HEIGHT = 9;

        private final Paint mIndicatorPaint;

        private float mIndicatorWidth, mIndicatorHeight;

        private SolutionStatus mStatus;

        public SolutionIndicatorView(Context context, AttributeSet attrs) {
            super(context, attrs);

            final float density = getResources().getDisplayMetrics().density;

            mIndicatorWidth = DEFAULT_INDICATOR_WIDTH * density;
            mIndicatorHeight = DEFAULT_INDICATOR_HEIGHT * density;

            mStatus = SolutionStatus.NONE;

            mIndicatorPaint = new Paint();
            mIndicatorPaint.setColor(DEFAULT_COLOR_STATE_CLOSE);
        }

        public void setStatus(SolutionStatus status) {
            mStatus = status;
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(measureWidth(widthMeasureSpec),
                    measureHeight(heightMeasureSpec));
        }

        private int measureWidth(int measureSpec) {
            int result = 0;
            int specMode = MeasureSpec.getMode(measureSpec);
            int specSize = MeasureSpec.getSize(measureSpec);

            if (specMode == MeasureSpec.EXACTLY) {
                // We were told how big to be
                result = specSize;
            } else {
                // Measure the text
                result = (int)(mIndicatorWidth + getPaddingLeft() + getPaddingRight());
                if (specMode == MeasureSpec.AT_MOST) {
                    // Respect AT_MOST value if that was what is called for by measureSpec
                    result = Math.min(result, specSize);
                }
            }

            return result;
        }

        private int measureHeight(int measureSpec) {
            int result = 0;
            int specMode = MeasureSpec.getMode(measureSpec);
            int specSize = MeasureSpec.getSize(measureSpec);

            if (specMode == MeasureSpec.EXACTLY) {
                // We were told how big to be
                result = specSize;
            } else {
                // Measure the text (beware: ascent is a negative number)
                result = (int)(mIndicatorHeight + getPaddingTop() + getPaddingBottom());
                if (specMode == MeasureSpec.AT_MOST) {
                    // Respect AT_MOST value if that was what is called for by measureSpec
                    result = Math.min(result, specSize);
                }
            }
            return result;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float x, y;

            super.onDraw(canvas);

            x = getPaddingTop();
            y = getPaddingLeft();

            mIndicatorPaint.setColor(getIndicatorColor());
            canvas.drawRect(x, y, x+mIndicatorWidth,
                    y+mIndicatorHeight, mIndicatorPaint);

        }

        protected int getIndicatorColor() {
            int c;

            switch (mStatus) {
            case NONE:
                c = DEFAULT_COLOR_STATE_CLOSE;
                break;
            case FIX:
                c = Color.GREEN;
                break;
            case FLOAT:
                c = Color.rgb(255, 127, 0);
                break;
            case SBAS:
                c = Color.rgb(255, 0, 255);
                break;
            case DGPS:
                c = Color.BLUE;
                break;
            case SINGLE:
                c = Color.RED;
                break;
            case PPP:
                c = Color.rgb(0, 128, 128);
                break;
            case DR:
                c = Color.YELLOW;
                break;
            default:
                c = Color.WHITE;
                break;
            }
            return c;
        }


    }

}
