package codemagnus.com.dealokav2.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import codemagnus.com.dealokav2.BaseTabActivity;
import codemagnus.com.dealokav2.http.WebserviceRequest;

/**
 * Created by eharoldreyes on 1/30/15.
 */
public class WifiHelper {
    public static String tag = "WifiHelper";
    private final WifiManager wifiManager;
    private final Context context;
    private ScanResultsListener scanResultsListener;

    private WifiInfo wifiInfo;


    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                if(scanResultsListener != null) {
                    scanResultsListener.onChange(getScanResults());
                }
            }
        }
    };

    public interface ScanResultsListener{
        void onChange(List<ScanResult> scanResults);
    }

    public WifiHelper(Context context){
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.wifiManager.setWifiEnabled(true);
        this.wifiInfo = wifiManager.getConnectionInfo();
        startUpdates();
    }

    public WifiManager getWifiManager(){
        return this.wifiManager;
    }

    public List<ScanResult> getScanResults(){
        return wifiManager.getScanResults();
    }

    public void setScanResultsListener(ScanResultsListener listener){
        this.scanResultsListener = listener;
        this.wifiManager.startScan();
    }

    public void startUpdates(){
        this.context.registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void removeUpdates(){
        context.unregisterReceiver(mWifiScanReceiver);
    }
    public WifiInfo getWifiInfo() {
        return wifiInfo;
    }

    public WebserviceRequest.HttpURLCONNECTION submitWifiHotSpots(final JSONObject wifiObject,
                                                                  final WebserviceRequest.NullCallback callback) {
        Log.w(tag, "submitWifiHotSpots data: " + wifiObject.toString());
        WebserviceRequest.HttpURLCONNECTION postWifis = new WebserviceRequest.HttpURLCONNECTION();
        postWifis.setRequestMethod("POST");
        ArrayList<BasicNameValuePair> headers = new ArrayList<>();
        headers.add(new BasicNameValuePair("Content-Type", "application/json"));
        postWifis.setHeaders(headers);
        postWifis.setUrl("http://54.169.132.22:3000/wifi");
        postWifis.setParameters(wifiObject.toString());
        postWifis.setCallback(new WebserviceRequest.Callback() {
            @Override
            public void onResult(int responseCode, String responseMessage, Exception exception) {
                if(responseCode != 0 && responseMessage != null) {
                    Log.d(tag, "submitWifiHotSpots Post success: " + responseMessage);
                    ((BaseTabActivity)context).getApp().getSocketIO().emit("send-point", wifiObject);
                } else if(exception != null) {
                    exception.printStackTrace();
                } else {
                    Toast.makeText(context, "Post failed", Toast.LENGTH_SHORT).show();
                    Log.d(tag, "submitWifiHotSpots response code: " + responseCode + " message: " + responseMessage);
                }

                Log.d(tag, "Response code: " + responseMessage + " message: " + responseMessage);

                if(callback != null) {
                    callback.onResult(responseCode);
                }
            }
        });
        postWifis.execute();
        return postWifis;
    }
}
