package gpsplus.rtkgps.view;

import java.text.DecimalFormat;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import gpsplus.rtklib.RtkServerObservationStatus;
import gpsplus.rtklib.RtkCommon.Dops;
import gpsplus.rtklib.RtkServerObservationStatus.SatStatus;

public class GpsSkyView extends View {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = GpsSkyView.class.getSimpleName();

    public static final String BAND_L1 = "L1";

    public static final String BAND_L2 = "L2";

    public static final String BAND_L5 = "L5";

    private static final float SAT_RADIUS = 16.0f;

    static final int COLOR_NOT_VALID_SAT = Color.LTGRAY;

    private static final int SAT_PRN_TEXT_SIZE = 14;

    private static final int GRID_TEXT_SIZE = 16;

    static int SNR_COLORS[] = new int[] {
        Color.argb(0xff, 0, 0xff, 0),
        Color.argb(0xff, 0, 0xaa, 0xff),
        Color.argb(0xff, 0xff, 0, 0xff),
        Color.argb(0xff, 0, 0, 0xff),
        Color.argb(0xff, 0xff, 0, 0),
        Color.argb(0xff, 0xa0, 0xa0, 0xa0)
    };

    private String mBand;

    private final RtkServerObservationStatus mStatus;
    private final gpsplus.rtklib.RtkCommon.Dops mDops;

    private final DecimalFormat mDopsFormatter;

    private final Paint mGridStrokePaint;
    private final Paint mSkyTextPaint, mSkyTextBackgroundPaint;
    private final Paint mLeftInfoTextPaint;
    private final Paint mSatelliteFillPaint, mSatelliteStrokePaint, mSatellitePrnPaint;


    public GpsSkyView(Context context, AttributeSet attrs) {

        super(context, attrs);

        mStatus = new RtkServerObservationStatus();
        mDops = new Dops();
        mBand = BAND_L1;

        mGridStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridStrokePaint.setColor(Color.GRAY);
        mGridStrokePaint.setStyle(Paint.Style.STROKE);

        mSkyTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
        mSkyTextPaint.setColor(Color.GRAY);
        mSkyTextPaint.setTextSize(GRID_TEXT_SIZE);
        mSkyTextPaint.setTextAlign(Paint.Align.CENTER);

        mSkyTextBackgroundPaint = new Paint(mSkyTextPaint);
        mSkyTextBackgroundPaint.setStyle(Paint.Style.STROKE);
        mSkyTextBackgroundPaint.setColor(Color.BLACK);
        mSkyTextBackgroundPaint.setStrokeWidth(4.0f);

        mLeftInfoTextPaint = new Paint(mSkyTextPaint);
        mLeftInfoTextPaint.setTextAlign(Paint.Align.LEFT);

        mSatelliteFillPaint = new Paint();
        mSatelliteFillPaint.setColor(Color.YELLOW);
        mSatelliteFillPaint.setStyle(Paint.Style.FILL);

        mSatelliteStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSatelliteStrokePaint.setColor(Color.BLACK);
        mSatelliteStrokePaint.setStyle(Paint.Style.STROKE);
        mSatelliteStrokePaint.setStrokeWidth(2.0f);

        mSatellitePrnPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
        mSatellitePrnPaint.setColor(Color.WHITE);
        mSatellitePrnPaint.setTextSize(SAT_PRN_TEXT_SIZE);
        mSatellitePrnPaint.setTextAlign(Paint.Align.CENTER);

        mDopsFormatter = new DecimalFormat("0.0");

        setFocusable(true);

    }

    public void setStats(RtkServerObservationStatus status) {
        status.copyTo(mStatus);
        mStatus.getDops(mDops);
        invalidate();
    }

    /**
     *
     * @param band {@link BAND_L1}, {@link BAND_L2}, {@link BAND_L5}
     */
    public void setFreqBand(final String band) {
        if (!TextUtils.equals(band, BAND_L1)
                && !TextUtils.equals(band, BAND_L2)
                && !TextUtils.equals(band, BAND_L5)
                ) throw new IllegalArgumentException();

        mBand = band;
        invalidate();
    }

    public String getBand() {
        return mBand;
    }

