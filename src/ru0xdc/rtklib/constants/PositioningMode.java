package ru0xdc.rtklib.constants;

import ru0xdc.rtkgps.R;
import android.content.res.Resources;

/**
 * Positioning mode PMODE_XXX
 *
 */
public enum PositioningMode implements IHasRtklibId {

	/** positioning mode: single */
	SINGLE(0, R.string.pmode_single),
	/** positioning mode: DGPS/DGNSS */
	DGPS(1, R.string.pmode_dgps),
	/** positioning mode: kinematic */
	KINEMA(2, R.string.pmode_kinema),
	/** positioning mode: static */
	STATIC(3, R.string.pmode_static),
	/** positioning mode: moving-base */
	MOVEB(4, R.string.pmode_moveb),
	/** positioning mode: fixed */
	FIXED(5, R.string.pmode_fixed),
	/** positioning mode: PPP-kinemaric */
	PPP_KINEMA(6, R.string.pmode_ppp_kinema),
	/** positioning mode: PPP-static */
	PPP_STATIC(7, R.string.pmode_ppp_static),
	/** positioning mode: PPP-fixed */
	PPP_FIXED(8, R.string.pmode_ppp_fixed)
	;

	private final int mRtklibId;
	private final int mNameResId;

	private PositioningMode(int rtklibId, int nameResId) {
		mRtklibId = rtklibId;
		mNameResId = nameResId;
	}

	@Override
	public int getRtklibId() {
		return mRtklibId;
	}

	@Override
	public int getNameResId() {
		return mNameResId;
	}

	public static PositioningMode valueOf(int rtklibId) {
		for (PositioningMode v: values()) {
			if (v.mRtklibId == rtklibId) return v;
		}
		throw new IllegalArgumentException();
	}

	public static CharSequence[] getEntries(Resources r) {
		final PositioningMode values[] = values();
		final CharSequence res[] = new CharSequence[values.length];
		for (int i=0; i<values.length; ++i) res[i] = r.getString(values[i].mNameResId);
		return res;
	}

	public static CharSequence[] getEntryValues() {
		final PositioningMode values[] = values();
		final CharSequence res[] = new CharSequence[values.length];
		for (int i=0; i<values.length; ++i) res[i] = values[i].name();
		return res;
	}
}
