package ru0xdc.rtkgps.view;

import static junit.framework.Assert.assertTrue;
import ru0xdc.rtkgps.BuildConfig;
import ru0xdc.rtkgps.R;
import ru0xdc.rtklib.RtkServerObservationStatus;
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

public class SnrView extends View {

	@SuppressWarnings("unused")
	private static final boolean DBG = BuildConfig.DEBUG & true;

	static final String TAG = SnrView.class.getSimpleName();

	public static final int BAND_ANY = 0;

	public static final int BAND_L1 = 1;

	public static final int BAND_L2 = 2;

	public static final int BAND_L5 = 5;

	private final static int COLOR_BAR = Color.WHITE;

	private final static int MIN_SNR = 10;

	private final static int MAX_SNR = 60;

	private static final int GRID_TEXT_SIZE = 12;

	private static final int GRID_TEXT_COLOR = Color.GRAY;


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

    	if (mStatus.ns == 0) return;

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

    	final float barBoxWidth = (gridRect.right - gridRect.left) / mStatus.ns;
    	final float dbhzHeight = (gridRect.bottom - gridRect.top) / (MAX_SNR-MIN_SNR);
    	final RectF barBox = new RectF();

    	if (barBoxWidth <= interBarWidth) {
    		interBarWidth = 0.0f;
    	}

    	for (int i=0; i<mStatus.ns; ++i) {
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
    		assertTrue(x2>=x1);

    		// Text
    		canvas.drawText(
    				mStatus.getSatId(i),
    				barBox.left + barBoxWidth/2.0f,
    				gridRect.bottom + mGridTextPaint.getTextSize(),
    				mGridTextPaint
    				);

    		// Fill
    		if (mStatus.vsat[i] != 0) {
    			// L1
    			if ((mBand == BAND_ANY || (mBand == BAND_L1))
    					&& (mStatus.freq1Snr[i] > MIN_SNR)) {
    				snr = Math.min(mStatus.freq1Snr[i], MAX_SNR);
    				mBarFillPaint.setColor(GpsSkyView.getSatellitePaintColor(
    						mStatus.freq1Snr[i], mStatus.vsat[i]));
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
    					&& (mStatus.freq2Snr[i] > MIN_SNR)) {
    				snr = Math.min(mStatus.freq2Snr[i], MAX_SNR);
    				mBarFillPaint.setColor(GpsSkyView.getSatellitePaintColor(
    						mStatus.freq2Snr[i], mStatus.vsat[i]));
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
    					&& (mStatus.freq3Snr[i] > MIN_SNR)
    					) {
    				snr = Math.min(mStatus.freq3Snr[i], MAX_SNR);
    				mBarFillPaint.setColor(GpsSkyView.getSatellitePaintColor(
    						mStatus.freq3Snr[i], mStatus.vsat[i]));
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
    		snr = Math.max(mStatus.freq1Snr[i],
					Math.max(mStatus.freq2Snr[i], mStatus.freq3Snr[i]));
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
