package ru0xdc.rtkgps;

import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.SafeDrawOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafePaint;
import org.osmdroid.views.safecanvas.SafeTranslatedPath;

import ru0xdc.rtkgps.view.SolutionView;
import ru0xdc.rtklib.RtkCommon;
import ru0xdc.rtklib.RtkCommon.Position3d;
import ru0xdc.rtklib.Solution;
import ru0xdc.rtklib.constants.SolutionStatus;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

/**
 *
 * @author Viesturs Zarins
 * @author Martin Pearman
 *
 *         This class draws a path line in given color.
 */
public class SolutionPathOverlay extends SafeDrawOverlay {

    private static final int DEFAULT_SIZE = 1000;


    private static final int PATH_COLOR = Color.GRAY;


	private final int[] mLat;
	private final int[] mLon;
	private final SolutionStatus[] mPointSolutionStatus;

	private final int[] mProjectedX;
	private final int[] mProjectedY;

	private int mBufHead;
	private int mBufSize;

	private final double mPointsCache[][];
	private final int mPointsCacheSize[];

	private final SafePaint mPaint;
	private final SafePaint mPointPaint;

	private final SafeTranslatedPath mPath;


	public SolutionPathOverlay(final ResourceProxy pResourceProxy) {
	    this(DEFAULT_SIZE, pResourceProxy);
	}

	public SolutionPathOverlay(final int size, final ResourceProxy pResourceProxy) {
		super(pResourceProxy);

	    this.mPaint = new SafePaint();
	    this.mPointPaint = new SafePaint();
	    this.mPath = new SafeTranslatedPath();
		this.mLat = new int[size];
		this.mLon = new int[size];
		this.mProjectedX = new int[size];
		this.mProjectedY = new int[size];
		this.mPointSolutionStatus = new SolutionStatus[size];
		this.mBufHead = 0;
		this.mBufSize = 0;

		this.mPointsCache = new double[SolutionStatus.values().length][];
		this.mPointsCacheSize = new int[this.mPointsCache.length];

		this.mPaint.setColor(PATH_COLOR);
		this.mPaint.setStrokeWidth(1.0f);
		this.mPaint.setStyle(Paint.Style.STROKE);

		this.mPointPaint.setStrokeWidth(5.0f);

		this.clear();
	}

	public void clear() {
		this.mBufHead = 0;
		this.mBufSize = 0;
	}

	private int getSize() {
	    return mLat.length;
	}

	@SuppressWarnings("unused")
    private boolean isBufEmpty() {
	    return this.mBufSize == 0;
	}

	private boolean isBufFull() {
	    return this.mBufSize == getSize();
	}

	public boolean addSolution(final Solution solution) {
	    if (solution.getSolutionStatus() == SolutionStatus.NONE) {
	        return false;
	    }

	    final Position3d pos = RtkCommon.ecef2pos(solution.getPosition());
	    final int tail = (this.mBufHead + this.mBufSize) % getSize();
	    mLat[tail] = (int)Math.round(1.0e6 * Math.toDegrees(pos.getLat()));
	    mLon[tail] = (int)Math.round(1.0e6 * Math.toDegrees(pos.getLon()));
	    mPointSolutionStatus[tail] = solution.getSolutionStatus();
	    if (isBufFull()) {
	        mBufHead = (mBufHead + 1) % getSize();
	    }else {
	        mBufSize += 1;
	    }

	    return true;
	}

	public void addSolutions(final Solution[] solutions) {
	    for (Solution s: solutions) {
	        addSolution(s);
	    }
	}

    /**
     *  precompute new points to the intermediate projection.
     * @param prj
     */
	private void precomputePoints(final Projection prj) {
        final Point dst = new Point();
        for (int i = 0; i < mBufSize; ++i) {
            final int bufIdx =(mBufHead + i) % getSize();
            prj.toMapPixelsProjected(mLat[bufIdx], mLon[bufIdx], dst);
            mProjectedX[bufIdx] = dst.x;
            mProjectedY[bufIdx] = dst.y;
        }
	}

	private void rewindPointsCache() {
	    for (int i=0; i<mPointsCacheSize.length; ++i) mPointsCacheSize[i]=0;
	}

