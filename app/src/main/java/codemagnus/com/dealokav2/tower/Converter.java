package codemagnus.com.dealokav2.tower;

import android.annotation.TargetApi;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.gsm.GsmCellLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by eharoldreyes on 1/19/15.
 */
public class Converter {

    public static JSONObject towerToJson(TowerManager towerManager, Tower tower)throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lac", "" + tower.getLocationAreaCode());
        jsonObject.put("cid", "" + tower.getCellId());
        jsonObject.put("mcc", "" + towerManager.getMobileCountryCode());
        jsonObject.put("mnc", "" + towerManager.getMobileNetworkCode());
        jsonObject.put("signal", "" + tower.getRssi());
        return jsonObject;
    }
    public static JSONObject neighboringCellInfoToJSON(TowerManager towerManager, NeighboringCellInfo neighboringCellInfo) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lac", "" + neighboringCellInfo.getLac());
        jsonObject.put("cid", "" + neighboringCellInfo.getCid());
        jsonObject.put("mcc", "" + towerManager.getMobileCountryCode());
        jsonObject.put("mnc", "" + towerManager.getMobileNetworkCode());
        jsonObject.put("signal", "" + neighboringCellInfo.getRssi());
        return jsonObject;
    }
    public static JSONObject gsmCellLocationToJson(TowerManager towerManager, GsmCellLocation gsmCellLocation) throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lac", "" + gsmCellLocation.getLac());
        jsonObject.put("cid", "" + gsmCellLocation.getCid());
        jsonObject.put("mcc", "" + towerManager.getMobileCountryCode());
        jsonObject.put("mnc", "" + towerManager.getMobileNetworkCode());
        jsonObject.put("signal", "" + (towerManager.getSignalStrength() == null ? "0" : towerManager.getSignalStrength().getGsmSignalStrength()));
        return jsonObject;
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static JSONObject cellInfoGSMtoJson(CellInfoGsm cellInfoGsm) throws JSONException{
        CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
        JSONObject jInfoGsm = new JSONObject();
        jInfoGsm.put("lac", "" + cellIdentityGsm.getLac());
        jInfoGsm.put("cid", "" + cellIdentityGsm.getCid());
        jInfoGsm.put("mcc", "" + cellIdentityGsm.getMcc());
        jInfoGsm.put("mnc", "" + cellIdentityGsm.getMnc());
        jInfoGsm.put("signal", "" + (cellInfoGsm.getCellSignalStrength().getLevel()));//.getDbm() / -30) - 50));
        return jInfoGsm;
    }
    @TargetApi(18)
    public static JSONObject cellInfoWCDMAtoJson(TowerManager towerManager, CellInfoWcdma cellInfoWcdma) throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lac", "" + cellInfoWcdma.getCellIdentity().getCid());
        jsonObject.put("cid", "" + cellInfoWcdma.getCellIdentity().getLac());
        jsonObject.put("mcc", "" + towerManager.getMobileCountryCode());
        jsonObject.put("mnc", "" + towerManager.getMobileNetworkCode());
        jsonObject.put("signal", "" + cellInfoWcdma.getCellSignalStrength().getLevel());
        return jsonObject;
    }

    public static ArrayList<Tower> neighboringCellInfosToTowers(List<NeighboringCellInfo> neighboringCellInfos) {
        ArrayList<Tower> towers = new ArrayList<>();
        for (int i = 0; i < neighboringCellInfos.size(); i++) {
            NeighboringCellInfo neighboringCellInfo = neighboringCellInfos.get(i);
            if(!towers.toString().contains("" + neighboringCellInfo.getCid())) {
                if(neighboringCellInfo.getLac() != 0){
                    towers.add(new Tower(neighboringCellInfo));
                }
            }
        }
        Collections.sort(towers, new TowerManager.TowerComparator());
        return towers;
    }

    public static  ArrayList<Tower> cellInfosToTowers(TowerManager towerManager, List<CellInfo> cellInfos) throws JSONException{
        ArrayList<Tower> towers = new ArrayList<>();
        CellInfo cellInfo = cellInfos.get(0);
        if(cellInfo instanceof CellInfoGsm){
            towers = cellInfosGSMtoTowers(cellInfos);
        } else if (cellInfo instanceof CellInfoCdma){
            towers = cellInfoCdmaToTowers(cellInfos);
        } else if (cellInfo instanceof CellInfoWcdma || cellInfo instanceof CellInfoLte){
            Tower primaryTower = towerManager.getPrimaryTower();
            if(primaryTower != null) towers.add(primaryTower);
        }
        return towers;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static  ArrayList<Tower> cellInfosGSMtoTowers(List<CellInfo> cellInfos) throws JSONException{
        ArrayList<Tower> towers = new ArrayList<>();
        for (int i = 0; i < cellInfos.size(); i++) {
            CellInfo cellInfo = cellInfos.get(i);
            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
            if (cellInfoGsm.getCellIdentity().getLac() != 0 && cellInfoGsm.getCellIdentity().getCid() != -1) {
                towers.add(new Tower(Converter.cellInfoGSMtoJson(cellInfoGsm)));
            }
        }
        return towers;
    }

    public static  ArrayList<Tower> cellInfoCdmaToTowers(List<CellInfo> cellInfos) {
        ArrayList<Tower> towers = new ArrayList<>();
        for (int i = 0; i < cellInfos.size(); i++) {
            CellInfo cellInfo = cellInfos.get(i);
            towers.add(new Tower((CellInfoCdma) cellInfo));
        }
        return towers;
    }

}
