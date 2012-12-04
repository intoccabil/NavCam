package com.android.navcam.Model;

import org.opencv.core.Mat;

import android.util.Log;

public class Test implements Runnable {

	private static final String TAG = "NavCam::Test";

	Detector detector;
	Mat image;
	String Statistics;

	public Test(Detector d, Mat src_img) {
		detector = d;
		image = src_img.clone();

		Log.i(TAG, "Test for " + d.getClass() + " initialized!");
	}

	public void run() {
		synchronized (this) {
			detector.detect(image);
			Statistics = detector.getStats();
		
			long time = System.currentTimeMillis();
			
			Util.saveStats(Statistics, "test_result_" + time);
			Util.saveImage(image, "test_screenshot_" + time);

			Log.i(TAG, "Test finished!");
		}
	}
}
