package codemagnus.com.dealokav2.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import codemagnus.com.dealokav2.BaseTabActivity;
import codemagnus.com.dealokav2.R;
import codemagnus.com.dealokav2.adapter.TowerAdapter;

public class TowerListFragment extends Fragment {

    public static final String tag = "TowerListFragment";
    private TextView tvMobileNumber, tvSimSerial, tvOperatorName, tvMCC, tvMNC, tvLAC, tvRNC, tvCellId, tvPhoneType;
    private TextView tvLinkSpeed, tvWifiName;

    private ListView listView;
    private BaseTabActivity activity;

    private TextView tvLocation;
    private TextView tvTimer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view       = inflater.inflate(R.layout.fragment_home, container, false);

        tvMobileNumber  = (TextView) view.findViewById(R.id.tv_mobile_number);
        tvSimSerial     = (TextView) view.findViewById(R.id.tv_sim_serial);
        tvOperatorName  = (TextView) view.findViewById(R.id.tv_operator_name);
        tvMCC           = (TextView) view.findViewById(R.id.tv_mcc);
        tvMNC           = (TextView) view.findViewById(R.id.tv_mnc);
        tvLAC           = (TextView) view.findViewById(R.id.tv_lac);
        tvRNC           = (TextView) view.findViewById(R.id.tv_rnc);
        tvCellId        = (TextView) view.findViewById(R.id.tv_cell_id);

        tvLinkSpeed      = (TextView) view.findViewById(R.id.tv_link_speed);
        tvWifiName      = (TextView) view.findViewById(R.id.tv_wifi_name);

        tvPhoneType     = (TextView) view.findViewById(R.id.tv_phone_type);
        listView        = (ListView) view.findViewById(R.id.listView);
        tvLocation      = (TextView) view.findViewById(R.id.tv_last_location);
        tvTimer         = (TextView) view.findViewById(R.id.tv_timer);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseTabActivity) getActivity();

        if(activity.getSwitchCompat() != null)
            activity.getSwitchCompat().setVisibility(View.VISIBLE);

        getUserLastKnownLocation();

        initPhoneDetails();
        getUserLastKnownLocation();
        activity.getToolBar().setTitle("Tower List");

        if(activity.getTowerAdapter() != null) {
            setListAdapter(activity.getTowerAdapter());
        }
    }

    public void getUserLastKnownLocation() {
        Location loc = null;
        if(activity.lastKnownLocation != null) {
            loc = activity.lastKnownLocation;
        }
        if(loc != null) {
            tvLocation.setText("Lat: " + loc.getLatitude()  +
                                "\nLng: " + loc.getLongitude());
        }
    }

    public void initPhoneDetails() {
        tvMobileNumber  .setText("Mobile Number: " + activity.getTowerManager().getMobileNumber());
        tvSimSerial     .setText("Sim Serial: "     + activity.getTowerManager().getSIMSerialNumber());
        tvOperatorName  .setText("Operator Name: "  + activity.getTowerManager().getOperatorName());

        tvMCC           .setText("MCC: "    + activity.getTowerManager().getMobileCountryCode());
        tvMNC           .setText("MNC: "    + activity.getTowerManager().getMobileNetworkCode());
        tvLAC           .setText("LAC: "    + activity.getTowerManager().getCellLocation().getLac());

        //TODO
        tvRNC           .setText("RNC: ");
        tvCellId        .setText("CELLID: "     + activity.getTowerManager().getCellLocation().getCid());
        tvLinkSpeed     .setText("Link Speed: " + activity.getWifiHelper().getWifiInfo().getLinkSpeed() + " Kbps");
        tvWifiName      .setText("WIFI: "       + activity.getWifiHelper().getWifiInfo().getSSID());
        tvPhoneType     .setText("Type: "       + activity.getTowerManager().getPhoneType());
    }

    public void setTimerLabelText(String time){
        tvTimer.setText("Next request: " + time);
    }

    public void setListAdapter(TowerAdapter adapter){
        listView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(tag, "Toggle onDestroyView");
        if(activity.getSwitchCompat() != null)
            activity.getSwitchCompat().setVisibility(View.GONE);
    }
}
