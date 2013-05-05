package ru0xdc.rtklib.constants;

import ru0xdc.rtkgps.R;
import android.content.res.Resources;

/** Geoid model GEOID_XXX */
public enum GeoidModel implements IHasRtklibId {

	/** geoid model: embedded geoid */
	EMBEDDED(0, R.string.geoid_embedded),

	/** geoid model: EGM96 15x15" */
	EGM96_M150(1, R.string.geoid_egm96_m150),

	/** geoid model: EGM2008 2.5x2.5" */
	EGM2008_M25(2, R.string.geoid_egm2008_m25),

	/** geoid model: EGM2008 1.0x1.0" */
	EGM2008_M10(3, R.string.geoid_egm2008_m10),

	/**  geoid model: GSI geoid 2000 1.0x1.5" */
	GSI2000_M15(4, R.string.geoid_gsi2000_m15)

	;

	private final int mRtklibId;
	private final int mNameResId;

	private GeoidModel(int rtklibId, int nameResId) {
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

	public static GeoidModel valueOf(int rtklibId) {
		for (GeoidModel v: values()) {
			if (v.mRtklibId == rtklibId) return v;
		}
		throw new IllegalArgumentException();
	}

	public static CharSequence[] getEntries(Resources r) {
		final GeoidModel values[] = values();
		final CharSequence res[] = new CharSequence[values.length];
		for (int i=0; i<values.length; ++i) res[i] = r.getString(values[i].mNameResId);
		return res;
	}

	public static CharSequence[] getEntryValues() {
		final GeoidModel values[] = values();
		final CharSequence res[] = new CharSequence[values.length];
		for (int i=0; i<values.length; ++i) res[i] = values[i].name();
		return res;
	}
}