    private void drawSky(Canvas c, float s) {
        float radius = s / 2.0f;

        for (double a=0; a<360; a += 45) {
            int x=(int)((radius-SAT_RADIUS)*Math.sin(Math.toRadians(a)));
            int y=(int)((radius-SAT_RADIUS)*Math.cos(Math.toRadians(a)));
            c.drawLine(radius, radius, radius+x, radius+y, mGridStrokePaint);
        }

        c.drawCircle(radius, radius, elevationToRadius(s,  0.0f), mGridStrokePaint);
        c.drawCircle(radius, radius, elevationToRadius(s, 60.0f), mGridStrokePaint);
        c.drawCircle(radius, radius, elevationToRadius(s, 30.0f), mGridStrokePaint);


        final Context ctx = getContext();
        drawTextWithBackground(c, ctx.getString(R.string.sky_plot_north),
                radius,
                SAT_RADIUS + 0.4f * GRID_TEXT_SIZE
                );
        drawTextWithBackground(c, ctx.getString(R.string.sky_plot_east),
                2.0f * radius - SAT_RADIUS,
                radius + 0.4f * GRID_TEXT_SIZE
                );
        drawTextWithBackground(c, ctx.getString(R.string.sky_plot_south),
                radius,
                2.0f * radius - SAT_RADIUS  + 0.4f * GRID_TEXT_SIZE
                );
        drawTextWithBackground(c, ctx.getString(R.string.sky_plot_west),
                SAT_RADIUS,
                (radius + 0.4f*GRID_TEXT_SIZE)
                );
    }

    private void drawTextWithBackground(Canvas c, String str, float x, float y) {
        c.drawText(str, x, y, mSkyTextBackgroundPaint);
        c.drawText(str, x, y, mSkyTextPaint);
    }

    private void drawSatellites(Canvas canvas, float s) {
        final int numSatellites;
        final SatStatus satStatus;

        numSatellites = mStatus.getNumSatellites();
        satStatus = new SatStatus();

        for (int i = 0; i < numSatellites; ++i) {
            int snr;
            double radius, angle;
            float x, y;

            mStatus.getSatStatus(i, satStatus);

            if (satStatus.getElevation() <= 0.0)  continue;
            if (BAND_L1.equals(mBand)) {
                snr = satStatus.getFreq1Snr();
            }else if (BAND_L2.equals(mBand)) {
                snr = satStatus.getFreq2Snr();
            }else {
                snr = satStatus.getFreq2Snr();
            }

            mSatelliteFillPaint.setColor(getSatellitePaintColor(snr,
                    satStatus.isValid()));

            radius = elevationToRadius(s, Math.toDegrees(satStatus.getElevation()));
            angle = satStatus.getAzimuth();

            x = (float)((s / 2) + (radius * Math.sin(angle)));
            y = (float)((s / 2) - (radius * Math.cos(angle)));

            canvas.drawCircle(x, y, SAT_RADIUS, mSatelliteFillPaint);
            canvas.drawCircle(x, y, SAT_RADIUS, mSatelliteStrokePaint);
            canvas.drawText(satStatus.getSatId(),
                    x,
                    (int)(y + 0.4f*SAT_PRN_TEXT_SIZE),
                    mSatellitePrnPaint);
        }
    }

    private void printInfo(Canvas canvas, float s) {
        final String numOfSat = String.format(
                getContext().getString(R.string.sky_plot_num_of_sat)
                , mStatus.getNumSatellites());

        String plotName;
        try {
            plotName = getResources().getStringArray(gpsplus.rtkgps.R.array.rtk_server_receiver)[mStatus.getReceiver()]
                    + " " + mBand;
        }catch (Resources.NotFoundException	e) {
            plotName = "Rover L1";
        }

        final String gdop = "GDOP: " + mDopsFormatter.format(mDops.getGdop());

        canvas.drawText(plotName, 0, 2f * SAT_PRN_TEXT_SIZE, mLeftInfoTextPaint);
        canvas.drawText(numOfSat, 0, s - SAT_PRN_TEXT_SIZE, mLeftInfoTextPaint);
        canvas.drawText(gdop, s - 8*SAT_PRN_TEXT_SIZE, s - SAT_PRN_TEXT_SIZE, mLeftInfoTextPaint);
    }


    static int getSatellitePaintColor(int snr, boolean valid) {
        int j, color;

        if (!valid) {
            color = COLOR_NOT_VALID_SAT;
        }else {
            j = (49 - snr)/(SNR_COLORS.length-1);
            if (j < 0 || j >= (SNR_COLORS.length-1)) {
                j = SNR_COLORS.length-2;
            }
            color = SNR_COLORS[j];
        }
        return color;
    }


    private float elevationToRadius(float s, double elev) {
        return ((s / 2.0f) - SAT_RADIUS) * (90.0f - (float)elev) / 90.0f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Keep the view squared
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        if (w != 0) {
            w = w - getPaddingLeft() - getPaddingRight();
        }
        if (h != 0) {
            h = h - getPaddingTop() - getPaddingBottom();
        }

        int d = w == 0 ? h : h == 0 ? w : w < h ? w : h;
        setMeasuredDimension(d, d);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w, h, s;

        super.onDraw(canvas);

        w = getWidth() - getPaddingLeft() - getPaddingRight();
        h = getHeight() - getPaddingTop() - getPaddingBottom();
        s = (w < h) ? w : h;

        drawSky(canvas, s);
        drawSatellites(canvas, s);
        printInfo(canvas, s);
    }

}
