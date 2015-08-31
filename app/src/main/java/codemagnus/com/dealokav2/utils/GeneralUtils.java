package codemagnus.com.dealokav2.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import codemagnus.com.dealokav2.http.WebService;
import codemagnus.com.dealokav2.tower.Tower;
import codemagnus.com.dealokav2.wifi.WifiObject;

/**
 * Created by Harold on 12/13/2014.
 */
public class GeneralUtils {

    public static final String TAG = "GeneralUtils";

    public static boolean checkIfTowersAreSame(Tower lastTowers, Tower towerSaved) {
        if (lastTowers.getCellId() == towerSaved.getCellId()
                && lastTowers.getLocationAreaCode() == towerSaved.getLocationAreaCode()
                && lastTowers.getPrimaryScrambleCode() == towerSaved.getPrimaryScrambleCode()
                && lastTowers.getSignalStrength() == towerSaved.getSignalStrength()
                && lastTowers.getTime().equals(towerSaved.getTime())) {
            return true;
        }
        return false;
    }

    public static String timeString(long millisUntilFinished) {
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        int minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static JSONObject submitTowerData(Context context, List<Tower> results, Location lastKnownLocation){
        JSONObject object = new JSONObject();
        JSONObject coordinates = new JSONObject();
        try {
            coordinates.put(WebService.LATITUDE,    lastKnownLocation.getLatitude());
            coordinates.put(WebService.LONGITUDE,   lastKnownLocation.getLongitude());
            object.put(WebService.COORDINATES,  coordinates);
            object.put(WebService.TOWERS,        getTowersData(results));
            object.put(WebService.DEVICE_ID,    GeneralUtils.getDeviceID(context));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public static JSONArray getTowersData(List<Tower> results) {
        JSONArray arrayWifi = new JSONArray();
        for(Tower tower:results) {
            JSONObject object = getTowerObject(tower);
            if(object != null)
                arrayWifi.put(getTowerObject(tower));
        }
        return arrayWifi;
    }

    private static  JSONObject getTowerObject(Tower tower){
        JSONObject objTower = null;
        try {
            objTower = new JSONObject();
            objTower.put(WebService.BTS,                    tower.getBts());
            objTower.put(WebService.OPERATOR,               tower.getOperator());
            objTower.put(WebService.LOCATION_AREA_CODE,       tower.getLocationAreaCode());
            objTower.put(WebService.CELL_ID,                  tower.getCellId());
            objTower.put(WebService.SIGNAL_STRENGTH,          tower.getSignalStrength());
            objTower.put(WebService.TOWER_RSSI,               tower.getRssi());
            objTower.put(WebService.PRIMARY_SCRAMBLED_CODE,   tower.getPrimaryScrambleCode());
            objTower.put(WebService.NETWORK_TYPE,           tower.getNetworkType());

            JSONObject objLatLng = new JSONObject();
            if(tower.getLatLng() != null) {
                objLatLng.put(WebService.LATITUDE, tower.getLatLng().latitude);
                objLatLng.put(WebService.LONGITUDE, tower.getLatLng().longitude);
            } else {
                objLatLng.put(WebService.LATITUDE, 0);
                objLatLng.put(WebService.LONGITUDE, 0);
            }

            objTower.put(WebService.TOWER_LATLNG,   objLatLng);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return objTower;
    }

    public static String getDeviceID(Context context){
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getOnlyDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy");
        return sdf.format(System.currentTimeMillis());
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy, kk:mm:ss");
        return sdf.format(System.currentTimeMillis());
    }

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("kk:mm:ss");
        return sdf.format(System.currentTimeMillis());
    }

    public static boolean sameLists(List<WifiObject> listFromTemp, List<WifiObject> newList){
        if (listFromTemp == null && newList == null){
            return true;
        }
        if((listFromTemp == null && newList != null)
                || listFromTemp != null && newList == null
                || listFromTemp.size() != newList.size()){
            return false;
        }
        listFromTemp = new ArrayList<>(listFromTemp);
        newList = new ArrayList<>(newList);
        if(listFromTemp.containsAll(newList))
            return true;

        if(listFromTemp.size() == newList.size())
            return true;

        return false;
    }

    public static boolean sameTowerLists(List<Tower> listFromTemp, List<Tower> newList){
        if (listFromTemp == null && newList == null){
            return true;
        }
        if((listFromTemp == null && newList != null)
                || listFromTemp != null && newList == null
                || listFromTemp.size() != newList.size()){
            return false;
        }
        listFromTemp = new ArrayList<>(listFromTemp);
        newList = new ArrayList<>(newList);
        if(listFromTemp.containsAll(newList))
            return true;

        if(listFromTemp.size() == newList.size())
            return true;

        return false;
    }


    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static void changeFont(TextView view, String path){
        Typeface face = Typeface.createFromAsset(view.getContext().getAssets(), path);
        view.setTypeface(face);
    }

    public static String md5(String s) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes(), 0, s.length());
            String hash = new BigInteger(1, digest.digest()).toString(16);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        int resultCode =  GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d("Location Updates", "Google Play services is available.");
            return true;
        } else {
            return false;
        }
    }

    public static int randInt(int min, int max) {
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public static boolean checkIfWithInRange(Calendar calendar, Tower tower) {
        Calendar firstLimit= calendar;
        Calendar secondLimit = Calendar.getInstance();
        secondLimit.setTime(firstLimit.getTime());
        secondLimit.add(Calendar.SECOND, -30);
        Calendar towerTime = null;
        try {
            towerTime = stringTimeToCalendar(tower.getTime(), TimeZone.getDefault());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        boolean lessTime = false;
        //check if time is within 30 secs.
        if (towerTime.getTime().before(firstLimit.getTime())
                && towerTime.getTime().after(secondLimit.getTime())) {
            lessTime =  true;
        } else if (firstLimit.getTime().equals(towerTime.getTime())) {
            lessTime =  true;
        }else if (secondLimit.getTime().equals(towerTime.getTime())) {
            lessTime =  true;
        }

        return lessTime;
    }

    public static Calendar stringTimeToCalendar(String strDate, TimeZone timezone) throws java.text.ParseException {
        String FORMAT_DATETIME =  "MMM dd yyyy, kk:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATETIME);
        sdf.setTimeZone(timezone);

        String dateStr = GeneralUtils.getOnlyDate() + ", " + strDate;
        Log.d(TAG, "Date str: " + dateStr);
        Date date = sdf.parse(dateStr);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }
    public static int calculateBtsSignal(int rssi){
        int MAX_RSSI = 31;
        final int MIN_RSSI = 0;
        if (rssi <= MIN_RSSI) {
            return 0;
        } else if (rssi >= MAX_RSSI) {
            return 100 - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (100 - 1);
            return (int)((float)(rssi - MIN_RSSI) * outputRange / inputRange);
        }
    }


}
