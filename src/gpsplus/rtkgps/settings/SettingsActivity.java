package gpsplus.rtkgps.settings;

import java.util.List;

import gpsplus.rtkgps.R;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    protected boolean isValidFragmentA(String fragmentName) {
        return ProcessingOptions1Fragment.class.getName().equals(fragmentName);
    }
    protected boolean isValidFragment(String fragmentName) {
        // Three setting panels can hit this:
        String processingPanel = ProcessingOptions1Fragment.class.getName();
        String solutionPanel = SolutionOutputSettingsFragment.class.getName();
        String ntripcasterPanel = NTRIPCasterSettingsFragment.class.getName();
        return (processingPanel.equals(fragmentName) || solutionPanel.equals(fragmentName) || ntripcasterPanel.equals(fragmentName));
    }

}
