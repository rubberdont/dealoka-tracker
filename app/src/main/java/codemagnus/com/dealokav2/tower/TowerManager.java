package codemagnus.com.dealokav2.tower;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import codemagnus.com.dealokav2.BaseTabActivity;
import codemagnus.com.dealokav2.http.WebService;
import codemagnus.com.dealokav2.http.WebserviceRequest;

/**
 * Created by eharoldreyes on 11/21/14.
 */
public class TowerManager {

    public static final String tag = "TowerManager";
    private static final String ENDPOINT = "http://54.169.132.22:3000";
    private final TelephonyManager telephonyManager;
    private TowersChangeCallback towersChangeCallback;
    private PrimaryTowersChangeCallback primaryTowersChangeCallback;
    private SignalStrength signalStrength;
    private Context context;

    private static TowerManager instance;

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            TowerManager.this.signalStrength = signalStrength;

            if(towersChangeCallback != null) {
                try {
                    towersChangeCallback.didTowersChanged(getTowers(), null);
                } catch (JSONException e) {
                    towersChangeCallback.didTowersChanged(null, e);
                }
                return;
            }

            if(primaryTowersChangeCallback != null) {
                try {
                    List<Tower> availableTowers = getPrimaryTowers();
                    ArrayList<Tower> towersPrimary = new ArrayList<>();
                    ArrayList<Tower> towersNeighbor = new ArrayList<>();

                    for(Tower tower: availableTowers) {
                        if(tower.isNeighbor()) {
                            towersNeighbor.add(tower);
                        } else {
                            towersPrimary.add(tower);
                        }
                    }

                    primaryTowersChangeCallback.didTowersChanged(towersPrimary, towersNeighbor, null);
//                    primaryTowersChangeCallback.didTowersChanged(getTowers(), null);
                } catch (JSONException e) {
                    primaryTowersChangeCallback.didTowersChanged(null, null, e);
                }
            }
        }
    };

    public static TowerManager getInstance(Context context){
        if(instance == null){
            synchronized (TowerManager.class){
                if(instance == null)
                    instance = new TowerManager(context);
            }
        }

        return instance;
    }

    public TowerManager(Context context){
        this.context = context;
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void setOnTowersChangedListener(TowersChangeCallback towersChangeCallback) {
        this.towersChangeCallback = towersChangeCallback;
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public void setOnPrimaryTowerChangedListener(PrimaryTowersChangeCallback towersChangeCallback){
        this.primaryTowersChangeCallback = towersChangeCallback;
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public void stopTowerUpdate(){
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    public ArrayList<Tower> getTowers() throws JSONException {
        ArrayList<Tower> towers                             = new ArrayList<>();

        List<NeighboringCellInfo> neighboringCellInfos      = getNeighboringCellInfo();
        List<CellInfo> cellInfos                            = getAllCellInfo();

        Log.i(tag, "getTowers phoneType: " + getPhoneType());
        Log.i(tag, "getTowers using: NeighboringCellInfos size: " + neighboringCellInfos.size());
        Log.i(tag, "getTowers using: CellInfos size: " + (cellInfos == null ? "null":cellInfos.size()));

        Log.i(tag, "getTowers using: PrimaryTower");
        Tower primaryTower = getPrimaryTower();
        if(primaryTower != null) towers.add(primaryTower);
        if(cellInfos != null && cellInfos.size() > 0) {
            towers.addAll(Converter.cellInfosToTowers(this, cellInfos));
        }
        if (neighboringCellInfos != null && neighboringCellInfos.size() > 0) {
            towers.addAll(Converter.neighboringCellInfosToTowers(neighboringCellInfos));
        }
        Log.i(tag, "getTowers using: PrimaryTower size in here: " + towers.size());
        return towers;
    }

    public ArrayList<Tower> getPrimaryTowers() throws JSONException {
        ArrayList<Tower> towers = new ArrayList<>();
        Log.i(tag, "getTowers using: PrimaryTower");
        Tower primaryTower = getPrimaryTower();
        if (primaryTower != null) towers.add(primaryTower);

        List<NeighboringCellInfo> neighboringCellInfos      = getNeighboringCellInfo();
        if (neighboringCellInfos != null && neighboringCellInfos.size() > 0) {
            Log.i(tag, "getTowers using: NeighboringCellInfos size: " + neighboringCellInfos.size());
            for(Tower info: Converter.neighboringCellInfosToTowers(neighboringCellInfos))
            if(!towers.toString().contains("" + info.getCellId())) {
                towers.add(info);
            }
        }
        return towers;
    }

    public Tower getPrimaryTower() throws JSONException{
        CellLocation cellLocation = telephonyManager.getCellLocation();
        Log.d(tag, "Cell location instance:");
        if(cellLocation instanceof GsmCellLocation){
            Log.d(tag, "Cell location instance: GsmCellLocation");
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            return new Tower(Converter.gsmCellLocationToJson(this, gsmCellLocation));
        }else if(cellLocation instanceof CellLocation){
            Log.d(tag, "Cell location instance: CellLocation");
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            return new Tower(Converter.gsmCellLocationToJson(instance, gsmCellLocation));
        }  else {
            Log.d(tag, "Cell location instance: null");
            return null;
        }
    }

    public void getCurrentLocationByTowers(ArrayList<Tower> towers, LocationRequestCallback locationRequestCallback){
        JSONArray jCellTowers = new JSONArray();
        try {
            Log.d(tag, "Tower size in here getCurrentLocationByTowers: " + towers.size());
            for (int i = 0; i < towers.size(); i++) {
                JSONObject objTower = Converter.towerToJson(this, towers.get(i));
                try {
                    if(!jCellTowers.toString().contains(objTower.getString("cid"))) {
                        jCellTowers.put(objTower);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            requestLocationByJson(jCellTowers, locationRequestCallback);
        } catch (JSONException e) {
            e.printStackTrace();
            locationRequestCallback.onFailed(e);
        }
    }

    public void requestLocationByJson(JSONArray jCellTowers, LocationRequestCallback locationRequestCallback) throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("towers", jCellTowers);
        String params = jsonObject.toString();
        Log.d(tag, "requestLocationByJson jRequestBody: " + params);
        GeoLocate(params, locationRequestCallback);
    }

    public List<NeighboringCellInfo> getNeighboringCellInfo(){
        return telephonyManager.getNeighboringCellInfo();
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public List<CellInfo> getAllCellInfo(){
        return telephonyManager.getAllCellInfo();
    }
    public String getSIMSerialNumber() {
        try{
            return telephonyManager.getSimSerialNumber();
        }catch (Exception e) {
            return "Not available";
        }
    }
    public String getSIMIMSI()  {
        return telephonyManager.getSubscriberId();
    }
    public String getMobileNumber() {
        try{
            return telephonyManager.getLine1Number();
        } catch (Exception e){
            return "Not available";
        }
    }

    public String getOperatorName() {
        try{
            return telephonyManager.getNetworkOperator().substring(0, 3);
        } catch (Exception e){
            return "Not available";
        }
    }

    public String getMobileCountryCode(){
        try{
            return telephonyManager.getNetworkOperator().substring(0, 3);
        } catch (Exception e){
            try {
                return telephonyManager.getSimOperator().substring(0, 3);
            } catch (Exception e1) {
                return "Not available";
            }
        }
    }
    public String getMobileNetworkCode(){
        try{
            return telephonyManager.getNetworkOperator().substring(3);
        } catch (Exception e){
            try {
                return telephonyManager.getSimOperator().substring(3);
            } catch (Exception e1) {
                return "Not available";
            }
        }
    }
    public String getPhoneType(){
        switch (telephonyManager.getPhoneType()){
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "cdma";
            case TelephonyManager.PHONE_TYPE_GSM:
                return "gsm";
            default:
                return "unknown";
        }
    }
    public GsmCellLocation getCellLocation(){
        try {
            return (GsmCellLocation) telephonyManager.getCellLocation();
        } catch (Exception e) {
            return null;
        }
    }
    public SignalStrength getSignalStrength() {
        try {
            return signalStrength;
        } catch (Exception e) {
            return null;
        }
    }

    public static void GeoLocate(String params, final LocationRequestCallback locationRequestCallback){
        WebserviceRequest.HttpURLCONNECTION urlconnection = new WebserviceRequest.HttpURLCONNECTION();
        urlconnection.setUrl(ENDPOINT + "/carrier/triangulate");
        urlconnection.setRequestMethod(WebService.METHOD_POST);
        ArrayList<BasicNameValuePair> authHeader = new ArrayList<>();
        authHeader.add(new BasicNameValuePair("Content-Type", "application/json"));
        urlconnection.setHeaders(authHeader);
        urlconnection.setParameters(params);
        urlconnection.setCallback(new GeoLocateCallback(locationRequestCallback));
        urlconnection.execute();
    }

    public static WebserviceRequest.HttpGET GetTowerLocation(JSONObject jTower, WebserviceRequest.Callback callback) throws JSONException{
        WebserviceRequest.HttpGET httpGET = new WebserviceRequest.HttpGET();
        httpGET.setUrl(ENDPOINT + "/station"
                + "?lac=" + jTower.getString("lac")
                + "&mnc=" + jTower.getString("mnc")
                + "&cid=" + jTower.getString("cid"));
        httpGET.setCallback(callback);
        httpGET.execute();
        return  httpGET;
    }

    public static interface LocationRequestCallback{
        public void onSuccess(LatLng latLng, List<Tower> towers);
        public void onFailed(Exception e);
    }

    public static interface TowersChangeCallback {
        public abstract void didTowersChanged(ArrayList<Tower> towers, Exception e);
    }

    public static interface PrimaryTowersChangeCallback {
        public abstract void didTowersChanged(ArrayList<Tower> towersPrimary, ArrayList<Tower> neighbor, Exception e);
    }

    public static  class TowerComparator implements Comparator<Tower> {
        @Override
        public int compare(Tower o1, Tower o2) {
            return ((Integer) o2.getSignalStrength()).compareTo(o1.getSignalStrength());
        }
    }

    private static class GeoLocateCallback implements WebserviceRequest.Callback{
        private final LocationRequestCallback locationRequestCallback;
        public GeoLocateCallback(LocationRequestCallback locationRequestCallback){
            this.locationRequestCallback = locationRequestCallback;
        }

        @Override
        public void onResult(int i, String s, Exception e) {
            Log.d(tag, "geolocate urlconnection: " + i + " " + s);
            if (i == 200 && s != null) {
                try {
                    JSONObject jResult = new JSONObject(s);
                    JSONObject jUser = jResult.getJSONObject("user");
                    LatLng latLng = new LatLng(jUser.getDouble("x"), jUser.getDouble("y"));
                    JSONArray jTowers = jResult.getJSONArray("towers");
                    List<Tower> towers = new ArrayList<>();
                    for (int j = 0; j < jTowers.length(); j++) {
                        towers.add(new Tower(jTowers.getJSONObject(j)));
                    }
                    locationRequestCallback.onSuccess(latLng, towers);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                    locationRequestCallback.onFailed(e1);
                }
            } else if (e != null) {
                locationRequestCallback.onFailed(e);
            }
        }
    }

    public WebserviceRequest.HttpURLCONNECTION
                    submitTowerCellsData(final JSONObject towerObject,
                                         final boolean showToast, final WebserviceRequest.NullCallback callbacks) {
        Log.w(tag, "submitTowerCellsData data: " + towerObject.toString());
        final WebserviceRequest.HttpURLCONNECTION postWifis = new WebserviceRequest.HttpURLCONNECTION();
        postWifis.setRequestMethod("POST");
        ArrayList<BasicNameValuePair> headers = new ArrayList<>();
        headers.add(new BasicNameValuePair("Content-Type", "application/json"));
        postWifis.setHeaders(headers);
        postWifis.setUrl("http://54.169.132.22:3000/bts");
        postWifis.setParameters(towerObject.toString());
        postWifis.setCallback(new WebserviceRequest.Callback() {
            @Override
            public void onResult(int responseCode, String responseMessage, Exception exception) {
                Log.w(tag, "submitTowerCellsData response: " + responseCode + " msg: " + responseMessage);
                if(responseCode != 0 && responseMessage != null) {
                    if(showToast)
                        Toast.makeText(context, "Post success", Toast.LENGTH_SHORT).show();
                    try {
                        towerObject.put("session_id",((BaseTabActivity)context).getRandomSession());
                        Log.d(tag, "Random session putted: " + towerObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ((BaseTabActivity)context).getApp().getSocketIO().emit("send-point", towerObject);
                } else if(exception != null) {
                    exception.printStackTrace();
                } else {
                    Toast.makeText(context, "Post failed", Toast.LENGTH_SHORT).show();
                    Log.w(tag, "submitTowerCellsData response code: " + responseCode + " message: " + responseMessage);
                }

                if(callbacks != null) {
                    callbacks.onResult(responseCode);
                }
            }
        });
        postWifis.execute();
        return postWifis;
    }
}
