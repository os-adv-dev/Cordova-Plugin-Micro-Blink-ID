package com.os.mobile.blinkid;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.microblink.MicroblinkSDK;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkid.mrtd.MrtdRecognizer;
import com.microblink.entities.recognizers.blinkid.mrtd.MrzResult;
import com.microblink.results.date.DateResult;
import com.microblink.uisettings.ActivityRunner;
import com.microblink.uisettings.DocumentUISettings;
import com.microblink.util.Log;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by vitoroliveira on 06/11/15.
 */
public class BlinkIdPlugin extends CordovaPlugin {

    /* Request Code received on Activity Result */
    private static final int MY_BLINKID_REQUEST_CODE = 0x101;
    /* Cordova Plugin - Action to Read Cards */
    private static final String ACTION_READ_CARD_ID = "readCardId";
    private CallbackContext callbackContext;
    private MrtdRecognizer mMRTDRecognizer;
    private RecognizerBundle mRecognizerBundle;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (ACTION_READ_CARD_ID.equals(action)) {
            readCardId(callbackContext, args);
        }
        return true;
    }

    /**
     * Method to launch Card ID Reader
     *
     * @param callbackContext
     * @param args
     * @throws JSONException
     */
    private void readCardId(CallbackContext callbackContext, JSONArray args) throws JSONException {
        // check if BlinkID is supported on the device
        RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility
                .getRecognizerCompatibilityStatus(cordova.getActivity());
        if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
            callbackContext.error("BlinkID is not supported! Reason: " + supportStatus.name());
            return;
        }

        if (args != null) {
            /* Get the license key from cordova */
            JSONObject object = args.getJSONObject(0);
            String licenceKeY = object.getString("android");

            if (licenceKeY == null) {
                callbackContext.error("Is mandatory a license key to use the this plugin");
                return;
            } else {
                startScanner(licenceKeY);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == MY_BLINKID_REQUEST_CODE) {
            // make sure PhotoPay activity returned result
            if (resultCode == Activity.RESULT_OK && intent != null) {
                onScanSuccess(intent);
            } else {
                Toast.makeText(cordova.getActivity(), "Scan cancelled!", Toast.LENGTH_SHORT).show();
            }
        } else {
            // if PhotoPay activity did not return result, user has probably
            // pressed Back button and cancelled scanning
            Toast.makeText(cordova.getActivity(), "Scan cancelled!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * When the scanner is done and has results
     *
     * @param data result from retrieve to the activity
     */
    private void onScanSuccess(Intent data) {
        // update recognizer results with scanned data
        mRecognizerBundle.loadFromIntent(data);
        // you can now extract any scanned data from result
        MrzResult mrzResult = mMRTDRecognizer.getResult().getMrzResult();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("isParsed", mrzResult.isMrzParsed());
            jsonObject.put("issuer", mrzResult.getIssuer());
            jsonObject.put("documentNumber", mrzResult.getDocumentNumber());
            jsonObject.put("documentCode", mrzResult.getDocumentCode());
            jsonObject.put("dateOfExpiry", dateFormatted(mrzResult.getDateOfExpiry()));
            jsonObject.put("primaryId", mrzResult.getPrimaryId());
            jsonObject.put("secondaryId", mrzResult.getSecondaryId());
            jsonObject.put("dateOfBirth", dateFormatted(mrzResult.getDateOfBirth()));
            jsonObject.put("nationality", mrzResult.getNationality());
            jsonObject.put("sex", mrzResult.getGender());
            jsonObject.put("opt1", mrzResult.getOpt1());
            jsonObject.put("opt2", mrzResult.getOpt2());
            jsonObject.put("mrzText", mrzResult.getMrzText());

            this.callbackContext.success(jsonObject.toString());
            // break;
        } catch (JSONException e) {
            Log.e("MicroBlink", e.toString());
        }
    }

    /**
     * Start the scanner if license key is available
     *
     * @param licenseKey the key generated on
     *                   https://microblink.com/customer/generatedemolicence
     */
    private void startScanner(String licenseKey) {
        // Set up your license key
        MicroblinkSDK.setLicenseKey(licenseKey, this.cordova.getActivity());
        // we'll use Machine Readable Travel Document recognizer
        mMRTDRecognizer = new MrtdRecognizer();
        // put our recognizer in bundle so that it can be sent via intent
        mRecognizerBundle = new RecognizerBundle(mMRTDRecognizer);
        mRecognizerBundle.setNumMsBeforeTimeout(10000);
        mRecognizerBundle.setAllowMultipleScanResultsOnSingleImage(true);
        // use default UI for scanning documents
        DocumentUISettings documentUISettings = new DocumentUISettings(mRecognizerBundle);
        documentUISettings.setShowMrzDetection(true);
        // start scan activity based on UI settings
        this.cordova.setActivityResultCallback(this);
        ActivityRunner.startActivityForResult(this.cordova.getActivity(), MY_BLINKID_REQUEST_CODE, documentUISettings);
    }

    /**
     * Get the date from DateResult
     *
     * @param value the date unformatted
     * @return the date correct formatted
     */
    private String dateFormatted(DateResult value) {
        String valueFormatted = "";
        if (value != null && value.getDate() != null) {
            String[] val = value.getDate().toString().split("[.]");
            return val[2] + "-" + val[1] + "-" + val[0];
        }
        return valueFormatted;
    }
}