package gpsplus.rtkgps.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerObservationStatus;
import gpsplus.rtklib.RtkServerObservationStatus.SatStatus;

public class SnrView extends View {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;

    static final String TAG = SnrView.class.getSimpleName();

    public static final int BAND_ANY = 0;

    public static final int BAND_L1 = 1;

    public static final int BAND_L2 = 2;

    public static final int BAND_L5 = 5;

    private final static int COLOR_BAR = Color.argb(0x80, 0xff, 0xff, 0xff);

    private final static int MIN_SNR = 10;

    private final static int MAX_SNR = 60;

    private static final int GRID_TEXT_SIZE = 12;

    private static final int GRID_TEXT_COLOR = Color.GRAY;

    private static final SatStatus TEST_SAT_STATUS[] = new SatStatus[] {
        new SatStatus(10, 0, 0, 10, 10, 10, false),
        new SatStatus(15, 0, 0, 15, 15, 15, true),
        new SatStatus(20, 0, 0, 20, 20, 20, true),
        new SatStatus(25, 0, 0, 25, 25, 25, true),
        new SatStatus(30, 0, 0, 30, 30, 30, true),
        new SatStatus(35, 0, 0, 35, 35, 35, true),
        new SatStatus(40, 0, 0, 40, 40, 40, true),
        new SatStatus(45, 0, 0, 45, 45, 45, true),


    };

    private final RtkServerObservationStatus mStatus;

    private int mBand;

    private final Paint mGridPaint, mGridTextPaint, mGridTextBackground;
    private final Paint mBarPaint, mBarFillPaint;

    public SnrView(Context context) {
        this(context, null);
    }

    public SnrView(Context context, AttributeSet attrs) {
        super(context, attrs);

        int gridTextSize;
        int gridTextColor;

        mStatus = new RtkServerObservationStatus();
        mBand = BAND_ANY;

        // process style attributes
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.GTimeView, 0, 0);
        try {
            gridTextSize = a.getInt(R.styleable.SnrView_grid_text_size, GRID_TEXT_SIZE);
            gridTextColor = a.getColor(R.styleable.SnrView_grid_text_color, GRID_TEXT_COLOR);
        }finally {
            a.recycle();
        }

        final float density = getResources().getDisplayMetrics().density;

        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPaint.setColor(gridTextColor);
        mGridPaint.setStyle(Style.STROKE);

        mGridTextPaint = new Paint(mGridPaint);
        mGridTextPaint.setTextSize(gridTextSize * density);
        mGridTextPaint.setTextAlign(Paint.Align.CENTER);
        mGridTextPaint.setStyle(Style.FILL);

        mGridTextBackground = new Paint();
        mGridTextBackground.setColor(Color.TRANSPARENT);
        mGridTextBackground.setStyle(Style.FILL);
        mGridTextBackground.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

        mBarPaint = new Paint();
        mBarPaint.setColor(COLOR_BAR);
        mBarPaint.setStyle(Style.STROKE);
        mBarPaint.setStrokeWidth(1.0f);

