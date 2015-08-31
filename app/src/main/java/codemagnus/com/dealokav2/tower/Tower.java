package codemagnus.com.dealokav2.tower;

import android.annotation.TargetApi;
import android.os.Build;
import android.telephony.CellInfoCdma;
import android.telephony.NeighboringCellInfo;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import codemagnus.com.dealokav2.utils.GeneralUtils;

/**
 * Created by eharoldreyes on 11/21/14.
 */
public class Tower {

    private String id;
    private String bts;
    private String operator;
    private int locationAreaCode;
    private int cellId;
    private int signalStrength;
    private int rssi;
    private int primaryScrambleCode;
    private int networkType;
    private LatLng latLng;
    private String millis;
    private boolean neighbor;
    private String time;
    private String postStatus = "-";
    private boolean posted;

    public String getPostStatus() {
        return postStatus;
    }
    private int serveTime;

    public Tower() {

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public Tower(CellInfoCdma cellInfoCdma) {
        setLocationAreaCode(cellInfoCdma.getCellIdentity().getNetworkId());
        setCellId(cellInfoCdma.getCellIdentity().getSystemId());
        setSignalStrength(cellInfoCdma.getCellSignalStrength().getLevel());
        setRssi(cellInfoCdma.getCellSignalStrength().getCdmaDbm());
        setPrimaryScrambleCode(cellInfoCdma.getCellIdentity().getSystemId());
        setNetworkType(cellInfoCdma.getCellIdentity().getBasestationId());
        setLatLng(new LatLng(cellInfoCdma.getCellIdentity().getLatitude(), cellInfoCdma.getCellIdentity().getLongitude()));
        setTime(GeneralUtils.getCurrentTime());
        setNeighbor(false);
    }

    public Tower(NeighboringCellInfo cellTowerInfo) {
        setLocationAreaCode(cellTowerInfo.getLac());
        setCellId(cellTowerInfo.getCid());
        setSignalStrength(cellTowerInfo.getRssi());
        setRssi(cellTowerInfo.getRssi());
        setPrimaryScrambleCode(cellTowerInfo.getPsc());
        setNetworkType(cellTowerInfo.getNetworkType());
        setTime(GeneralUtils.getCurrentTime());
        setNeighbor(true);
    }

    public Tower(JSONObject jsonObject) {
        try {
            setLocationAreaCode(Integer.parseInt(jsonObject.getString("lac")));
        } catch (JSONException e) {
        }
        try {
            setCellId(Integer.parseInt(jsonObject.getString("cid")));
        } catch (JSONException e) {
        }
        try {
            JSONObject location = jsonObject.getJSONObject("location");
            setLatLng(new LatLng(location.getDouble("Latitude"), location.getDouble("Longitude")));
        } catch (JSONException e) {
        }
        try {
            setRssi(Integer.parseInt(jsonObject.getString("signal")));
        } catch (JSONException e) {
        }
        try {
            setBts(jsonObject.getString("bts"));
        } catch (JSONException e) {
        }
        try {
            setId(jsonObject.getString("_id"));
        } catch (JSONException e) {
        }
        try {
            setOperator(jsonObject.getString("operator"));
        } catch (JSONException e) {
        }
        setTime(GeneralUtils.getCurrentTime());
        setNeighbor(false);
    }

    public int getLocationAreaCode() {
        return locationAreaCode;
    }

    public void setLocationAreaCode(int locationAreaCode) {
        this.locationAreaCode = locationAreaCode;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        if (signalStrength == NeighboringCellInfo.UNKNOWN_RSSI){
            this.signalStrength = signalStrength;
        } else {
            this.signalStrength = -113 + 2 * signalStrength;
        }
    }

    public int getPrimaryScrambleCode() {
        return primaryScrambleCode;
    }

    public void setPrimaryScrambleCode(int primaryScrambleCode) {
        this.primaryScrambleCode = primaryScrambleCode;
    }

    public int getNetworkType() {
        return networkType;
    }

    public void setNetworkType(int networkType) {
        this.networkType = networkType;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBts() {
        return bts;
    }

    public void setBts(String bts) {
        this.bts = bts;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getMillis() {
        return millis;
    }

    public void setMillis(String millis) {
        this.millis = millis;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
    public boolean isNeighbor() {
        return neighbor;
    }

    public void setNeighbor(boolean neighbor) {
        this.neighbor = neighbor;
    }

    public void setPostStatus(String postStatus) {
        this.postStatus = postStatus;
    }

    public int getServeTime() {
        return serveTime;
    }

    public void setServeTime(int serveTime) {
        this.serveTime = serveTime;
    }

    public boolean isPosted() {
        return posted;
    }

    public void setPosted(boolean posted) {
        this.posted = posted;
    }

    @Override
    public String toString() {
        JSONObject objTower = new JSONObject();
        try {
            objTower.put("cell_id", "" + cellId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return objTower.toString();
    }
}
