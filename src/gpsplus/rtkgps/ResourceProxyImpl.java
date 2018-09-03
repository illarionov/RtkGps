package gpsplus.rtkgps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import org.osmdroid.DefaultResourceProxyImpl;

public class ResourceProxyImpl extends DefaultResourceProxyImpl {

    private final Resources mResources;

    public ResourceProxyImpl(final Context pContext) {
        super(pContext);
        mResources = pContext.getResources();
    }

    @Override
    public String getString(final string pResId) {
        try {
            final int res = R.string.class.getDeclaredField(pResId.name()).getInt(null);
            return mResources.getString(res);
        } catch (final Exception e) {
            return super.getString(pResId);
        }
    }

    @Override
    public Bitmap getBitmap(final bitmap pResId) {
        try {
            final int res = R.drawable.class.getDeclaredField(pResId.name()).getInt(null);
            return BitmapFactory.decodeResource(mResources, res);
        } catch (final Exception e) {
            return super.getBitmap(pResId);
        }
    }

    @Override
    public Drawable getDrawable(final bitmap pResId) {
        try {
            final int res = R.drawable.class.getDeclaredField(pResId.name()).getInt(null);
            return mResources.getDrawable(res);
        } catch (final Exception e) {
            return super.getDrawable(pResId);
        }
    }
}
