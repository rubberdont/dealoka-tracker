package codemagnus.com.dealokav2.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.telephony.CellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import codemagnus.com.dealokav2.BaseTabActivity;
import codemagnus.com.dealokav2.R;
import codemagnus.com.dealokav2.http.WebserviceRequest;
import codemagnus.com.dealokav2.tower.Tower;
import codemagnus.com.dealokav2.tower.TowerManager;
import codemagnus.com.dealokav2.tower.TowerMath;
import codemagnus.com.dealokav2.utils.GeneralUtils;
import codemagnus.com.dealokav2.utils.LocationUtils;

/**
 * Created by eharoldreyes on 12/22/14.
 */
public class VisualizationFragment  extends Fragment implements CustomMapFragment.OnMapReadyListener{

    public static final String tag = "VisualizationFragment";
    private int towerRequestDelay;
    private CustomMapFragment mapFragment;
    private BaseTabActivity activity;
    private GoogleMap googleMap;
    private TowerManager towerManager;
    private HashMap<String, Tower> cellTowerMarkers;
    private ArrayList<Marker> markers;
    private Dialog dialog;
    private TextView dialogCID, dialogLAC, dialogBTS, dialogID, dialogLAT, dialogLNG;
    private String[] delayTitles = {"30sec","1min","2mins","3mins","4mins","5mins"};
    private int[] delayValues = {30000 , 60000, 120000, 180000, 240000, 300000};

    private Spinner spinner;

    private SharedPreferences sharedpreferences;
    private boolean isMapBoundsSet;

    private LocationUtils locationUtils;
    private Location lastKnownLocation;

    private Marker gpsMarker;

