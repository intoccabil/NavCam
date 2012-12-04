package com.android.navcam.Model;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.util.Log;

public class TrafficLightsDetector implements Detector {
	private static final String TAG = "NavCam::RLD";

	int canny_threshold = 200;
	int canny_threshold_linking = 400;

	public Mat detect(Mat image) {
		Mat temp = new Mat(image.size(), CvType.CV_8UC3);
		Mat canny_red = new Mat(image.size(), CvType.CV_8UC1);

		List<MatOfPoint> all_contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> good_contours = new ArrayList<MatOfPoint>();

		temp = Util.segmentate(image, 0);

		Imgproc.medianBlur(temp, temp, 5);

		Imgproc.Canny(temp, canny_red, canny_threshold, canny_threshold_linking);
		// Log.d(TAG, "Red canny");

		Imgproc.findContours(canny_red, all_contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

		if (canny_red != null) {
			canny_red.release();
			canny_red = null;
		}

		for (MatOfPoint contour : all_contours) {

			if (contour.total() > 25) {

				MatOfPoint2f contour2f = new MatOfPoint2f();
				contour.convertTo(contour2f, CvType.CV_32FC2);

				RotatedRect br = Imgproc.fitEllipse(contour2f);

				Mat extracted_sign = new Mat();

				temp.submat(Imgproc.boundingRect(contour)).copyTo(extracted_sign);
				Scalar mean = Core.mean(extracted_sign);

				if (Imgproc.contourArea(contour) > image.size().area() * 0.001 && mean.val[0] > 150
						&& Math.abs(br.size.width - br.size.height) < (br.size.width + br.size.height) * 0.3) {

					Core.ellipse(image, Imgproc.fitEllipse(contour2f), new Scalar(255, 255, 0, 255), -1);
				}
			}
		}

		return image;
	}

	public String getStats() {
		return "";
	}
}
