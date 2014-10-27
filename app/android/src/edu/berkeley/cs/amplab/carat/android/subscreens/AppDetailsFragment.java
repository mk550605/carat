package edu.berkeley.cs.amplab.carat.android.subscreens;

import java.util.HashMap;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.CaratApplication.Type;
import edu.berkeley.cs.amplab.carat.android.protocol.ClickTracking;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.storage.SimpleHogBug;
import edu.berkeley.cs.amplab.carat.android.ui.DrawView;
import edu.berkeley.cs.amplab.carat.thrift.DetailScreenReport;
import edu.berkeley.cs.amplab.carat.thrift.Reports;

public class AppDetailsFragment extends Fragment {

	private SimpleHogBug fullObject;
	private boolean isBugs = false;
	private boolean isApp = false;
	private boolean isOs = false;
	private boolean isModel = false;
	private double ev, error, evWithout, errorWo;
	private int samplesCount, samplesCountWithout;

	/*
	 * @Param type the type of the details we are going to display. Supported
	 * values are: TYPE.OS, TYPE.MODEL, TYPE.OTHER. 
	 * To specify that we are going to show the details of regular app,
	 * pass the TYPE "other" as the first argument. Use TYPE.OS for OS details
	 * and TYPE.MODEL for device details
	 */
	public static AppDetailsFragment newInstance(Type type, SimpleHogBug fullObject, boolean isBugs) {
		AppDetailsFragment fragment = new AppDetailsFragment();
		switch (type) {
		case OTHER:
			fragment.setFullObject(fullObject);
			fragment.isBugs = isBugs;
			fragment.isApp = true;
			break;
		case OS:
			fragment.isOs = true;
			Log.d("AppDetailsFragment", "type = OS");
			break;
		case MODEL:
			fragment.isModel = true;
			Log.d("AppDetailsFragment", "type = MODEL");
			break;
		default:
			throw new IllegalArgumentException(
					"only the types OS, MODEL, and OTHER should be passed to AppDetailsFragment.newInstance()");
		}

		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View detailsPage = inflater.inflate(R.layout.graph, container, false);

		if (isApp) {
			String label = CaratApplication.labelForApp(getActivity(), fullObject.getAppName());
			Drawable icon = CaratApplication.iconForApp(getActivity(), fullObject.getAppName());
			PackageInfo pak = SamplingLibrary.getPackageInfo(getActivity(), fullObject.getAppName());

			String ver = "";
			if (pak != null) {
				ver = pak.versionName;
				if (ver == null)
					ver = pak.versionCode + "";
			}
			final String s = label + " " + ver;

			((TextView) detailsPage.findViewById(R.id.name)).setText(s);
			((ImageView) detailsPage.findViewById(R.id.appIcon)).setImageDrawable(icon);

			// setDetails(fullObject.getExpectedValue(), fullObject.getError(),
			// osWithout.getExpectedValue(), osWithout.getError(), (int)
			// fullObject.getSamples(), (int) fullObject.getSamplesWithout());

			((TextView) detailsPage.findViewById(R.id.benefit)).setText(fullObject.textBenefit());
			((TextView) detailsPage.findViewById(R.id.killBenefit)).setText(fullObject.textBenefit());
			((TextView) detailsPage.findViewById(R.id.samples)).setText(String.valueOf(fullObject.getSamples()));
			((TextView) detailsPage.findViewById(R.id.error)).setText(String.valueOf(fullObject.getError()));
			((TextView) detailsPage.findViewById(R.id.samplesWo))
					.setText(String.valueOf(fullObject.getSamplesWithout()));

			View moreinfo = detailsPage.findViewById(R.id.moreinfo);
			moreinfo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					CaratApplication.showHTMLFile("detailinfo");
				}
			});
			trackUser(label, pak);
		} else {
			DrawView drawView = new DrawView(getActivity());
			Reports r = CaratApplication.s.getReports();
			String label;

			if (r != null) {
				Log.d("ReadReports", "Read reports. Reports are not null.");
				if (isOs) {
					// Log.d("isOS", "isOS");
					DetailScreenReport os = r.getOs();
					DetailScreenReport osWithout = r.getOsWithout();
					label = getString(R.string.os) + ": " + SamplingLibrary.getOsVersion();
					setDetails(os, osWithout);
					drawView.setParams(Type.OS, SamplingLibrary.getOsVersion(), ev, evWithout, samplesCount,
							samplesCountWithout, error, errorWo, detailsPage);

					Log.v("OsInfo", "Os score: " + os.getScore());
					CaratApplication.trackUser("osInfo");
				} else {
					// Log.d("isModel", "isModel");
					DetailScreenReport model = r.getModel();
					DetailScreenReport modelWithout = r.getModelWithout();
					label = getString(R.string.model) + ": " + SamplingLibrary.getModel();
					setDetails(model, modelWithout);
					drawView.setParams(Type.MODEL, SamplingLibrary.getModel(), ev, evWithout, samplesCount,
							samplesCountWithout, error, errorWo, detailsPage);

					Log.v("ModelInfo", "Model score: " + model.getScore());
					CaratApplication.trackUser("deviceInfo");
				}

				Drawable icon = CaratApplication.iconForApp(getActivity(), "Carat");
				((TextView) detailsPage.findViewById(R.id.name)).setText(label);
				((ImageView) detailsPage.findViewById(R.id.appIcon)).setImageDrawable(icon);

				String benefitText = SimpleHogBug.textBenefit(ev, error, evWithout, errorWo);
				if (benefitText == null)
					benefitText = getString(R.string.jsna);

				((TextView) detailsPage.findViewById(R.id.benefit)).setText(benefitText);
			}

			Log.d("NullReports", "Reports are null!!!");
		}

		// onCreateView() should return the view resulting from inflating the
		// layout file
		return detailsPage;
	}

	private void setDetails(DetailScreenReport obj, DetailScreenReport objWithout) {
		this.ev = obj.getExpectedValue();
		this.error = obj.getError();
		this.evWithout = objWithout.getExpectedValue();
		this.errorWo = objWithout.getError();
		this.samplesCount = (int) obj.getSamples();
		this.samplesCountWithout = (int) objWithout.getSamples();
	}

	private void trackUser(String label, PackageInfo pak) {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (p != null) {
			String uuId = p.getString(CaratApplication.REGISTERED_UUID, "UNKNOWN");
			HashMap<String, String> options = new HashMap<String, String>();
			options.put("status", getActivity().getTitle().toString());
			options.put("type", isBugs ? "Bugs" : "Hogs");
			if (pak != null) {
				options.put("app", pak.packageName);
				options.put("version", pak.versionName);
				options.put("versionCode", pak.versionCode + "");
				options.put("label", label);
			}
			options.put("benefit", fullObject.textBenefit().replace('\u00B1', '+'));
			ClickTracking.track(uuId, "samplesview", options, getActivity());
		}
	}

	public void setFullObject(SimpleHogBug fullObject) {
		this.fullObject = fullObject;
	}
}
