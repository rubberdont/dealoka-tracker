package codemagnus.com.dealokav2.fragments;


import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import codemagnus.com.dealokav2.BaseTabActivity;
import codemagnus.com.dealokav2.R;
import codemagnus.com.dealokav2.adapter.ScanResultAdapter;

public class WifiListFragment extends Fragment {

    public static final String tag = "WifiListFragment";

    private ListView listView;
    private BaseTabActivity activity;

    private Location lastKnownLocation;
    private TextView tvWifiName, tvLocation, tvTimer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_list, container, false);
        listView    = (ListView) rootView.findViewById(R.id.listView);
        tvWifiName  = (TextView) rootView.findViewById(R.id.tv_wifi_name);
        tvLocation  = (TextView) rootView.findViewById(R.id.tv_last_location);
        tvTimer     = (TextView) rootView.findViewById(R.id.tv_timer);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        activity = (BaseTabActivity) getActivity();

        if(activity.getSwitchCompat() != null)
            activity.getSwitchCompat().setVisibility(View.VISIBLE);

        getUserLastKnownLocation();
        setWifiName();
        activity.getToolBar().setTitle("Access Point List");
    }

    public void setTimerText(String strMessage){
        tvTimer.setText("Next request: " + strMessage);
    }

    public void setWifiName() {
        tvWifiName.setText("WIFI: " + activity.getWifiUtility().getWifiHelper().getWifiInfo().getSSID());
    }

    public void getUserLastKnownLocation() {
        Location loc = null;
        if(activity.lastKnownLocation != null) {
            loc = activity.lastKnownLocation;
        } else if(lastKnownLocation != null){
            loc = lastKnownLocation;
        }
        if(loc != null) {
            lastKnownLocation = loc;
            tvLocation.setText("Lat: " + lastKnownLocation.getLatitude()  +
                    "\nLng: " + lastKnownLocation.getLongitude());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(activity.getScannedWifiAdapter() != null)
            listView.setAdapter(activity.getScannedWifiAdapter());
    }

    public void refreshListAdapter(ScanResultAdapter adapter){
        listView.setAdapter(adapter);
    }

    public interface WiFiSubmitCallback{
        void onResult(int responseCode);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(activity.getSwitchCompat() != null)
            activity.getSwitchCompat().setVisibility(View.GONE);
    }
}

