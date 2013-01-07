// TODO Refactor the whole class (might raise the performance)

package com.android.navcam.Deprecated;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import org.opencv.ml.*;

import com.android.navcam.Model.Util;

import android.content.pm.FeatureInfo;
import android.graphics.Bitmap;
import android.util.Log;

public class OrbSignsDetector {
	private static final String TAG = "NavCam::SD";

	int canny_threshold = 200;
	int canny_threshold_linking = 400;

	List<Mat> _SignTemplates = new ArrayList<Mat>();
	List<MatOfKeyPoint> _STKeypoints = new ArrayList<MatOfKeyPoint>();
	List<Mat> _TemplateDescriptors = new ArrayList<Mat>();
	String[] _SignNames;

	FeatureDetector fd = FeatureDetector.create(FeatureDetector.BRISK);
	DescriptorExtractor de = DescriptorExtractor.create(DescriptorExtractor.BRISK);
	DescriptorMatcher dm = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

	/**
	 * This class implements a traffic signs detector based on ORB Feature detector.
	 * 
	 * @param Signs
	 *            List of Bitmap signs [150x150] must be provided to construct the class
	 */
	public OrbSignsDetector(List<Bitmap> Signs) {
		for (Bitmap sign : Signs) {
			try {
				Mat temp = new Mat();
				Utils.bitmapToMat(sign, temp);
				Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);
				_SignTemplates.add(temp);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}

		for (int i = 0; i < _SignTemplates.size(); i++) {
			Log.i(TAG, _SignTemplates.get(i).toString());
		}
	}

	/**
	 * 
	 * @param Signs
	 *            List of Bitmap signs [150x150] must be provided to construct the class
	 * @param Filenames
	 *            Array of filenames (corresponding with signs) is provided for identification of detected signs
	 */
	public OrbSignsDetector(List<Bitmap> Signs, String[] Filenames) {

		this._SignNames = Filenames.clone();

		for (Bitmap sign : Signs) {
			try {
				Mat temp = new Mat();
				Utils.bitmapToMat(sign, temp);
				// Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGBA2RGB);
				Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGBA2GRAY);
				_SignTemplates.add(temp);

			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}

		fd.detect(_SignTemplates, _STKeypoints);
		de.compute(_SignTemplates, _STKeypoints, _TemplateDescriptors);

		// dm.add(_TemplateDescriptors);
		// dm.train();

		for (int i = 0; i < _SignTemplates.size(); i++) {
			Log.i(TAG, _SignTemplates.get(i).toString());
		}
	}

	/**
	 * Method for blue signs detection. Segmentation -> Canny edge detection -> Contours acquisition -> Convex hull acquisition ->
	 * Approximation -> Template matching recognition.
	 * 
	 * @param Input
	 *            image
	 * @return Output image with signs boundaries, names and correlation quotient drawn on it
	 */
	public Mat DetectBlueSigns(Mat image) {
		Mat temp = new Mat(image.size(), CvType.CV_8UC3);
		Mat canny_blue = new Mat(image.size(), CvType.CV_8UC1);
		Mat matching_result = new Mat();
		Log.d(TAG, "Matching result created: " + matching_result.toString());
		Mat extracted_sign = new Mat();

		List<MatOfPoint> all_contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> good_contours = new ArrayList<MatOfPoint>();

		// List<Mat> signs_rects = new ArrayList<Mat>();

		temp = Util.segmentate(image, 90);

		Imgproc.medianBlur(temp, temp, 5);

		Imgproc.Canny(temp, canny_blue, canny_threshold, canny_threshold_linking);
		// Log.d(TAG, "Blue canny");

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
				// mat_hull2f.release();
				// mat_hull2f = null;
				mat_hull.release();
				mat_hull = null;
			}

			if ((Imgproc.contourArea(mat_hull_approximated) > (image.size().width * image.size().height / 80))
					&& mat_hull_approximated.total() == 4) {

				good_contours.add(mat_hull_approximated);

				// signs_rects.clear();
				// signs_rects.add(image.submat(Imgproc.boundingRect(mat_hull_approximated)));

				Imgproc.resize(image.submat(Imgproc.boundingRect(mat_hull_approximated)), extracted_sign, new Size(150, 150));
				Imgproc.cvtColor(extracted_sign, extracted_sign, Imgproc.COLOR_BGR2GRAY);
				// Imgproc.threshold(extracted_sign, extracted_sign, 200, 255, Imgproc.THRESH_BINARY);

				MatOfDMatch max_matches = new MatOfDMatch();
				int num = 0;

				MatOfKeyPoint kp = new MatOfKeyPoint();
				fd.detect(extracted_sign, kp);

				Mat descriptors = new Mat();
				de.compute(extracted_sign, kp, descriptors);

				// for (int i = 0; i < _SignTemplates.size(); i++) {

				MatOfDMatch current_matches = new MatOfDMatch();

				dm.match(descriptors, _TemplateDescriptors.get(2), current_matches);

				Log.w("FEATURE", "===================START=====================");
				for (int j = 0; j < current_matches.size().area(); j++) {
					Log.w("FEATURE", "FP #" + (int) current_matches.get(j, 0)[0] + " = " + (int) current_matches.get(j, 0)[1] + "; "
							+ current_matches.get(j, 0)[2] + "; " + current_matches.get(j, 0)[3]);
				}
				Log.w(TAG, "===================END=====================");

				MatOfDMatch good_matches = new MatOfDMatch();

				// current_matches.

				// Log.w(TAG, String.format("%f", current_matches.size().area()));

				// if (current_matches.size().area() > max_matches.size().area()) {
				// max_matches = current_matches;
				// num = 2;
				// }
				// }

				Features2d.drawMatches(extracted_sign, kp, _SignTemplates.get(2), _STKeypoints.get(2), current_matches, matching_result,
						new Scalar(255, 0, 0), new Scalar(0, 255, 0), new MatOfByte(), 0);
				// Imgproc.resize(matching_result, matching_result, new Size(320, 221));
			}
		}

		// Core.putText(image.submat(Imgproc.boundingRect(mat_hull_approximated)), _SignNames[num].split("\\.(?=[^\\.]+$)")[0],
		// new Point(5, 20), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 255, 0));
		// Core.putText(image.submat(Imgproc.boundingRect(mat_hull_approximated)), String.format("%.3f", max_corr), new Point(5,
		// 40), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 255, 0));

		// if (extracted_sign.width() > 0) {
		// return extracted_sign;
		// } else
		// return image;

		// if (good_contours != null) {
		// Imgproc.drawContours(image, good_contours, -1, new Scalar(255, 255, 0), 2);
		// }

		if (matching_result.width() > 0) {
			return matching_result;
		} else
			return image;
	}
}