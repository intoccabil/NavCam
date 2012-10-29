package com.android.navcam.Model;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class Util {
	private static final String TAG = "NavCam::Util";
	
	public static Mat Segmentate(Mat image, int hue) {

		Mat hsv_image = new Mat(image.size(), CvType.CV_8UC1);

		Imgproc.cvtColor(image, hsv_image, Imgproc.COLOR_RGB2HSV);

		int min_hue, max_hue;

		min_hue = hue - 20;
		if (min_hue < 0)
			min_hue = 0;

		max_hue = hue + 20;
		if (max_hue > 180)
			max_hue = 180;
		
		Core.inRange(hsv_image, new Scalar(min_hue, 128, 50), new Scalar(max_hue, 255, 255), hsv_image);

		Log.i(TAG, "Performing segmentation");

		return hsv_image;
	}
}
