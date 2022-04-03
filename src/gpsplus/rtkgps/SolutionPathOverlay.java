package gpsplus.rtkgps;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import gpsplus.rtkgps.view.SolutionView;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.Solution;
import gpsplus.rtklib.constants.SolutionStatus;
import microsoft.mappoint.TileSystem;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.drawing.OsmPath;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.util.constants.MapViewConstants;

/**
 *
 * @author Viesturs Zarins
 * @author Martin Pearman
 *
 *         This class draws a path line in given color.
 */
public class SolutionPathOverlay extends PathOverlay {

    private static final int DEFAULT_SIZE = 1000;
    private static final int PATH_COLOR = Color.GRAY;


    private final int[] mProjectedX;
    private final int[] mProjectedY;
	private final SolutionStatus[] mPointSolutionStatus;

	private int mBufHead;
	private int mBufSize;

	private final float mPointsCache[][];
	private final int mPointsCacheSize[];

	private final Paint mPaint;
	private final Paint mPointPaint;

	private final OsmPath mPath;


	public SolutionPathOverlay(final ResourceProxy pResourceProxy) {
	    this(DEFAULT_SIZE, pResourceProxy);
	}

	public SolutionPathOverlay(final int size, final ResourceProxy pResourceProxy) {
		super(PATH_COLOR,pResourceProxy);

	    this.mPaint = new Paint();
	    this.mPointPaint = new Paint();
	    this.mPath = new OsmPath();
		this.mProjectedX = new int[size];
		this.mProjectedY = new int[size];
		this.mPointSolutionStatus = new SolutionStatus[size];
		this.mBufHead = 0;
		this.mBufSize = 0;

		this.mPointsCache = new float[SolutionStatus.values().length][];
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
	    return mProjectedX.length;
	}

	@SuppressWarnings("unused")
    private boolean isBufEmpty() {
	    return this.mBufSize == 0;
	}

	private boolean isBufFull() {
	    return this.mBufSize == getSize();
	}

	public boolean addSolution(final Solution solution) {
	    final Position3d pos;
	    final double lat, lon;
	    final int tail;

	    if (solution.getSolutionStatus() == SolutionStatus.NONE) {
	        return false;
	    }

	    pos = RtkCommon.ecef2pos(solution.getPosition());
	    lat = Math.toDegrees(pos.getLat());
	    lon = Math.toDegrees(pos.getLon());
	    tail = (this.mBufHead + this.mBufSize) % getSize();
	    mPointSolutionStatus[tail] = solution.getSolutionStatus();

	    // Performs the first computationally heavy part of the projection.
	    final Point projected = TileSystem.LatLongToPixelXY(lat,
	            lon, MapViewConstants.MAXIMUM_ZOOMLEVEL, null);
        mProjectedX[tail] = projected.x;
        mProjectedY[tail] = projected.y;

	    if (isBufFull()) {
	        mBufHead = (mBufHead + 1) % getSize();
	    }else {
	        mBufSize += 1;
	    }

	    return true;
	}

	public void addSolutions(final Solution[] solutions) {
	    for (Solution s: solutions) addSolution(s);
	}

	private void rewindPointsCache() {
	    for (int i=0; i<mPointsCacheSize.length; ++i) mPointsCacheSize[i]=0;
	}

	private void appendPoint(double x, double y, SolutionStatus status) {
	    final int ord = status.ordinal();
	    if (mPointsCache[ord] == null) {
	        mPointsCache[ord] = new float[getSize() * 2];
	    }

	    final float dst[] = mPointsCache[ord];
	    dst[mPointsCacheSize[ord]] = (float)x;
	    dst[mPointsCacheSize[ord]+1] = (float)y;
	    mPointsCacheSize[ord] += 2;
	}

	private void drawPoints(Canvas canvas) {
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
    protected void draw(Canvas canvas, MapView osmv, boolean shadow) {
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

        // clipping rectangle in the intermediate projection, to avoid performing projection.
        BoundingBoxE6 boundingBox = pj.getBoundingBox();
        Point topLeft = pj.toProjectedPixels(boundingBox.getLatNorthE6(),
                boundingBox.getLonWestE6(), null);
        Point bottomRight = pj.toProjectedPixels(boundingBox.getLatSouthE6(),
                boundingBox.getLonEastE6(), null);
        clipBounds = new Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);

        screenPoint0 = null;
        screenPoint1 = null;
        projectedPoint0 = new Point();
        projectedPoint1 = new Point();
        tempPoint0 = new Point();
        tempPoint1 = new Point();

        mPath.rewind();

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
                screenPoint0 = pj.toPixelsFromProjected(projectedPoint0, tempPoint0);
                mPath.moveTo(screenPoint0.x, screenPoint0.y);
                appendPoint(screenPoint0.x, screenPoint0.y, mPointSolutionStatus[bufIdx]);
            }

            screenPoint1 = pj.toPixelsFromProjected(projectedPoint1, tempPoint1);

            // skip this point, too close to previous point
            if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
                continue;
            }

            canvas.drawLine(screenPoint0.x, screenPoint0.y, screenPoint1.x, screenPoint1.y, mPaint);
            mPath.lineTo(screenPoint1.x, screenPoint1.y);
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
