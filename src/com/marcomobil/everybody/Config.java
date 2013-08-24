/**
 * @author Ry
 * @date 2013.08.23
 * @filename Config.java
 */

package com.marcomobil.everybody;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONObject;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.AssetManager.AssetInputStream;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.SessionEvents;
import com.facebook.android.SessionEvents.AuthListener;
import com.facebook.android.SessionEvents.LogoutListener;
import com.facebook.android.SessionStore;
import com.facebook.android.Util;
import com.marcomobil.everybody.R;

public class Config {

    public static final boolean DEBUG = false;
    
    public static final String UPLOAD_PHOTO_PATH = "Everybody.png";
    
    // DOWNLOAD URL
    public static final String DOWNLOAD_URL = "https://play.google.com/store/apps/details?id=com.marcomobil.everybody";

    // REVMOB
    public static final String REVMOB_APP_ID = "521732c19b82fd49ea000050";

    // Facebook
    public static final String FACEBOOK_APP_ID = "658116450868327";
    public static final String FACEBOOK_APP_SECRET = "b101d518e89da15f04f95ddd9478439b";

    private static Activity sActivity;

    ////////////////////////////////////////////////////////////////////////////////
    // Facebook
    ////////////////////////////////////////////////////////////////////////////////

    public static void shareWithFacebook(Activity activity, String imagePath) {
        sActivity = activity;

        postToFacebook(imagePath);
    }

    private static Facebook mFacebook;
    private static AsyncFacebookRunner mAsyncRunner;
    private static String mImgPath;

    private static class LoginDialogListener implements DialogListener {
        @Override
        public void onComplete(Bundle values) {
            SessionEvents.onLoginSuccess();
            Toast.makeText(sActivity, "Facebook Login Success",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFacebookError(FacebookError error) {
            SessionEvents.onLoginError(error.getMessage());
            Toast.makeText(sActivity, error.getLocalizedMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(DialogError error) {
            SessionEvents.onLoginError(error.getMessage());
            Toast.makeText(sActivity, error.getLocalizedMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
            SessionEvents.onLoginError("Action Canceled");
            Toast.makeText(sActivity, "Facebook Login Cancel",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static void initFacebook() {
        mFacebook = new Facebook(FACEBOOK_APP_ID);
        mAsyncRunner = new AsyncFacebookRunner(mFacebook);
        SessionStore.restore(mFacebook, sActivity);
        SessionListener listener = new SessionListener();
        SessionEvents.addAuthListener(listener);
        SessionEvents.addLogoutListener(listener);
    }

    private static void logout() {
        try {
            mFacebook.logout(sActivity);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void postToFacebook(String imgPath) {
        mImgPath = imgPath;
        //if (mFacebook == null)
            initFacebook();
        if (mFacebook.isSessionValid()) {
            uploadBitmap();
        } else {
            mFacebook.authorize(sActivity, 
            		new String[]{"publish_stream", "read_stream", "offline_access"},
                    new LoginDialogListener());
        }
    }

    private static void uploadBitmap() {
        Bundle params = new Bundle();
        params.putString("method", "photos.upload");
        params.putString("caption", String.format("%s %s", sActivity.getString(R.string.post_share), DOWNLOAD_URL));

        AssetManager assetManager = sActivity.getAssets();

        try {
            long len = assetManager.openFd(mImgPath).getLength();
            AssetInputStream ais = (AssetInputStream) assetManager.open(mImgPath);
            byte[] imgData = new byte[(int) len];
            ais.read(imgData);
            params.putByteArray("picture", imgData);
            ais.close();
            mAsyncRunner.request(null, params, "POST", new ImageUploadListener(), null);
        } catch (FileNotFoundException e) {
            if (DEBUG) e.printStackTrace();
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
        }

		/*File file = new File(mImgPath);
        try {
			FileInputStream fis = new FileInputStream(file);
			byte[] imgData = new byte[(int) file.length()];
			fis.read(imgData);
			params.putByteArray("picture", imgData);
			fis.close();
			mAsyncRunner.request(null, params, "POST", new EmojiUploadListener(), null);
		} catch (FileNotFoundException e) {
			if (DEBUG) e.printStackTrace();
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
		}*/
    }

    private static class ImageUploadListener extends BaseRequestListener {
        @Override
        public void onComplete(final String response, final Object state) {
            try {
                if (DEBUG) Log.d("Config:Facebook", "Response: " + response.toString());
                JSONObject json = Util.parseJson(response);
                final String src = json.getString("src");
                if (DEBUG) Log.d("Config:Facebook", "src: " + src);
                showSuccessText();
            } catch (Exception e) {
                showFailText();
            }
        }
    }

    private static void showSuccessText() {
        sActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(sActivity, "Success Photo Uploading!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showFailText() {
        sActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(sActivity, "Fail Photo Uploading!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        logout();
    }

    public static class SessionListener implements AuthListener, LogoutListener {
        @Override
        public void onAuthSucceed() {
            // Login Success!
            uploadBitmap();
        }

        @Override
        public void onAuthFail(String error) {
            // Login Fail
        }

        @Override
        public void onLogoutBegin() {
        }

        @Override
        public void onLogoutFinish() {
        }
    }

}
