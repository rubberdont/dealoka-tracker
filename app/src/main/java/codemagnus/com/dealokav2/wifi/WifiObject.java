package codemagnus.com.dealokav2.wifi;

import android.net.wifi.ScanResult;

/**
 * Created by codemagnus on 4/21/2015.
 */
public class WifiObject {
    private String nodeChanges = "-";
    private String status = "-";
    private ScanResult scanResult;

    public WifiObject(){}

    public WifiObject(ScanResult result){
        setScanResult(result);
        setNodeChanges("");
        setStatus("");
    }
    public String getNodeChanges() {
        return nodeChanges;
    }
    public void setNodeChanges(String nodeChanges) {
        this.nodeChanges = nodeChanges;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public ScanResult getScanResult() {
        return scanResult;
    }
    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }
}
