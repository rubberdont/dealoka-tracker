package codemagnus.com.dealokav2.tower;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by eharoldreyes on 1/19/15.
 */
public class TowerMath {



    public static double getDistance(LatLng from, LatLng to){
        Location baseLocation = new Location("base");
        baseLocation.setLatitude(from.latitude);
        baseLocation.setLongitude(from.longitude);
        Location towerLocation = new Location("tower");
        towerLocation.setLatitude(to.latitude);
        towerLocation.setLongitude(to.longitude);
        return baseLocation.distanceTo(towerLocation);
    }

    public static LatLng getTriangulation(ArrayList<LatLngSignalPair> pairs){ //Triangulation
        double totalDbm = 0;
        double[] wDbm = new double[pairs.size()];
        double lat = 0, lng = 0;
        for (int i = 0; i < pairs.size(); i++) {
            totalDbm += pairs.get(i).getDbm();
        }
        for (int i = 0; i < pairs.size(); i++) {
            wDbm[i] = pairs.get(i).getDbm() / totalDbm;
        }
        for (int i = 0; i < pairs.size(); i++) {
            lat += pairs.get(i).getLatLng().latitude * wDbm[i];
            lng += pairs.get(i).getLatLng().longitude * wDbm[i];
        }
        return new LatLng(lat, lng);
    }

    public static LatLng getCentroid(ArrayList<LatLng> latLngs) {
        double[] centroid = { 0.0, 0.0 };
        for (int i = 0; i < latLngs.size(); i++) {
            centroid[0] += latLngs.get(i).latitude;
            centroid[1] += latLngs.get(i).longitude;
        }
        centroid[0] = centroid[0] / latLngs.size();
        centroid[1] = centroid[1] / latLngs.size();
        return new LatLng(centroid[0], centroid[1]);
    }












































//    private void requestLocationByJsonTowers(JSONArray jCellTowers, LocationRequestCallback locationRequestCallback){
//        Log.e(tag, "requestLocationByJsonTowers");
//        if (jCellTowers != null && jCellTowers.length() > 0){
//            try {
//                requestLocationByJson(jCellTowers, locationRequestCallback);
//            } catch (JSONException e) {
//                e.printStackTrace();
//                locationRequestCallback.onFailed(e);
//            }
//        }
//    }

//    public void getCurrentLocationByNearbyTowers(LocationRequestCallback locationRequestCallback){
//        try{
//            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
//            List<NeighboringCellInfo> neighboringCellInfos = getNeighboringCellInfo();
//            if(cellInfos != null && cellInfos.size() > 0){
//                CellInfo cellInfo = cellInfos.get(0);
//
//                if(cellInfo instanceof CellInfoGsm){
//                    getCurrentLocationByGSM(cellInfos, locationRequestCallback);
//                } else if (cellInfo instanceof CellInfoCdma){
//                    getCurrentLocationByCDMA(cellInfos, locationRequestCallback);
//                } else if (cellInfo instanceof CellInfoWcdma || cellInfo instanceof CellInfoLte){
//                    Log.e(tag, "getCurrentLocationByNearbyTowers CellInfoWcdma CellInfoLte");
//                    getCurrentLocationBySingleTower(locationRequestCallback);
////                    if(neighboringCellInfos != null){
////                        getCurrentLocationByNeighboringCellInfo(neighboringCellInfos, locationRequestCallback);
////                    } else {
////                        locationRequestCallback.onFailed(new Exception("WCDMA or LTE network not supported"));
////                    }
//                }
//
//            } else if (neighboringCellInfos != null && neighboringCellInfos.size() > 0) {
//                getCurrentLocationByNeighboringCellInfo(neighboringCellInfos, locationRequestCallback);
//            } else {
//                getCurrentLocationBySingleTower(locationRequestCallback);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            locationRequestCallback.onFailed(e);
//        }
//    }

//    private void getCurrentLocationByGSM(List<CellInfo> cellInfos, LocationRequestCallback locationRequestCallback) throws JSONException  {
//        Log.e(tag, "getCurrentLocationByGSM");
//        JSONArray jCellTowers = new JSONArray();
//        for (int i = 0; i < cellInfos.size(); i++) {
//            CellInfo cellInfo = cellInfos.get(i);
//            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
//            if (cellInfoGsm.getCellIdentity().getLac() != 0 && cellInfoGsm.getCellIdentity().getCid() != -1) {
//                jCellTowers.put(cellInfoGSMtoJson(cellInfoGsm));
//            }
//        }
//        requestLocationByJsonTowers(jCellTowers, locationRequestCallback);
//    }
//
//    private void getCurrentLocationByCDMA(List<CellInfo> cellInfos, LocationRequestCallback locationRequestCallback) {
//        Log.e(tag, "getCurrentLocationByCDMA");
//        ArrayList<LatLngSignalPair> latLngSignalPairs = new ArrayList<>();
//        ArrayList<Tower> towers = new ArrayList<>();
//        for (int i = 0; i < cellInfos.size(); i++) {
//            CellInfo cellInfo = cellInfos.get(i);
//            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
//            double lat = cellInfoCdma.getCellIdentity().getLatitude();
//            double lng = cellInfoCdma.getCellIdentity().getLongitude();
//            towers.add(new Tower(cellInfoCdma));
//            latLngSignalPairs.add(new LatLngSignalPair(new LatLng(lat, lng), cellInfoCdma.getCellSignalStrength().getDbm()));
//        }
//        locationRequestCallback.onSuccess(getTriangulation(latLngSignalPairs), towers);
//    }

//    @TargetApi(18)
//    private void getCurrentLocationByWCDMA(List<CellInfo> cellInfos, LocationRequestCallback locationRequestCallback)  throws JSONException {
//        Log.e(tag, "getCurrentLocationByWCDMA");
//        JSONArray jCellTowers = new JSONArray();
//        for (int i = 0; i < cellInfos.size(); i++) {
//            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
//            cellInfoWcdma.getCellSignalStrength().getLevel();
//            jCellTowers.put(cellInfoWCDMAtoJson(cellInfoWcdma));
//        }
//        requestLocationByJsonTowers(jCellTowers, locationRequestCallback);
//    }
//
//    private void getCurrentLocationByNeighboringCellInfo(List<NeighboringCellInfo> neighboringCellInfos, LocationRequestCallback locationRequestCallback) throws JSONException{
//        Log.e(tag, "getCurrentLocationByNeighboringCellInfo");
//        JSONArray jCellTowers = new JSONArray();
//        for (int i = 0; i < neighboringCellInfos.size(); i++) {
//            NeighboringCellInfo neighboringCellInfo = neighboringCellInfos.get(i);
//            jCellTowers.put(neighboringCellInfoToJSON(neighboringCellInfo));
//        }
//        requestLocationByJsonTowers(jCellTowers, locationRequestCallback);
//    }
//
//    private void getCurrentLocationBySingleTower(LocationRequestCallback locationRequestCallback) throws JSONException {
//        Log.e(tag, "getCurrentLocationBySingleTower");
//        CellLocation cellLocation = telephonyManager.getCellLocation();
//        if(cellLocation instanceof GsmCellLocation){
//            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
//            JSONArray jCellTowers = new JSONArray();
//            jCellTowers.put(gsmCellLocationToJson(gsmCellLocation));
//            requestLocationByJsonTowers(jCellTowers, locationRequestCallback);
//        }//rabbitmq //tornado //reddis
//    }
}
