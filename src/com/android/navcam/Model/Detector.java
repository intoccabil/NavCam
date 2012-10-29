package com.android.navcam.Model;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class Detector {
	private static final String TAG = "NavCam::SignsDetector";

	int canny_threshold = 200;
	int canny_threshold_linking = 400;

	public Detector() {

	}

	// Method for blue signs detection. Segmentation -> Canny edge detection ->
	// Contours acquisition -> Convex hull acquisition -> Approximation ->
	// -> Recognition.
	public Mat DetectBlueSigns(Mat image) {
		Mat temp = new Mat(image.size(), CvType.CV_8UC3);
		Mat canny_blue = new Mat(image.size(), CvType.CV_8UC1);
		List<MatOfPoint> all_contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> good_contours = new ArrayList<MatOfPoint>();

		temp = Util.Segmentate(image, 90);

		Imgproc.medianBlur(temp, temp, 5);

		Imgproc.Canny(temp, canny_blue, canny_threshold, canny_threshold_linking);
		Log.i(TAG, "Blue canny");

		Imgproc.findContours(canny_blue, all_contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		temp.release();
		temp = null;

		if (canny_blue != null) {
			canny_blue.release();
			canny_blue = null;
		}

		for (MatOfPoint contour : all_contours) {
			// Declarations
			// TODO: These should probably be private class members so matrices are not allocated every time a frame is processed.
			MatOfInt hull = new MatOfInt();
			Imgproc.convexHull(contour, hull);

			MatOfPoint2f contour2f = new MatOfPoint2f();
			MatOfPoint2f mat_hull2f = new MatOfPoint2f();
			MatOfPoint mat_hull_approximated = new MatOfPoint();

			MatOfPoint mat_hull = new MatOfPoint();

			if (hull != null) {
				// Points of the whole contour
				Point[] mplist = contour.toArray();
				// Hull points' indices
				int[] intlist = hull.toArray();

				List<Point> plist = new ArrayList<Point>();

				// Filling list of points with hull points (indices are stored in MatOfInt hull). This abomination is
				// required because OpenCV4Android lacks proper method signatures.
				for (int i = 0; i < intlist.length; i++) {
					plist.add(mplist[intlist[i]]);
				}

				mat_hull.fromList(plist);

				mat_hull.convertTo(contour2f, CvType.CV_32FC2);

				// This is a result of another lacking method signature. Need to convert between MatOfPoint and
				// MatOfPoint2f, which is oh so beneficial performance-wise /s.
				Imgproc.approxPolyDP(contour2f, mat_hull2f, Imgproc.arcLength(contour2f, true) * 0.03, true);

				mat_hull2f.convertTo(mat_hull_approximated, CvType.CV_32S);

				// Releasing section. I doubt it's done correctly (is null-assigning necessary here?).
				hull.release();
				hull = null;
				contour2f.release();
				contour2f = null;
				mat_hull2f.release();
				mat_hull2f = null;
			}

			if ((Imgproc.contourArea(mat_hull_approximated) > (image.size().width * image.size().height / 80))
					&& mat_hull_approximated.total() == 4) {

				good_contours.add(mat_hull_approximated);

				// Seems to release correctly now. Although suspicious.
				if (mat_hull != null) {
					mat_hull.release();
					mat_hull = null;
				}
			}
		}

		// Mat result = new Mat(image.size(), CvType.CV_8UC1);

		if (good_contours != null) {
			Imgproc.drawContours(image, good_contours, -1, new Scalar(255, 0, 0), 2);
		}

		return image;
	}

	// Not implemented correctly yet.
	public Mat DetectRedLights(Mat image) {
		Mat temp = new Mat();
		Mat canny_red = new Mat();

		List<MatOfPoint> all_contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> good_contours = new ArrayList<MatOfPoint>();

		temp = Util.Segmentate(image, 180);

		Imgproc.medianBlur(temp, temp, 5);

		Imgproc.Canny(temp, canny_red, canny_threshold, canny_threshold_linking);
		Log.i(TAG, "Red canny");

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

			if ((Imgproc.contourArea(contour) > (image.size().width * image.size().height / 80))
					&& (Math.abs(contour.width() - contour.height()) < 30)) {
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
			Imgproc.drawContours(image, good_contours, -1, new Scalar(255, 0, 0), -1);
			// Core.fillConvexPoly(image, good_contours.get(0), new Scalar(255, 0, 0));
		}

		return image;
	}
}
