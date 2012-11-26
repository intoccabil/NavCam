package com.android.navcam.Model;

import org.opencv.core.Mat;

public interface Detector {
	public Mat detect(Mat image);
	public String getStats();
}