    private TextView tvCurrentTower;
    public ArrayList<Tower> savedTowers;
    private TextView textClock;
    private CountDownTimer countDownTimer;
    private View rootView;
    private Toolbar toolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        }
        try {
            rootView = inflater.inflate(R.layout.fragment_visualization, container, false);
        } catch (InflateException e) {

        }

        tvCurrentTower = (TextView) rootView.findViewById(R.id.tv_current_tower);
        textClock = (TextView) rootView.findViewById(R.id.textClock);
        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapsInitializer.initialize(getActivity());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        activity    = (BaseTabActivity) getActivity();
        towerManager = TowerManager.getInstance(activity);
        sharedpreferences = activity.getSharedPreferences("Delay-Map", Context.MODE_PRIVATE);

        setTowerRequestDelay(delayValues[sharedpreferences.getInt("spinner:position", 0)]);
        locationUtils = new LocationUtils(activity, 1000, 50);

        locationUtils.getLocationUpdates(LocationUtils.GPS_PROVIDER, locationListener);
        locationUtils.getLocationUpdates(LocationUtils.NETWORK_PROVIDER, locationListener);

        toolbar.setTitle("Triangulation");

        getMapFragment();
    }

    private void getMapFragment() {
        if(mapFragment != null) {
            googleMap = mapFragment.getMap();
            if(googleMap != null){
                googleMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        killOldMap();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(countDownTimer != null)countDownTimer.cancel();

        if(urlConnection != null && urlConnection.getStatus() == AsyncTask.Status.RUNNING)
            urlConnection.cancel(true);

        if(locationUtils != null ) {
            locationUtils.stopLocationUpdates();
        }
        if(activity.getToolBar() != null)
            activity.getToolBar().setVisibility(View.VISIBLE);
        if(activity.getSwitchCompat() != null)
            activity.getSwitchCompat().setVisibility(View.VISIBLE);
    }

    private void killOldMap() {
        if(mapFragment != null) {
            FragmentManager fM = getChildFragmentManager();
            try {
                fM.beginTransaction().remove(mapFragment).commit();
            } catch (Exception e) {
                Log.d(tag, "exc. msg: " + e.getMessage());
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapFragment = new CustomMapFragment();
        addFragment(R.id.mapContainer, CustomMapFragment.tag, mapFragment, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        isMapBoundsSet = false;
        if(activity.getToolBar() != null)
            activity.getToolBar().setVisibility(View.GONE);

        spinner     = (Spinner) toolbar.findViewById(R.id.spinner_nav);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, delayTitles));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setTowerRequestDelay(delayValues[position]);
                sharedpreferences.edit().putInt("spinner:position", position).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinner.setSelection(sharedpreferences.getInt("spinner:position", 0), true);
    }

    public void addFragment(int id, String tag, Fragment fragment, boolean addToBackStack){
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.add(id, fragment, tag);
        if(addToBackStack) fragmentTransaction.addToBackStack(tag);
        fragmentTransaction.commit();
    }

    @Override
    public void onMapReady() {
        if(GeneralUtils.isGooglePlayServicesAvailable(activity)) {
            getMapFragment();
        }
    }

    private void requestLocationByTowers(){
        displayCurrentTower();
        towerManager.setOnTowersChangedListener(new TowerManager.TowersChangeCallback() {
            @Override
            public void didTowersChanged(ArrayList<Tower> towers, Exception e) {
                displayCurrentTower();
                // Dynamic
                if(towers != null) {
                    saveTowers(towers);
                    towerManager.getCurrentLocationByTowers(towers, locationRequestCallback);
                }

                // Testing
//                try {
//                    testGeoLocate();
//                } catch (JSONException e1) {
//                    e1.printStackTrace();
//                }
            }
        });
    }

    public void displayCurrentTower() {
        if(activity.getTowerManager() != null) {
            String mnc = "MNC: " + towerManager.getMobileNetworkCode();
            String mcc = "MCC: " + towerManager.getMobileCountryCode();
            String cid = "CID: " + towerManager.getCellLocation().getCid();
            String lac = "LAC: " + towerManager.getCellLocation().getLac();
            String signal = "SIGNAL: " + towerManager.getSignalStrength();
            tvCurrentTower.setText("Current tower: \n" + mnc + "\n" + mcc + "\n" +cid + "\n" + lac + "\n" + signal);
        }
    }

    private void setTowerRequestDelay(int value) {
        this.towerRequestDelay = value;
        startNewCountDown();
    }

    private void startNewCountDown(){
        if(countDownTimer != null)
            countDownTimer.cancel();

        countDownTimer = new CountDownTimer(towerRequestDelay, 1000) {
            public void onTick(long millisUntilFinished) {
                Log.d(tag, "seconds remaining: " + millisUntilFinished / 1000);
                displayCountDown(millisUntilFinished);
            }

            public void onFinish() {
                Log.d(tag, "expired");
                startNewCountDown();
                if(savedTowers != null){
                    towerManager.getCurrentLocationByTowers(savedTowers, locationRequestCallback);
                }
            }
        };
        countDownTimer.start();
    }

    private void displayCountDown(long millisUntilFinished) {
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        int minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
        String ms = String.format("%02d:%02d", minutes, seconds);
        textClock.setText(ms);
    }

    public void saveTowers(ArrayList<Tower> towers) {
        Log.d(tag, "Added " + towers.size() + " new Towers");

        if (savedTowers == null) savedTowers = new ArrayList<>();
        for(Tower tower: towers){
            savedTowers.add(tower);
        }
        if(!GeneralUtils.sameTowerLists(activity.getTowers(), savedTowers)) {
//            activity.setTowers(savedTowers);
            if (savedTowers.size() > 0) {
                if (lastKnownLocation != null) {
                    if(activity.getSwitchCompat() != null && activity.getSwitchCompat().isChecked()) {
                        //TODO here push the data for available towers
                        if(urlConnection == null) {
                            JSONObject objTowers = GeneralUtils.submitTowerData(activity, savedTowers, lastKnownLocation);
                            urlConnection =  towerManager.submitTowerCellsData(objTowers, true, new WebserviceRequest.NullCallback() {
                                @Override
                                public void onResult(int responseCode) {
                                    urlConnection = null;
                                }
                            });
                        }
                    }
                } else {
                    if(activity.getSwitchCompat() != null &&  activity.getSwitchCompat().isChecked())
                        Toast.makeText(activity, "Please enable your device location provider.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private WebserviceRequest.HttpURLCONNECTION urlConnection;

    private Marker addMarker(Tower tower) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(tower.getLatLng());
        markerOptions.title("CID: " + tower.getCellId());
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_marker));
        Marker marker = googleMap.addMarker(markerOptions);
        markers.add(marker);
        cellTowerMarkers.put(marker.getId(), tower);
        return marker;
    }

    private Marker addMyMarker(LatLng latLng){
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Lat: " + latLng.latitude + " \nLng: " + latLng.longitude);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location));
        Marker marker = googleMap.addMarker(markerOptions);
        if (markers == null) markers = new ArrayList<>();
        markers.add(marker);
        return marker;
    }

    private void setGPSMarker(LatLng latLng){
        Log.d(tag, "setGPSMarker");

        if(gpsMarker != null && markers != null) {
            markers.remove(gpsMarker);
            gpsMarker.remove();
        }

        if (gpsMarker != null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("GPS Location");
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location));

            gpsMarker = googleMap.addMarker(markerOptions);
            markers.add(gpsMarker);
        }
    }

    private void setMarkerListener(){
        if(googleMap != null) {
            googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    String markerID = marker.getId();
                    Log.d(tag, "onMarkerClick " + markerID);
                    if(cellTowerMarkers.containsKey(markerID)){
                        Tower tower = cellTowerMarkers.get(markerID);
                        Log.d(tag, "onMarkerClick tower " + tower.getCellId());
                        showChargingSiteDialog(tower);
                    } else {
                        Log.e(tag, "onMarkerClick marker not found");
                        if(!marker.getTitle().equals("GPS Location")){
                            marker.showInfoWindow();
                        }
                    }
                    return false;
                }
            });
        }
    }

    private void setMapBounds() throws Exception {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        int padding = 45; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        googleMap.moveCamera(cu);
        googleMap.animateCamera(cu);
        isMapBoundsSet = true;
    }

    private void zoomCamera(LatLng latLng, float zoom){
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        googleMap.animateCamera(yourLocation);
    }

    private void drawCircle(LatLng latLng, double radius) {
        googleMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(radius)
                .strokeWidth(2)
                .strokeColor(R.color.transparent_blue)
                .fillColor(R.color.transparent_white));
    }

    private void showChargingSiteDialog(final Tower tower) {
        if(dialog == null){
            dialog = new Dialog(activity);
            dialog.setTitle(tower.getOperator());
            dialog.setContentView(R.layout.dialog_tower_details);
            dialogID = (TextView) dialog.findViewById(R.id.tv_id);
            dialogBTS = (TextView) dialog.findViewById(R.id.tv_bts);
            dialogCID = (TextView) dialog.findViewById(R.id.tv_cid);
            dialogLAC = (TextView) dialog.findViewById(R.id.tv_lac);
            dialogLAT = (TextView) dialog.findViewById(R.id.tv_lat);
            dialogLNG = (TextView) dialog.findViewById(R.id.tv_lng);
        }
        dialogID.setText("ID: " + tower.getId());
        dialogCID.setText("CID: " + tower.getCellId());
        dialogLAC.setText("LAC: " + tower.getLocationAreaCode());
        dialogBTS.setText("BTS: " + tower.getBts());
        dialogLAT.setText("LAT: " + tower.getLatLng().latitude);
        dialogLNG.setText("LNG: " + tower.getLatLng().longitude);
        dialog.show();
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(location != null && googleMap != null) {
                if(location.getLatitude() > 0 && location.getLongitude() > 0) {
                    setGPSMarker(new LatLng(location.getLatitude(), location.getLongitude()));
                    lastKnownLocation = location;
                    activity.lastKnownLocation = lastKnownLocation;
                }
            } else if(activity.lastKnownLocation != null) {
                Location loc = activity.lastKnownLocation;
                setGPSMarker(new LatLng(loc.getLatitude(), loc.getLongitude()));
                lastKnownLocation = loc;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(activity, provider.toUpperCase() + " location provider enabled.", Toast.LENGTH_LONG).show();
            locationUtils.getLocationUpdates(provider, this);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(activity, "Please enable your device " + provider.toUpperCase() + " location provider.", Toast.LENGTH_LONG).show();
        }
    };

    public TowerManager.LocationRequestCallback locationRequestCallback = new TowerManager.LocationRequestCallback() {
        @Override
        public void onSuccess(LatLng latLng, List<Tower> towers) {
            Log.e(tag, "locationRequestCallback: onSuccess " + latLng + " towers: " + towers.size());

            if(googleMap != null){
                googleMap.clear();
                markers = new ArrayList<>();

                if(gpsMarker != null && lastKnownLocation != null) {
                    setGPSMarker(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                }
                cellTowerMarkers = new HashMap<>();
                LatLng newLatLng = null;
                if(latLng.latitude != 0 && latLng.longitude != 0) {
                    newLatLng = latLng;
                    Location loc = new Location("towers");
                    loc.setLatitude(newLatLng.latitude);
                    loc.setLongitude(newLatLng.longitude);
                    activity.lastKnownLocation = loc;
                } else if(activity.lastKnownLocation != null) {
                    Location loc = activity.lastKnownLocation;
                    newLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                    setGPSMarker(newLatLng);
                    lastKnownLocation = loc;
                }

                addMyMarker(newLatLng == null ? latLng : newLatLng).showInfoWindow();

                for (int i = 0; i < towers.size(); i++) {
                    Tower tower = towers.get(i);
                    drawCircle(tower.getLatLng(), TowerMath.getDistance(latLng, tower.getLatLng()));
                    addMarker(tower);
                }
                setMarkerListener();
                if(!isMapBoundsSet){
                    try {
                        setMapBounds();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                savedTowers = null;
            }
        }

        @Override
        public void onFailed(Exception e) {
            Log.e("onFailed", "failed man!");

            if(activity.lastKnownLocation != null) {
                if(googleMap != null) {
                    if(isMapBoundsSet) {
                        googleMap.clear();
                        LatLng latlng = new LatLng(activity.lastKnownLocation.getLatitude(), activity.lastKnownLocation.getLongitude());
                        addMyMarker(latlng).showInfoWindow();
                        try {
                            setMapBounds();
                            zoomCamera(latlng, 16);
                            isMapBoundsSet = false;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
            if(e instanceof IOException || e instanceof ConnectException || e instanceof SocketException){
                Toast.makeText(activity, "No internet connection", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity, "Error caused by: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            e.printStackTrace();
        }
    };

    // Test functions
    private ArrayList<Tower> generateTower() throws JSONException{
        ArrayList<Tower> towers = new ArrayList<>();
        JSONObject jTower1 = new JSONObject();
        jTower1.put("cid", "60753");
        jTower1.put("lac", "4352");
        jTower1.put("mcc", "510");
        jTower1.put("mnc", "1");
        jTower1.put("signal", "9");
        towers.add(new Tower(jTower1));
        return towers;
    }

    private void testGeoLocate() throws JSONException {
        JSONArray jTowers = new JSONArray();

        JSONObject jTower1 = new JSONObject();
        jTower1.put("cid", "60753");
        jTower1.put("lac", "4352");
        jTower1.put("mcc", "510");
        jTower1.put("mnc", "1");
        jTower1.put("signal", "5");// + GeneralUtils.randInt(1, 50));
        jTowers.put(jTower1);

        JSONObject jTower2 = new JSONObject();
        jTower2.put("cid", "943");
        jTower2.put("lac", "4352");
        jTower2.put("mcc", "510");
        jTower2.put("mnc", "1");
        jTower2.put("signal", "12");// + GeneralUtils.randInt(1, 50));
        jTowers.put(jTower2);

        JSONObject jTower3 = new JSONObject();
        jTower3.put("cid", "914");
        jTower3.put("lac", "4352");
        jTower3.put("mcc", "510");
        jTower3.put("mnc", "1");
        jTower3.put("signal", "9");// + GeneralUtils.randInt(1, 50));
        jTowers.put(jTower3);

        towerManager.requestLocationByJson(jTowers, locationRequestCallback);
    }
}
