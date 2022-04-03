package gpsplus.rtkgps.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import gpsplus.rtklib.RtkServerStreamStatus;
import gpsplus.rtkgps.BuildConfig;

public class StreamIndicatorsView extends View {

    private final static int DEFAULT_COLOR_STATE_CLOSE = Color.DKGRAY;

    private final static int DEFAULT_COLOR_STATE_WAIT = Color.rgb(255, 127, 0);

    private final static int DEFAULT_COLOR_STATE_CONNECT = Color.rgb(0, 100, 0);

    private final static int DEFAULT_COLOR_STATE_ACTIVE = Color.GREEN;

    private final static int DEFAULT_COLOR_STATE_ERROR = Color.RED;

    private final static int DEFAULT_INDICATOR_WIDTH = 4;

    private final static int DEFAULT_INDICATOR_HEIGHT = 9;

    private final static int DEFAULT_INDICATOR_SPACING = 2;

    private final static String ARROW = "⇝";

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = StreamIndicatorsView.class.getSimpleName();

    private final Paint mStreamIndicatorPaint;

    private final Paint mArrayPaint;

    private final RtkServerStreamStatus mStatus;

    private int mServerStatus;

    private float mIndicatorWidth, mIndicatorHeight, mIndicatorSpacing;


    public StreamIndicatorsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mStatus = new RtkServerStreamStatus();
        mServerStatus = RtkServerStreamStatus.STATE_CLOSE;

        final float density = getResources().getDisplayMetrics().density;

        mIndicatorWidth = DEFAULT_INDICATOR_WIDTH * density;
        mIndicatorHeight = DEFAULT_INDICATOR_HEIGHT * density;
        mIndicatorSpacing = DEFAULT_INDICATOR_SPACING * density;

        mStreamIndicatorPaint = new Paint();
        mStreamIndicatorPaint.setColor(DEFAULT_COLOR_STATE_CLOSE);

        mArrayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArrayPaint.setColor(Color.LTGRAY);
        mArrayPaint.setTextSize(DEFAULT_INDICATOR_HEIGHT*density);
        mArrayPaint.setTextScaleX(1.5f);

    }

    public void setStats(RtkServerStreamStatus status, int serverStatus) {
        status.copyTo(mStatus);
        mServerStatus = serverStatus;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = getMinWidth();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = getMinHeight();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int getMinWidth() {
        float res;

        res = mIndicatorWidth * 9;
        res += mIndicatorSpacing * 7;
        res += 2 * getArrowSize();

        return (int)Math.floor(res);
    }

    private int getMinHeight() {
        return (int)(mIndicatorHeight + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float x, y;
        super.onDraw(canvas);

        if ((canvas.getWidth() < getMinWidth())
                || (canvas.getHeight() < getMinHeight())
                ) {
            Log.d(TAG, "Canvas too small");
            return;
        }

        x = getPaddingLeft();
        y = getPaddingTop();

        // Input stream rover
        drawIndicator(canvas, x, y, mStatus.getInputRoverStatus());

        // Input stream base
        x += mIndicatorWidth + mIndicatorSpacing;
        drawIndicator(canvas, x, y, mStatus.getInputBaseStatus());

        // Input stream correction
        x += mIndicatorWidth + mIndicatorSpacing;
        drawIndicator(canvas, x, y, mStatus.getInputCorrectionStatus());

        x += mIndicatorWidth;
        drawArrow(canvas, x, y);

        // XXX: Status
        x += getArrowSize();
        drawIndicator(canvas, x, y, mServerStatus);

        x += mIndicatorWidth;
        drawArrow(canvas, x, y);

        // Output stream solution 1
        x += getArrowSize();
        drawIndicator(canvas, x, y, mStatus.getOutputSolution1Status());

        // Output stream solution 2
        x += mIndicatorWidth + mIndicatorSpacing;
        drawIndicator(canvas, x, y, mStatus.getOutputSolution2Status());

        x += mIndicatorSpacing;

        // Log stream rover
        x += mIndicatorWidth + mIndicatorSpacing;
        drawIndicator(canvas, x, y, mStatus.getLogRoverStatus());

        // Log stream base
        x += mIndicatorWidth + mIndicatorSpacing;
        drawIndicator(canvas, x, y, mStatus.getLogBaseStatus());

        // Log stream corrections
        x += mIndicatorWidth + mIndicatorSpacing;
        drawIndicator(canvas, x, y, mStatus.getLogCorrectionStatus());
    }

    private void drawArrow(Canvas canvas, float x, float y) {
        canvas.drawText(ARROW, x, y+mIndicatorHeight/1.3f, mArrayPaint);
    }

    private float getArrowSize() {
        return mArrayPaint.measureText(ARROW);
    }

    private void drawIndicator(Canvas canvas, float x, float y, int status) {
        int color;
        switch (status) {
        case RtkServerStreamStatus.STATE_ERROR:
            color = DEFAULT_COLOR_STATE_ERROR;
            break;
        case RtkServerStreamStatus.STATE_CLOSE:
            color = DEFAULT_COLOR_STATE_CLOSE;
            break;
        case RtkServerStreamStatus.STATE_CONNECT:
            color = DEFAULT_COLOR_STATE_CONNECT;
            break;
        case RtkServerStreamStatus.STATE_ACTIVE:
            color = DEFAULT_COLOR_STATE_ACTIVE;
            break;
        case RtkServerStreamStatus.STATE_WAIT:
            color = DEFAULT_COLOR_STATE_WAIT;
            break;
        default:
            color = DEFAULT_COLOR_STATE_ERROR;
            break;
        }

        mStreamIndicatorPaint.setColor(color);
        canvas.drawRect(x, y, x+mIndicatorWidth,
                y+mIndicatorHeight, mStreamIndicatorPaint);
    }


}
