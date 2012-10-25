package com.android.navcam.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;

public class MainActivity extends Activity {
	private static final String TAG = "NavCam::MainActivity";

	private NavCamView mView;

	enum ViewMode {
		NORMAL, SEGMENTED, SIGNS, LIGHTS
	};

	private MenuItem mItemPreviewNormal;
	private MenuItem mItemPreviewSegmented;
	private MenuItem mItemPreviewSigns;
	private MenuItem mItemPreviewLights;

	public static ViewMode viewmode = ViewMode.NORMAL;
	
	public static int hue = 0;

	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				// Create and set View
				mView = new NavCamView(mAppContext);
				setContentView(mView);
				// Check native OpenCV camera
				if (!mView.openCamera()) {
					AlertDialog ad = new AlertDialog.Builder(mAppContext)
							.create();
					ad.setCancelable(false); // This blocks the 'BACK' button
					ad.setMessage("Fatal error: can't open camera!");
					ad.setButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					});
					ad.show();
				}
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MainActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
		if (mView != null)
			mView.releaseCamera();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
		if (mView != null && !mView.openCamera()) {
			AlertDialog ad = new AlertDialog.Builder(this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't open camera!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			});
			ad.show();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Log.i(TAG, "Trying to load OpenCV library");
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this,
				mOpenCVCallBack)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		mItemPreviewNormal = menu.add("Normal");
		mItemPreviewSegmented = menu.add("Segmented");
		mItemPreviewSigns = menu.add("Signs");
		mItemPreviewLights = menu.add("Lights");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Menu Item selected " + item);
		if (item == mItemPreviewNormal)
			viewmode = ViewMode.NORMAL;
		else if (item == mItemPreviewSegmented)
		{
			viewmode = ViewMode.SEGMENTED;
		
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Hue");
			alert.setMessage("Choose hue for segmentation!");

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  String value = input.getText().toString();
			// Do something with value!
			  try {
				  hue = Integer.parseInt(value);
			  }
			  catch(Exception exc) { exc.printStackTrace(); }
			  
			  }
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			    // Canceled.
			  }
			});

			alert.show();
		}
		else if (item == mItemPreviewSigns)
			viewmode = ViewMode.SIGNS;
		else if (item == mItemPreviewLights)
			viewmode = ViewMode.LIGHTS;
		return true;
	}
}
