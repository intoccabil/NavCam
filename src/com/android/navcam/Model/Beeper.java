package com.android.navcam.Model;

import java.util.Timer;
import java.util.TimerTask;

import android.media.Ringtone;
import android.util.Log;

public class Beeper {

	private static final String TAG = "NavCam::Beeper";

	Timer timer;

	private boolean hasBeeped = false;

	public void Beep(final Ringtone r) {
		if (hasBeeped) {
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					hasBeeped = false;
					timer.cancel();
					Log.d(TAG, "Unbeep!");
				}
			}, 10000);
			timer.purge();
		} else {

			timer = new Timer();
			r.play();
			Log.d(TAG, "Beep!");
			hasBeeped = true;
		}
	}

}
