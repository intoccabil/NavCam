package com.android.navcam.Model;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class Util {
	private static final String TAG = "NavCam::Util";
	
	public static Mat Segmentate(Mat image, int hue) {

		Mat hsv_image, h_channel, s_channel;

		hsv_image = new Mat();
//		h_channel = new Mat();
//		s_channel = new Mat();

		Imgproc.cvtColor(image, hsv_image, Imgproc.COLOR_RGB2HSV);

//		try {
//			Core.extractChannel(hsv_image, h_channel, 0);
//			Core.extractChannel(hsv_image, s_channel, 1);
//		} catch (Exception e) {
//			Log.e(TAG, e.getMessage());
//		}

		int min_hue, max_hue;

		min_hue = hue - 20;
		if (min_hue < 0)
			min_hue = 0;

		max_hue = hue + 20;
		if (max_hue > 180)
			max_hue = 180;
		
		Core.inRange(hsv_image, new Scalar(min_hue, 128, 50), new Scalar(max_hue, 255, 255), hsv_image);
		//Core.inRange(s_channel, new Scalar(128), new Scalar(255), s_channel);

		Log.i(TAG, "Performing segmentation");

		//Imgproc.threshold(s_channel, s_channel, 200, 200, Imgproc.THRESH_BINARY);

		//Core.bitwise_and(h_channel, s_channel, h_channel);

		//Imgproc.medianBlur(h_channel, h_channel, 5);

		//hsv_image.release();
		//s_channel.release();

		return hsv_image;
	}
}
