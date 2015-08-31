package codemagnus.com.dealokav2.tower;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by eharoldreyes on 12/17/14.
 */
public class LatLngSignalPair {

    private LatLng latLng;
    private int dbm;

    public LatLngSignalPair(LatLng latLng, int dbm){
        this.latLng = latLng;
        this.dbm = dbm;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public int getDbm() {
        return dbm;
    }

}
