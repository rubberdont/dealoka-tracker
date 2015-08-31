package codemagnus.com.dealokav2.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import codemagnus.com.dealokav2.BaseTabActivity;
import codemagnus.com.dealokav2.R;
import codemagnus.com.dealokav2.wifi.WifiObject;

/**
 * Created by eharoldreyes on 1/30/15.
 */
public class ScanResultAdapter extends BaseAdapter {

    private final Context context;
    private List<WifiObject> scanResults;

    public ScanResultAdapter(Context context, List<WifiObject> scanResults) {
        this.context = context;
        this.scanResults = scanResults;
    }

    static class ViewHolder {
        TextView BSSID, nodeChanges, status, signal, ssid;
    }

    public List<WifiObject> getWifis(){
        return scanResults;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        WifiObject scanResult = scanResults.get(position);
        if (convertView == null) {
            convertView      = LayoutInflater.from(context).inflate(R.layout.row_wifis, parent, false);
            holder           = new ViewHolder();
            holder.BSSID        = (TextView) convertView.findViewById(R.id.tv_bssid);
            holder.nodeChanges  = (TextView) convertView.findViewById(R.id.tv_serve_time);
            holder.status       = (TextView) convertView.findViewById(R.id.tv_status);
            holder.signal       = (TextView) convertView.findViewById(R.id.tv_signal);
            holder.ssid         = (TextView) convertView.findViewById(R.id.tv_ssid);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.BSSID           .setText("" + scanResult.getScanResult().BSSID);
        holder.status          .setText("" + scanResult.getStatus());
        holder.nodeChanges     .setText("" + (((BaseTabActivity) context).getSpinner() == null
                ? "30sec" : ((BaseTabActivity) context).getSpinner().getSelectedItem().toString()));
        holder.signal           .setText("" + WifiManager.calculateSignalLevel(scanResult.getScanResult().level, 100));
        holder.ssid             .setText("" + scanResult.getScanResult().SSID);
        return convertView;
    }

    public void setScanResults(List<WifiObject> scanResults) {
        this.scanResults = scanResults;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return scanResults.size();
    }

    @Override
    public WifiObject getItem(int position) {
        return scanResults.get(position);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public long getItemId(int position) {
        return scanResults.get(position).getScanResult().timestamp;
    }
}
