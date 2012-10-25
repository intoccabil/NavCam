package com.android.navcam.ViewModel;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

import com.android.navcam.Model.Detector;
import com.android.navcam.Model.NavCamViewBase;
import com.android.navcam.Model.Util;
import com.android.navcam.View.MainActivity;

public class NavCamView extends NavCamViewBase {
    private static final String   TAG = "NavCam::View";
    private Mat                   mRgba;
    
    private Detector sd;

    public NavCamView(Context context) {
        super(context);
    }

    @Override
	public void surfaceCreated(SurfaceHolder holder) {
        synchronized (this) {
            // initialize Mats before usage
            mRgba = new Mat();
            sd = new Detector();
        }

        super.surfaceCreated(holder);
	}

	@Override
	protected Bitmap processFrame(VideoCapture capture) {
		
		switch (MainActivity.viewmode) {
		case NORMAL:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
			break;
		case SEGMENTED:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
			mRgba = Util.Segmentate(mRgba, MainActivity.hue);
			break;
		case SIGNS:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
			mRgba = sd.DetectBlueSigns(mRgba);
			break;
		case LIGHTS:
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
			mRgba = sd.DetectRedLights(mRgba);
			break;
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
