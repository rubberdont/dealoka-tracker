package codemagnus.com.dealokav2.wifi;

import android.net.wifi.ScanResult;

/**
 * Created by codemagnus on 3/4/2015.
 */
public class WifiData {
    private String ssis;
    private String bssis;
//    private String frequency;

    public WifiData() {
    }
    public String getSsis() {
        return ssis;
    }

    public void setSsis(String ssis) {
        this.ssis = ssis;
    }

    public String getBssis() {
        return bssis;
    }

    public void setBssis(String bssis) {
        this.bssis = bssis;
    }

//    public String getFrequency() {
//        return frequency;
//    }
//
//    public void setFrequency(String frequency) {
//        this.frequency = frequency;
//    }

    public WifiData(ScanResult result) {
        setSsis(result.SSID);
        setBssis(result.BSSID);
//        setFrequency("" + result.frequency);
    }



}