        mBarFillPaint = new Paint();
        mBarFillPaint.setColor(Color.TRANSPARENT);
        mBarFillPaint.setStyle(Style.FILL);

    }

    public void setStats(RtkServerObservationStatus status) {
        status.copyTo(mStatus);
        invalidate();
    }

    /**
     *
     * @param band {@link BAND_ANY}, {@link BAND_L1}, {@link BAND_L2}, {@link BAND_L5}
     */
    public void setFreqBand(int band) {
        switch (band) {
        case BAND_ANY:
        case BAND_L1:
        case BAND_L2:
        case BAND_L5:
            mBand = band;
            break;
        default:
            throw new IllegalArgumentException();
        }
        invalidate();
    }

    private RectF getGridRect() {
        float left, top, right, bottom;

        left = getPaddingLeft();
        top = getPaddingTop();
        right = getWidth() - getPaddingRight() - getPaddingLeft() - 2*mGridPaint.getStrokeWidth();
        bottom = getHeight() - getPaddingBottom() - getPaddingTop() - 2*mGridPaint.getStrokeWidth();

        float headerSize = mGridTextPaint.getTextSize() + 5;
        float footerSize = headerSize;

        return new RectF(
                left,
                top+headerSize,
                right,
                bottom-footerSize
                );
    }

    private float getSnrLabelLength(){
        return mGridTextPaint.measureText("60") + 5;
    }

    private void drawGrid(Canvas canvas) {
        final RectF gridRect = getGridRect();
        canvas.drawRect(gridRect, mGridPaint);

        final float rectWidth = gridRect.bottom - gridRect.top;
        final float snrLabelLength = getSnrLabelLength();
        for (int snr=MIN_SNR+10; snr < MAX_SNR; snr += 10) {
            float y = gridRect.bottom - rectWidth * (snr-MIN_SNR) / (MAX_SNR-MIN_SNR);

            canvas.drawLine(gridRect.left,
                    y,
                    gridRect.right - snrLabelLength,
                    y, mGridPaint);

            canvas.drawText(String.valueOf(snr),
                    gridRect.right - snrLabelLength/2,
                    y+mGridTextPaint.getTextSize()*0.4f,
                    mGridTextPaint);
        }

    }

    private void drawSnr(Canvas canvas) {
        final RectF gridRect;
        final float density;
        final float paddingRight;
        final float paddingLeft;
        float interBarWidth;
        final int numSatellites;
        SatStatus satStatus;

        if (isInEditMode()) {
            numSatellites = TEST_SAT_STATUS.length;
        }else {
            numSatellites = mStatus.getNumSatellites();
        }

        if (numSatellites == 0) return;

        gridRect = getGridRect();
        density = getResources().getDisplayMetrics().density;
        paddingRight = getSnrLabelLength() + 4*density;
        paddingLeft = 2*density;
        interBarWidth = 4*density;

        gridRect.set(
                gridRect.left + paddingLeft,
                gridRect.top,
                gridRect.right - paddingRight,
                gridRect.bottom
                );
        if (gridRect.left < 0
                || gridRect.right < 0
                || gridRect.right <= gridRect.left
                || ((gridRect.right - gridRect.left) < 2*interBarWidth)
                ) {
            Log.v(TAG, "Canvas too small");
            return;
        }

        final float barBoxWidth = (gridRect.right - gridRect.left) / numSatellites;
        final float dbhzHeight = (gridRect.bottom - gridRect.top) / (MAX_SNR-MIN_SNR);
        final RectF barBox = new RectF();

        if (barBoxWidth <= interBarWidth) {
            interBarWidth = 0.0f;
        }

        satStatus = new SatStatus();

        for (int i=0; i<numSatellites; ++i) {
            float snr;
            final float x1, x2;

            barBox.set(
                    gridRect.left + i*barBoxWidth,
                    gridRect.top,
                    gridRect.left + i*barBoxWidth + barBoxWidth,
                    gridRect.bottom + mGridPaint.getStrokeWidth()*density
                    );

            x1 = barBox.left + interBarWidth/2.0f;
            x2 = barBox.right - interBarWidth/2.0f;
            // assertTrue(x2>=x1);

            if (isInEditMode()) {
                satStatus = TEST_SAT_STATUS[i];
            }else {
                mStatus.getSatStatus(i, satStatus);
            }

            // Text
            canvas.drawText(
                    satStatus.getSatId(),
                    barBox.left + barBoxWidth/2.0f,
                    gridRect.bottom + mGridTextPaint.getTextSize(),
                    mGridTextPaint
                    );

            // Fill
            if (satStatus.isValid()) {
                // L1
                if ((mBand == BAND_ANY || (mBand == BAND_L1))
                        && (satStatus.getFreq1Snr() > MIN_SNR)) {
                    snr = Math.min(satStatus.getFreq1Snr(), MAX_SNR);
                    mBarFillPaint.setColor(GpsSkyView.getSatellitePaintColor(
                            satStatus.getFreq1Snr(), true));
                    canvas.drawRect(
                            x1,
                            barBox.bottom - (snr-MIN_SNR) * dbhzHeight,
                            x2,
                            barBox.bottom,
                            mBarFillPaint
                            );
                }

                // L2
                if ((mBand == BAND_ANY || (mBand == BAND_L2))
                        && (satStatus.getFreq2Snr() > MIN_SNR)) {
                    snr = Math.min(satStatus.getFreq2Snr(), MAX_SNR);
                    mBarFillPaint.setColor(GpsSkyView.getSatellitePaintColor(
                            satStatus.getFreq2Snr(), true));
                    canvas.drawRect(
                            x1,
                            barBox.bottom - (snr-MIN_SNR) * dbhzHeight,
                            x2,
                            barBox.bottom,
                            mBarFillPaint
                            );
                }

                // L5
                if ((mBand == BAND_ANY || (mBand == BAND_L5))
                        && (satStatus.getFreq3Snr() > MIN_SNR)
                        ) {
                    snr = Math.min(satStatus.getFreq3Snr(), MAX_SNR);
                    mBarFillPaint.setColor(GpsSkyView.getSatellitePaintColor(
                            satStatus.getFreq3Snr(), true));
                    canvas.drawRect(
                            x1,
                            barBox.bottom - (snr-MIN_SNR) * dbhzHeight,
                            x2,
                            barBox.bottom,
                            mBarFillPaint
                            );
                }
            }

            // Stroke
            snr = Math.max(satStatus.getFreq1Snr(),
                    Math.max(satStatus.getFreq2Snr(), satStatus.getFreq3Snr()));
            if (snr > MAX_SNR) snr = MAX_SNR;
            if (snr > MIN_SNR) {
                float yTop = barBox.bottom - (snr - MIN_SNR) * dbhzHeight;
                canvas.drawLine(x1, yTop, x1, barBox.bottom, mBarPaint);
                canvas.drawLine(x1, yTop, x2, yTop, mBarPaint);
                canvas.drawLine(x2, yTop, x2, barBox.bottom, mBarPaint);
            }


        }



    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawGrid(canvas);
        drawSnr(canvas);
    }



}
