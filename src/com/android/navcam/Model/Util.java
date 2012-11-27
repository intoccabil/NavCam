package com.android.navcam.Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

public class Util {
	private static final String TAG = "NavCam::Util";

	public static Mat segmentate(Mat image, int hue) {

		Mat hsv_image;

		hsv_image = new Mat(image.size(), CvType.CV_8UC1);

		try {
			Imgproc.cvtColor(image, hsv_image, Imgproc.COLOR_RGB2HSV);

			int min_hue, max_hue;

			min_hue = hue - 25;
			if (min_hue < 0)
				min_hue = 0;

			max_hue = hue + 25;
			if (max_hue > 180)
				max_hue = 180;

			Core.inRange(hsv_image, new Scalar(min_hue, 128, 50), new Scalar(max_hue, 255, 255), hsv_image);

			Log.d(TAG, "Performing segmentation");

			//Imgproc.cvtColor(hsv_image, hsv_image, Imgproc.COLOR_HSV2RGB_FULL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return hsv_image;
	}

	public static void saveStats(String src, String filename) {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) // SDcard is available
		{
			String _filename = filename + ".csv";
			File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "navcam");
			dir.mkdirs();
			File file = new File(dir, _filename);
			FileOutputStream fos = null;

			try {
				fos = new FileOutputStream(file);
			} catch (FileNotFoundException fnfe) {
				// TODO Auto-generated catch block
				fnfe.printStackTrace();
			}

			try {
				if (src.length() > 0) {
					OutputStreamWriter myOutWriter = new OutputStreamWriter(fos);
					myOutWriter.write(src);

					myOutWriter.flush();
					myOutWriter.close();
				}

				fos.flush();
				fos.close();

				Log.d(TAG, "Written file" + _filename);
			} catch (IOException e) {
				e.printStackTrace();
				// handle exception
			}
			
			Log.i(TAG, "Stats saved!");
		}
	}
	
	public static void saveImage(Mat image, String filename) {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) // SDcard is available
		{
			String _filename = "nc_" + filename + ".png";
			File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "navcam");
			dir.mkdirs();
			File file = new File(dir, _filename);
			FileOutputStream fos = null;

			try {
				fos = new FileOutputStream(file);
			} catch (FileNotFoundException fnfe) {
				// TODO Auto-generated catch block
				fnfe.printStackTrace();
			}

			try {
				if (image.width() > 0) {
					Bitmap b = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
					Utils.matToBitmap(image, b);
					b.compress(Bitmap.CompressFormat.PNG, 90, fos);
				}
			} catch (IllegalArgumentException iex) {
				iex.printStackTrace();
			}

			try {
				fos.flush();
				fos.close();

				Log.d(TAG, "Written file" + _filename);
			} catch (IOException e) {
				e.printStackTrace();
				// handle exception
			}
			
			Log.i(TAG, "Screenshot saved!");
		}
	}
	
	//TODO: Not working ATM
	public static Mat getFourier(Mat image) {
		Mat padded = new Mat(); // expand input image to optimal size
		int m = Core.getOptimalDFTSize(image.rows());
		int n = Core.getOptimalDFTSize(image.cols()); // on the border add zero pixels
		Imgproc.copyMakeBorder(image, padded, 0, m - image.rows(), 0, n - image.cols(), Imgproc.BORDER_CONSTANT);

		padded.convertTo(padded, CvType.CV_32FC1);
		Imgproc.cvtColor(padded, padded, Imgproc.COLOR_RGB2GRAY);

		List<Mat> planes = new ArrayList<Mat>();

		planes.add(padded);
		planes.add(Mat.zeros(padded.size(), CvType.CV_32F));

		Mat complexI = new Mat();

		Core.merge(planes, complexI);

		Core.dft(padded, padded);

		Imgproc.cvtColor(padded, padded, Imgproc.COLOR_GRAY2RGB);

		return padded;
	}
}
