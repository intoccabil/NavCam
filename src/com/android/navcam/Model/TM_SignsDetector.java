// TODO Refactor the whole class (might raise the performance)

package com.android.navcam.Model;

import java.util.ArrayList;
import java.util.Calendar;
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
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeUtils;

public class TM_SignsDetector implements Detector {
	private static final String TAG = "NavCam::TM_SD";

	private String Statistics = "";

	int canny_threshold = 250;
	int canny_threshold_linking = 400;

	List<Mat> _SignTemplates = new ArrayList<Mat>();
	String[] _SignNames;

	/**
	 * This class implements a traffic signs detector based on template matching. This is a very slow method and thus it's provided for
	 * academic purpose only.
	 * 
	 * @param Signs
	 *            List of Bitmap signs [150x150] must be provided to construct the class
	 */
	public TM_SignsDetector(List<Bitmap> Signs) {
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
	public TM_SignsDetector(List<Bitmap> Signs, String[] Filenames) {

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
	public Mat detect(Mat image) {
		final long t_start = System.currentTimeMillis();
		Statistics += "Stats for TM_SD at " + Calendar.getInstance().getTime() + "\n\n";

		Mat temp = new Mat();
		Mat matching_result = new Mat();
		Mat extracted_sign = new Mat();

		List<MatOfPoint> all_contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> good_contours = new ArrayList<MatOfPoint>();

		temp = Util.segmentate(image, 95);

		//Imgproc.medianBlur(temp, temp, 3);

		Imgproc.Canny(temp, temp, canny_threshold, canny_threshold_linking);
		
		all_contours.clear();

		Imgproc.findContours(temp, all_contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

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
			
			if ((Imgproc.contourArea(mat_hull_approximated) > (image.size().width * image.size().height / 90))
					&& mat_hull_approximated.total() == 4) {

				for (int i = 0; i < mat_hull_approximated.total(); i++) {
					Log.d(TAG,
							Double.toString(mat_hull_approximated.get(i, 0)[0]) + " " + Double.toString(mat_hull_approximated.get(i, 0)[1]));
				}

				good_contours.add(mat_hull_approximated);

				Log.d(TAG, "Extracted sign created: " + extracted_sign.toString());

				Imgproc.threshold(extracted_sign, extracted_sign, 150, 255, Imgproc.THRESH_BINARY);

				double max_corr = 0;
				int num = 0;

				for (int i = 0; i < _SignTemplates.size(); i++) {
					final long recog_start = System.currentTimeMillis();
					
					Imgproc.resize(image.submat(Imgproc.boundingRect(mat_hull_approximated)), extracted_sign, _SignTemplates.get(i).size());
					Imgproc.cvtColor(extracted_sign, extracted_sign, Imgproc.COLOR_RGBA2GRAY);

					Mat thresholded_sign = new Mat();
					// Log.d(TAG, "Thresholded sign created: " + thresholded_sign.toString());

					Imgproc.threshold(_SignTemplates.get(i), thresholded_sign, 150, 255, Imgproc.THRESH_BINARY);
					// Imgproc.cvtColor(thresholded_sign, thresholded_sign, Imgproc.COLOR_GRAY2RGB);

					Imgproc.matchTemplate(extracted_sign, _SignTemplates.get(i), matching_result, Imgproc.TM_CCORR_NORMED);
					
					Log.w(TAG, _SignNames[i] + ": " + matching_result.get(0, 0)[0]);

					if (matching_result.get(0, 0)[0] > max_corr) {
						max_corr = matching_result.get(0, 0)[0];
						num = i;
					}

					final long recog_end = System.currentTimeMillis() - recog_start;
					Log.i(TAG, Long.toString(recog_end) + "ms recog time for " + _SignNames[i]);

					Statistics += _SignTemplates.get(i).size().width + ";" + recog_end + ";" + matching_result.get(0, 0)[0] + "\n";
					//Util.saveImage(image, "tm_sd_" + i);
				}

				if (max_corr >= 0.9) {
					Core.putText(image.submat(Imgproc.boundingRect(mat_hull_approximated)), _SignNames[num].split("\\.(?=[^\\.]+$)")[0],
							new Point(5, 20), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 255, 0));
					Core.putText(image.submat(Imgproc.boundingRect(mat_hull_approximated)), String.format("%.3f", max_corr), new Point(5,
							40), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 255, 0));
				}
			}
		}

		// if (extracted_sign.width() > 0) {
		// return extracted_sign;
		// } else
		// return image;

		if (good_contours != null) {
			Imgproc.drawContours(image, good_contours, -1, new Scalar(255, 255, 0, 255), 2);
			good_contours.clear();
		}

		final long t_end = System.currentTimeMillis() - t_start;
		Log.i(TAG, Long.toString(t_end) + "ms");
		Statistics += "\n" + "Overall elapsed " + t_end + " ms\n\n";

		return image;
	}

	public String getStats() {
		if (Statistics.length() > 0) {
			return Statistics;
		}
		else return "No stats!";
	}
}
