/**
 * @author Ry
 * @Date 2013.04.26
 * @Filename InAppBilling.java
 */

package com.marcomobil.everybody;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;

public class InAppBilling {

    private static final String TAG = InAppBilling.class.getSimpleName();

    private static final String IN_APP_BILLING_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA68LbkNfVwwJtfQfnwra4cBK/P4n0yRycu+fVLvWpU2377qSDzJYz0DwRToBSq5szjhb6L5O5F2TVe4XOLgM5PTRVpIRYhHN7iNVFMMNqnlYFNt0joHD2Ayubm18d6HboVhGCmyHSaYcOtcoCR/53ayB9Tah3ZMKYyL8oRPLEMfC3FRnqscz0ZZzpJNhatOJJpW+xrksvKGP72T6juPli35PL7OBVjmyYyE9g0c7lfFGujxvQNvdW/AzOlKRx6UFw8A1QtvplQbwCVvBATXXInRMPFk8y5zpcol9xkuHxCplBOK/fStyAi0at3+WgSP22l5mOQGNqlvojc6RGOYtaOwIDAQAB";

    // In app billing item definition
    public static final String SKU_BECOME_PROMEMBER = "com.everybody.promember";

    // (arbitrary) request code for the purchase flow
    public static final int RC_REQUEST = 10001;

    // The helper object
    private static IabHelper mHelper;

    private static Context mContext;
    
    private static boolean mIsBecomePromember;
    

    public static void initConfiguration(Context context) {
        mContext = context;

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        String base64EncodedPublicKey = IN_APP_BILLING_KEY;

        // Create the helper, passing it our context and the public key to verify signatures with
        if (Config.DEBUG) Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(mContext, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(Config.DEBUG);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        if (Config.DEBUG) Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (Config.DEBUG) Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                if (Config.DEBUG) Log.d(TAG, "Setup successful. Querying inventory.");

                List<String> additionalSkuList = new ArrayList<String>();
                additionalSkuList.add(SKU_BECOME_PROMEMBER);
                
                mHelper.queryInventoryAsync(true, additionalSkuList, mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    static IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (Config.DEBUG) Log.d(TAG, "Query inventory finished.");
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            if (Config.DEBUG) Log.d(TAG, "Query inventory was successful.");
            
            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the wave tone upgrade?
            Purchase mBecomePromemberPurchase = inventory.getPurchase(SKU_BECOME_PROMEMBER);
            mIsBecomePromember = (mBecomePromemberPurchase != null && verifyDeveloperPayload(mBecomePromemberPurchase));
           	//mMultiWaveTonePrice = inventory.getSkuDetails(SKU_MULTI_WAVE_TONE).getPrice();
            
           	Log.d(TAG, "User is " + (mIsBecomePromember ? "PURCHASED" : "NOT PURCHASED"));
           	((MainActivity) mContext).changeStateOnPurchaseButton(mIsBecomePromember);
           	
            if (Config.DEBUG) Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    /**
     * Verifies the developer payload of a purchase.
     */
    static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        
        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         * 
         * WARNING: Locally generating a random string when starting a purchase and 
         * verifying it here might seem like a good approach, but this will fail in the 
         * case where the user purchases an item on one device and then uses your app on 
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         * 
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         * 
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on 
         *    one device work on other devices owned by the user).
         * 
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        if (!TextUtils.isEmpty(payload) && payload.equalsIgnoreCase(getUDID()))
            return true;
        else
            return false;
    }

    // Callback for when a purchase is finished
    static IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            if (Config.DEBUG) Log.d(TAG, "Purchase successful.");

//            mOnCompleteListener.onPurchaseComplete(purchase.getSku());

            if (purchase.getSku().equals(SKU_BECOME_PROMEMBER)) {
                // bought the pro-member!
                Log.d(TAG, "Purchase lets you become to pro-member . Congratulating user.");
                alert("Thank you for becoming to pro-member!");
                
                ((MainActivity) mContext).changeStateOnPurchaseButton(true);
                mIsBecomePromember = true;
                //saveData();
            }
        }
    };

    // We're being destroyed. It's important to dispose of the helper here!
    public static void release() {
        // very important:
        if (Config.DEBUG) Log.d(TAG, "Destroying helper.");
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }
    
    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
    */

    public static boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    public static void purchaseBecomePromember() {
    	/* TODO: for security, generate your payload here for verification. See the comments on 
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use 
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = getUDID();

        mHelper.launchPurchaseFlow((Activity) mContext, SKU_BECOME_PROMEMBER, RC_REQUEST,
                mPurchaseFinishedListener, payload);
    }

    static void complain(String message) {
        if (Config.DEBUG) {
            Log.e(TAG, "**** ProTips4U Error: " + message);
            alert("Error: " + message);
        }
    }

    static void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(mContext);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    static String getUDID() {
        return Settings.System.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
