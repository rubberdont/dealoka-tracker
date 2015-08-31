package codemagnus.com.dealokav2.wifi;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import codemagnus.com.dealokav2.BaseTabActivity;
import codemagnus.com.dealokav2.fragments.WifiListFragment;
import codemagnus.com.dealokav2.http.WebService;
import codemagnus.com.dealokav2.http.WebserviceRequest;
import codemagnus.com.dealokav2.utils.GeneralUtils;

/**
 * Created by codemagnus on 4/21/2015.
 */
public class WifiDataUtils {

    public static final String tag = "WifiDataUtils";

    public WifiHelper getWifiHelper() {
        return wifiHelper;
    }

    private WifiHelper wifiHelper;
    private Context context;
    private Location lastKnownLocation;

    public WifiDataUtils(Context context, WifiHelper wifiHelper, Location lastKnownLocation){
        this.context = context;
        this.wifiHelper = wifiHelper;
        this.lastKnownLocation = lastKnownLocation;
    }

    public void setLastKnownLocation(Location lastKnownLocation){
        this.lastKnownLocation = lastKnownLocation;
    }

    public JSONObject submitWifiData(List<WifiObject> results){
        JSONObject object = new JSONObject();
        JSONObject coordinates = new JSONObject();
        try {
            coordinates.put(WebService.LATITUDE,    lastKnownLocation.getLatitude());
            coordinates.put(WebService.LONGITUDE,   lastKnownLocation.getLongitude());
            object.put(WebService.COORDINATES,  coordinates);
            object.put(WebService.DEVICE_ID,    GeneralUtils.getDeviceID(context));
            object.put(WebService.WIFIS, getWifisData(results));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public JSONArray getWifisData(List<WifiObject> results) {
        JSONArray arrayWifi = new JSONArray();
        for(WifiObject scanned:results) {
            JSONObject object = getWifiObject(scanned);
            if(object != null)
                arrayWifi.put(getWifiObject(scanned));
        }
        return arrayWifi;
    }

    public JSONObject getWifiObject(WifiObject scanned){
        WifiConfiguration config = getWifiConfig(scanned);
        JSONObject objScanned = null;
        if(config != null) {
            objScanned = new JSONObject();
            try {
                objScanned.put(WebService.BSSID,            scanned.getScanResult().BSSID);
                objScanned.put(WebService.RSSI,             scanned.getScanResult().level);
                objScanned.put(WebService.SSID,             scanned.getScanResult().SSID);
                objScanned.put(WebService.FREQUENCY,        scanned.getScanResult().frequency);
                objScanned.put(WebService.CAPABILITIES,     scanned.getScanResult().capabilities);
                objScanned.put(WebService.HIDDEN_SSID,      config.hiddenSSID);
                objScanned.put(WebService.NETWORK_ID,       config.networkId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return objScanned;
    }

    public WifiConfiguration getWifiConfig(WifiObject result){
        List<WifiConfiguration> infos = wifiHelper.getWifiManager().getConfiguredNetworks();
        if(infos != null) {
            Log.wtf("WifiPreference", "No of Networks" + infos.size());
            for(WifiConfiguration config: infos) {
                Log.d(tag, "Result SSID: " + result.getScanResult().SSID + " Config SSID: " + config.SSID);
//                if(result.getScanResult().SSID.equals(config.SSID.replaceAll("\"", ""))){
                    return config;
//                }
            }
        }
        return null;
    }

    public List<WifiObject> getWifiObjects(List<ScanResult> scanResults, String lastLongMillies) {
        List<WifiObject> wifis = new ArrayList<>();
        for(ScanResult result: scanResults) {
            WifiObject wifi = new WifiObject(result);
            wifi.setStatus("-");
            wifi.setNodeChanges(lastLongMillies);
            wifis.add(wifi);
        }
        return wifis;
    }

    public WebserviceRequest.HttpURLCONNECTION sendWifiDatas(final List<WifiObject> scanResults, final WifiListFragment.WiFiSubmitCallback callback) {
        BaseTabActivity activity = (BaseTabActivity) context;
        if (scanResults.size() > 0) {
            if (lastKnownLocation != null) {
                if(activity.getSwitchCompat() != null && activity.getSwitchCompat().isChecked()) {
                        //TODO here push the data for available wifis
                        JSONObject wifisObject = submitWifiData(scanResults);
                        return wifiHelper.submitWifiHotSpots(wifisObject, new WebserviceRequest.NullCallback() {
                            @Override
                            public void onResult(int responseCode) {
                                callback.onResult(responseCode);
                            }
                        });
                }
            } else {
                if(activity.getSwitchCompat() != null && activity.getSwitchCompat().isChecked())
                    Toast.makeText(activity, "Please enable your device location provider.", Toast.LENGTH_LONG).show();
            }
        }
        return null;
    }

}
