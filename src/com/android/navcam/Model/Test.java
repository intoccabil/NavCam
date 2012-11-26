package com.android.navcam.Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import org.opencv.core.Mat;

import android.os.Environment;
import android.util.Log;

public class Test {

	private static final String TAG = "NavCam::Test";

	Detector detector;
	Mat image;
	String Statistics;

	public Test(Detector d, Mat src_img) {
		detector = d;
		image = src_img.clone();
		
		Log.i(TAG, "Test for " + d.getClass() + " initialized!");
	}

	public boolean runTest() {
		detector.detect(image);
		Statistics = detector.getStats();
		saveStats();

		return true;
	}

	private void saveStats() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) // SDcard is available
		{
			String filename = "nc_" + "stats" + "_" + Calendar.getInstance().getTime().getHours() + "-" + Calendar.getInstance().getTime().getMinutes() + ".csv";
			File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "navcam");
			dir.mkdirs();
			File file = new File(dir, filename);
			FileOutputStream fos = null;

			try {
				fos = new FileOutputStream(file, true);
			} catch (FileNotFoundException fnfe) {
				// TODO Auto-generated catch block
				fnfe.printStackTrace();
			}

			try {
				if (Statistics.length() > 0) {
					OutputStreamWriter myOutWriter = new OutputStreamWriter(fos);
					myOutWriter.write(Statistics);

					myOutWriter.flush();
					myOutWriter.close();
				}

				fos.flush();
				fos.close();

				Log.d(TAG, "Written file" + filename);
			} catch (IOException e) {
				e.printStackTrace();
				// handle exception
			}
		}
	}
}
