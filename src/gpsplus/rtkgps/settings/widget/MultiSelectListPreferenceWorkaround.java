package gpsplus.rtkgps.settings.widget;

import java.util.Set;

import android.content.Context;
import android.os.Build;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

public class MultiSelectListPreferenceWorkaround extends MultiSelectListPreference {

    public MultiSelectListPreferenceWorkaround(Context context,
            AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiSelectListPreferenceWorkaround(Context context) {
        super(context);
    }

    @Override
    public void setValues(Set<String> values) {

        /**
         * Workaround for amnesia problem in MultiSelectListPreference
         * https://android.googlesource.com/platform/frameworks/base/+/cd9ea08d9cb68004b2d5f69302cddf53dc034e7b%5E!/
         *
         */
        if (Build.VERSION.SDK_INT >= 16) {
            super.setValues(values);
        }else {
            final Set<String> mValuesOrig;

            mValuesOrig = getValues();
            super.setValues(values);
            if (getValues() == values) {
                mValuesOrig.clear();
                mValuesOrig.addAll(values);
                super.setValues(mValuesOrig);
            }
        }
    }
}
