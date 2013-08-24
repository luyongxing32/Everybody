/**
 * @author Ry
 * @date 2013.08.23
 * @filename MainActivity.java
 */

package com.marcomobil.everybody;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.marcomobil.everybody.R;
import com.revmob.RevMob;

public class MainActivity extends Activity implements OnClickListener {

	static final String TAG = MainActivity.class.getSimpleName();
	
	private static final int TIMEOUT = 10000;
    private Timer mAdTimer;
	
	// Button for posting to facebook
	private Button mBtnShare;
	
	// Button for purchasing inapp
	private Button mBtnPurchase;
	
	// Revmob object
    private RevMob mRevmob;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		mBtnShare = (Button) findViewById(R.id.button_facebook_post);
		mBtnShare.setOnClickListener(this);
		
		mBtnPurchase = (Button) findViewById(R.id.button_purchase);
		mBtnPurchase.setOnClickListener(this);
		
		InAppBilling.initConfiguration(this);
		
		initRevMob();
	}

	@Override
	protected void onDestroy() {
		mAdTimer.cancel();
		InAppBilling.release();
		
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		switch (id) {
		case R.id.button_facebook_post:
			Config.shareWithFacebook(this, Config.UPLOAD_PHOTO_PATH);
			break;

		case R.id.button_purchase:
            try {
                InAppBilling.purchaseBecomePromember();
            } catch (Exception e) {
                if (Config.DEBUG) e.printStackTrace();
            }
			break;
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!InAppBilling.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            if (Config.DEBUG) Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
	
	public void changeStateOnPurchaseButton(boolean isPerchased) {
		if (isPerchased) {
			mBtnPurchase.setBackgroundResource(R.drawable.purchased);
			mBtnPurchase.setClickable(false);
		} else {
			mBtnPurchase.setBackgroundResource(R.drawable.buy_back);
			mBtnPurchase.setClickable(true);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// RevMob Setting
	////////////////////////////////////////////////////////////////////////////////

	private void initRevMob() {
		mRevmob = RevMob.start(this, Config.REVMOB_APP_ID);
		
		TimerTask task = new TimerTask() {

            @Override
            public void run() {
            	showRevMobFullScreenAd();
            }
        };

        mAdTimer = new Timer();
        mAdTimer.schedule(task, TIMEOUT);
	}

	public void showRevMobFullScreenAd() {
		mRevmob.showFullscreen(this);
	}

}