	private void appendPoint(double x, double y, SolutionStatus status) {
	    final int ord = status.ordinal();
	    if (mPointsCache[ord] == null) {
	        mPointsCache[ord] = new double[getSize() * 2];
	    }

	    final double dst[] = mPointsCache[ord];
	    dst[mPointsCacheSize[ord]] = x;
	    dst[mPointsCacheSize[ord]+1] = y;
	    mPointsCacheSize[ord] += 2;
	}

	private void drawPoints(ISafeCanvas canvas) {
	    for (int i = mPointsCache.length-1; i >= 0; i--) {
	        final int count = mPointsCacheSize[i];
	        if (count == 0) continue;
	        final SolutionStatus status = SolutionStatus.values()[i];
	        mPointPaint.setColor(SolutionView.SolutionIndicatorView.getIndicatorColor(status));
	        canvas.drawPoints(mPointsCache[i], 0, count, mPointPaint);
	    }
	}

	/**
	 * This method draws the line. Note - highly optimized to handle long paths, proceed with care.
	 * Should be fine up to 10K points.
	 */
    @Override
    protected void drawSafe(ISafeCanvas canvas, MapView osmv, boolean shadow) {
        final Projection pj;
        final Rect clipBounds, lineBounds;
        Point screenPoint0, screenPoint1;
        Point tempPoint0, tempPoint1;
        final Point projectedPoint0, projectedPoint1;
        int bufIdx;

        if (shadow) {
            return;
        }

        if (this.mBufSize < 2) {
            // nothing to paint
            return;
        }

        pj = osmv.getProjection();

        precomputePoints(pj);

        // clipping rectangle in the intermediate projection, to avoid performing projection.
        clipBounds = pj.fromPixelsToProjected(pj.getScreenRect());

        screenPoint0 = null;
        screenPoint1 = null;
        projectedPoint0 = new Point();
        projectedPoint1 = new Point();
        tempPoint0 = new Point();
        tempPoint1 = new Point();

        mPath.onDrawCycleStart(canvas);

        bufIdx = (mBufHead + mBufSize - 1) % getSize();
        projectedPoint0.set(mProjectedX[bufIdx], mProjectedY[bufIdx]);
        lineBounds = new Rect(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);

        for (int i = mBufSize - 2; i >= 0; i--) {
            bufIdx = (mBufHead + i) % getSize();

            // compute next points
            lineBounds.union(mProjectedX[bufIdx], mProjectedY[bufIdx]);
            if (!Rect.intersects(clipBounds, lineBounds)) {
                // skip this line, move to next point
                projectedPoint0.set(mProjectedX[bufIdx], mProjectedY[bufIdx]);
                screenPoint0 = null;
                continue;
            }else {
                projectedPoint1.set(mProjectedX[bufIdx], mProjectedY[bufIdx]);
            }

            // the starting point may be not calculated, because previous segment was out of clip
            // bounds
            if (screenPoint0 == null) {
                screenPoint0 = pj.toMapPixelsTranslated(projectedPoint0, tempPoint0);
                mPath.moveTo((double)screenPoint0.x, (double)screenPoint0.y);
                appendPoint(screenPoint0.x, screenPoint0.y, mPointSolutionStatus[bufIdx]);
            }

            screenPoint1 = pj.toMapPixelsTranslated(projectedPoint1, tempPoint1);

            // skip this point, too close to previous point
            if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
                continue;
            }

            canvas.drawLine(screenPoint0.x, screenPoint0.y, screenPoint1.x, screenPoint1.y, mPaint);
            mPath.lineTo((double)screenPoint1.x, (double)screenPoint1.y);
            appendPoint(screenPoint1.x, screenPoint1.y, mPointSolutionStatus[bufIdx]);

            // update starting point to next position
            projectedPoint0.set(projectedPoint1.x, projectedPoint1.y);
            screenPoint0.set(screenPoint1.x, screenPoint1.y);
            lineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);
        }

        canvas.drawPath(mPath, this.mPaint);
        drawPoints(canvas);

        rewindPointsCache();
        mPath.rewind();

    }
}
