package com.android.navcam.View;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.EditText;

import com.android.navcam.R;
import com.android.navcam.Deprecated.OrbSignsDetector;
import com.android.navcam.Model.Beeper;
import com.android.navcam.Model.CM_SignsDetector;
import com.android.navcam.Model.TMM_SignsDetector;
import com.android.navcam.Model.TM_SignsDetector;
import com.android.navcam.Model.Test;
import com.android.navcam.Model.TrafficLightsDetector;
import com.android.navcam.Model.Util;

public class MainActivity extends Activity implements CvCameraViewListener {
	private static final String TAG = "NavCam::MainActivity";

	public enum ViewMode {
		NORMAL, SEGMENTED, TM_SIGNS, CM_SIGNS, LIGHTS, TEST // , SIGNS_ORB
	};

	Beeper b;
	Ringtone r;
	private List<Bitmap> Signs = new ArrayList<Bitmap>();
	String[] Filenames;
	
	

	private TM_SignsDetector tm_sd;
	private TMM_SignsDetector tmm_sd;
	private CM_SignsDetector cm_sd;
	private OrbSignsDetector so;
	private TrafficLightsDetector tld;

	public static ViewMode viewMode = ViewMode.NORMAL;

	public static int hue = 0;

	private Mat mRgba;
	private Mat mIntermediateMat;

	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main_view);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
		
		InitRingtones();
		b = new Beeper();
	}

	@Override
	public void onPause() {
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
	}

	public void onCameraViewStopped() {
		mRgba.release();
		mIntermediateMat.release();
	}

	public Mat onCameraFrame(Mat inputFrame) {
		switch (viewMode) {
		case NORMAL: {
			inputFrame.copyTo(mRgba);
		}
			break;
		case SEGMENTED: {
			inputFrame.copyTo(mIntermediateMat);
			mRgba = Util.segmentate(mIntermediateMat, hue);
		}
			break;
		case TM_SIGNS: {
			mRgba = tm_sd.detect(inputFrame);
			
			if(tm_sd.detected)
			{
				b.Beep(r);
			}
		}
			break;
		case CM_SIGNS: {
			mRgba = cm_sd.detect(inputFrame);
		}
			break;
		case LIGHTS: {
			mRgba = tld.detect(inputFrame);
		}
			break;
		case TEST: {
			inputFrame.copyTo(mRgba);
		}
			break;
		}

		return mRgba;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		mOpenCvCameraView.SetCaptureFormat(Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);

		switch (item.getItemId()) {
		case R.id.vm_normal:
			viewMode = ViewMode.NORMAL;
			break;

		case R.id.vm_segmented: {
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
					} catch (Exception exc) {
						exc.printStackTrace();
					}

				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			alert.show();

			viewMode = ViewMode.SEGMENTED;
		}
			break;
		case R.id.vm_tm_signs: {
			InitTemplatesDir("signs", true);

			if (tm_sd == null)
				tm_sd = new TM_SignsDetector(Signs, Filenames);
			else {
				tm_sd = null;
				tm_sd = new TM_SignsDetector(Signs, Filenames);
			}

			viewMode = ViewMode.TM_SIGNS;
		}
			break;
		case R.id.vm_cm_signs:
			InitTemplatesDir("signs", true);

			if (cm_sd == null)
				cm_sd = new CM_SignsDetector(Signs, Filenames);
			else {
				cm_sd = null;
				cm_sd = new CM_SignsDetector(Signs, Filenames);
			}

			viewMode = ViewMode.CM_SIGNS;
			break;
		case R.id.vm_lights: {
			tld = new TrafficLightsDetector();

			viewMode = ViewMode.LIGHTS;
		}
			break;
		case R.id.vm_test: {
			InitTemplatesDir("test", true);

			viewMode = ViewMode.TEST;

//			if (tm_sd == null)
//				tm_sd = new TM_SignsDetector(Signs, Filenames);
//			else {
//				tm_sd = null;
//				tm_sd = new TM_SignsDetector(Signs, Filenames);
//			}

			// if (tmm_sd == null)
			// tmm_sd = new TMM_SignsDetector(Signs, Filenames);
			// else {
			// tmm_sd = null;
			// tmm_sd = new TMM_SignsDetector(Signs, Filenames);
			// }

			 if (cm_sd == null)
			 cm_sd = new CM_SignsDetector(Signs, Filenames);
			 else {
			 cm_sd = null;
			 cm_sd = new CM_SignsDetector(Signs, Filenames);
			 }
		}
			break;
		}

		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent me) {
		switch (me.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			if (viewMode == ViewMode.TEST) {

				InitTemplatesDir(Environment.getExternalStorageDirectory().getPath() + File.separator + "navcam" + File.separator
						+ "src_images", false);

				// Thread threads[] = new Thread[Signs.size()];

				for (int i = 0; i < Signs.size(); i++) {

					Mat temp = new Mat();
					Utils.bitmapToMat(Signs.get(i), temp);

					Log.i(TAG, "Test for " + Filenames[i] + " started!");

					// Test test = new Test(tm_sd, temp);
					// test.run();

					Test t = new Test(cm_sd, temp);
					t.run();
				}

				Log.i(TAG, "All tests finished!");

				// for (int i = 0; i < threads.length; i++) {
				// try {
				// threads[i].join();
				// } catch (InterruptedException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				// }
				//
				// Log.i(TAG, "All test threads finished!");

			} else {
				Util.saveImage(mRgba, "screenshot" + System.currentTimeMillis());
			}
		}
		}

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		onTrackballEvent(me);

		return true;
	}

	private void InitTemplatesDir(String dir, boolean assets) {
		Signs.clear();

		if (assets) {
			AssetManager am = getAssets();

			try {
				Filenames = am.list(dir);
				for (int i = 0; i < Filenames.length; i++) {
					Log.i(TAG, "File #" + i + " = " + Filenames[i]);
					Log.i(TAG, "Path: " + dir + File.separator + Filenames[i]);

					InputStream is = am.open(dir + File.separator + Filenames[i]);
					Bitmap b = BitmapFactory.decodeStream(is);

					Signs.add(b);

					// b.recycle();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				File sd_dir = new File(dir);
				Filenames = sd_dir.list();
				for (int i = 0; i < Filenames.length; i++) {
					Log.i(TAG, "File #" + i + " = " + Filenames[i]);
					Log.i(TAG, "Path: " + dir + File.separator + Filenames[i]);

					// Mat m = Highgui.imread(sd_dir + File.separator + Filenames[i], Highgui.CV_LOAD_IMAGE_UNCHANGED);
					// InputStream is = sd_dir.(dir + File.separator + Filenames[i]);
					Bitmap b = BitmapFactory.decodeFile(sd_dir + File.separator + Filenames[i]);

					Signs.add(b);

					// b.recycle();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void InitRingtones() {
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
	}
}
