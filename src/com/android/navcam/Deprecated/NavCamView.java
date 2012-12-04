package com.android.navcam.Deprecated;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.SurfaceHolder;

import com.android.navcam.Model.OrbSignsDetector;
import com.android.navcam.Model.TM_SignsDetector;
import com.android.navcam.Model.TrafficLightsDetector;
import com.android.navcam.Model.Util;

public class NavCamView extends NavCamViewBase {
    private static final String   TAG = "NavCam::View";
    private Mat                   mRgba;

    private OrbSignsDetector so;
    private TrafficLightsDetector tld;
    


    public NavCamView(Context context) {
        super(context);
    }

    @Override
	public void surfaceCreated(SurfaceHolder holder) {
        synchronized (this) {
            // initialize Mats before usage
            mRgba = new Mat();
            
            //so = new OrbSignsDetector(Signs, Filenames);
            tld = new TrafficLightsDetector();
        }

        super.surfaceCreated(holder);
	}

	@Override
	protected Bitmap processFrame(VideoCapture capture) {
		
		switch (MA2.viewmode) {
		case NORMAL:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB); //CV_CAP_ANDROID_COLOR_FRAME_RGBA was guilty in drawing transparent contours! We don't need no transparency.
			break;
		case SEGMENTED:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);
			mRgba = Util.segmentate(mRgba, MA2.hue);
			break;
		case TM_SIGNS:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);
			//mRgba = MainActivity.sd.tm_detectBlueSigns(mRgba);
			break;
		case TMM_SIGNS:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);
			//mRgba = sd.tmm_detectBlueSigns(mRgba);
			break;
		case LIGHTS:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);
			mRgba = tld.detect(mRgba);
			break;
//		case SIGNS_ORB:
//			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);
//			mRgba = so.DetectBlueSigns(mRgba);
//			break;
		}

		Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);

		try {
			Utils.matToBitmap(mRgba, bmp);
			return bmp;
		} catch(Exception e) {
			Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
			bmp.recycle();
			bmp = null;
			return null;
		}
	}

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
            if (mRgba != null)
            {
                mRgba.release();
                mRgba = null;
            }
        }
    }
}
