package com.android.navcam.Deprecated;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import com.android.navcam.Model.TM_SignsDetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;


public class MA2 extends Activity {
	private static final String TAG = "NavCam::MainActivity";

	private NavCamView mView;

	public enum ViewMode {
		NORMAL, SEGMENTED, TM_SIGNS, TMM_SIGNS, LIGHTS //, SIGNS_ORB
	};
	
    private List<Bitmap> Signs = new ArrayList<Bitmap>();
    String[] Filenames;

    public TM_SignsDetector sd;
    
	private MenuItem mItemPreviewNormal;
	private MenuItem mItemPreviewSegmented;
	private MenuItem mItemPreviewTMSigns;
	private MenuItem mItemPreviewTMMSigns;
	private MenuItem mItemPreviewLights;
//	private MenuItem mItemPreviewSORB;

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

	public MA2() {
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
		mItemPreviewTMSigns = menu.add("TM Signs");
		mItemPreviewTMMSigns = menu.add("TMM Signs");
		mItemPreviewLights = menu.add("Lights");
//		mItemPreviewSORB = menu.add("Signs ORB");
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
		else if (item == mItemPreviewTMSigns)
		{
			AssetManager am = getAssets();
			
			try {
				Filenames = am.list("signs");
				for(int i = 0; i < Filenames.length; i++) {
					Log.i(TAG, "File #" + i + " = " + Filenames[i]);
					Log.i(TAG, "Path: " + "signs" + File.separator + Filenames[i]);
					
					InputStream is = am.open("signs" + File.separator + Filenames[i]);
					Bitmap b = BitmapFactory.decodeStream(is);
					
					Signs.add(b);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			sd = new TM_SignsDetector(Signs, Filenames);
			
			viewmode = ViewMode.TM_SIGNS;
		}
		else if (item == mItemPreviewTMMSigns)
			viewmode = ViewMode.TMM_SIGNS;
		else if (item == mItemPreviewLights)
			viewmode = ViewMode.LIGHTS;
//		else if (item == mItemPreviewSORB)
//			viewmode = ViewMode.SIGNS_ORB;
		return true;
	}
}
