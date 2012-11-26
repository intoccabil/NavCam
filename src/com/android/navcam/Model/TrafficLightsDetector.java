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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.util.Log;

public class TrafficLightsDetector {
	private static final String TAG = "NavCam::RLD";
	
	int canny_threshold = 200;
	int canny_threshold_linking = 400;
	
	// Not implemented correctly yet.
	public Mat DetectRedLights(Mat image) {
		Mat temp = new Mat(image.size(), CvType.CV_8UC3);
		Mat canny_red = new Mat(image.size(), CvType.CV_8UC1);

		List<MatOfPoint> all_contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> good_contours = new ArrayList<MatOfPoint>();

		temp = Util.segmentate(image, 180);

		Imgproc.medianBlur(temp, temp, 5);

		Imgproc.Canny(temp, canny_red, canny_threshold, canny_threshold_linking);
		// Log.d(TAG, "Red canny");

		Imgproc.findContours(canny_red, all_contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		temp.release();
		temp = null;

		if (canny_red != null) {
			canny_red.release();
			canny_red = null;
		}

		for (MatOfPoint contour : all_contours) {

			// MatOfInt hull = new MatOfInt();
			// Imgproc.convexHull(contour, hull);
			//
			// MatOfPoint mat_hull = new MatOfPoint();
			//
			// if (contour != null) {
			// Point[] mplist = contour.toArray();
			// int[] intlist = hull.toArray();
			//
			// List<Point> plist = new ArrayList<Point>();
			//
			// for (int i = 0; i < intlist.length; i++) {
			// plist.add(mplist[intlist[i]]);
			// }
			//
			// mat_hull.fromList(plist);
			// }
			//
			// if (hull != null) {
			// hull.release();
			// hull = null;
			// }

			// mat_hull.release();

			if ((Imgproc.contourArea(contour) > (image.size().width * image.size().height / 80))) {
				Log.d(TAG, "width = " + contour.size().width + "height = " + contour.size().height);
				// && (Math.abs(contour.width() - contour.height()) < 60)) {
				// MatOfPoint2f contour2f = new MatOfPoint2f();
				// mat_hull.convertTo(contour2f, CvType.CV_32FC2);
				// MatOfPoint2f mat_hull2f = new MatOfPoint2f();
				// Imgproc.approxPolyDP(contour2f, mat_hull2f, 0.03, true);
				// MatOfPoint mat_hull_approximated = new MatOfPoint();
				// mat_hull2f.convertTo(mat_hull_approximated, CvType.CV_32S);
				//
				// contour2f.release();
				// contour2f = null;
				// mat_hull2f.release();
				// mat_hull2f = null;

				good_contours.add(contour);

				// if (mat_hull != null) {
				// mat_hull.release();
				// mat_hull = null;
				// }
			}
		}

		// Mat result = new Mat(image.size(), CvType.CV_8UC1);

		if (good_contours.size() > 0) {
			// Imgproc.drawContours(image, good_contours, -1, new Scalar(255, 255, 0), -1);
			Core.fillConvexPoly(image, good_contours.get(0), new Scalar(255, 0, 0));
		}

		return image;
	}
}
