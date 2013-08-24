/**
 * @author Ry
 * @date   2013.08.23
 * @filename SplashFragment.java 
 */

package com.marcomobil.everybody;

import java.util.Timer;
import java.util.TimerTask;

import com.marcomobil.everybody.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {

	static final String TAG = SplashActivity.class.getSimpleName();

    private static final int TIMEOUT = 3000;
    private Timer mTransitionTimer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		TimerTask task = new TimerTask() {

            @Override
            public void run() {
            	startActivity(new Intent(SplashActivity.this, MainActivity.class));
            	finish();
            }
        };

        mTransitionTimer = new Timer();
        mTransitionTimer.schedule(task, TIMEOUT);
	}

	@Override
	protected void onDestroy() {
		mTransitionTimer.cancel();
		
		super.onDestroy();
	}
    
}
