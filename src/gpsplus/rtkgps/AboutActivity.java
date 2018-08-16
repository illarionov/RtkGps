package gpsplus.rtkgps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import gpsplus.rtkgps.utils.ChangeLog;
import gpsplus.rtkgps.utils.Translated;

import java.util.Locale;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version)).setText(pi.versionName);
        } catch (NameNotFoundException nnfe) {
            nnfe.printStackTrace();
        }

        TextView translationTextView = (TextView)findViewById(R.id.translation_label);
        TextView translationLink = (TextView)findViewById(R.id.translation_link);
        if (!Translated.contains(Locale.getDefault().getISO3Language()))
        {
            translationTextView.setText(getResources().getString(R.string.about_translation_title,Locale.getDefault().getDisplayLanguage(Locale.ENGLISH))+"\n"+
                    getResources().getString(R.string.about_translation_subtitle)+"\n"+
                    getResources().getString(R.string.about_translation_message) );
            translationLink.setText( Html.fromHtml(getResources().getString(R.string.about_translation_link) ));
            translationLink.setMovementMethod(LinkMovementMethod.getInstance());
        }

    }

    public void onLegacyInfoButtonClicked(View v) {
        final DialogFragment dialog;
        dialog = new OpenSourceLicensesDialog();
        dialog.show(getFragmentManager(), null);
    }

    public void onChangelogButtonClicked(View v) {
        ChangeLog cl = new ChangeLog(this);
        cl.getFullLogDialog().show();
    }

    public static class OpenSourceLicensesDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");

            return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.about_licenses)
                .setView(webView)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    }
            ).create();
        }

    }
}
