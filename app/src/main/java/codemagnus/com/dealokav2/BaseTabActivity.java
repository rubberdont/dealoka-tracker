package codemagnus.com.dealokav2;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.net.wifi.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import codemagnus.com.dealokav2.adapter.ScanResultAdapter;
import codemagnus.com.dealokav2.adapter.TowerAdapter;
import codemagnus.com.dealokav2.db.RequestDataSource;
import codemagnus.com.dealokav2.fragments.TowerListFragment;
import codemagnus.com.dealokav2.fragments.VisualizationFragment;
import codemagnus.com.dealokav2.fragments.WifiListFragment;
import codemagnus.com.dealokav2.http.WebserviceRequest;
import codemagnus.com.dealokav2.tower.Tower;
import codemagnus.com.dealokav2.tower.TowerManager;
import codemagnus.com.dealokav2.utils.Dealoka;
import codemagnus.com.dealokav2.utils.GeneralUtils;
import codemagnus.com.dealokav2.utils.LocationUtils;
import codemagnus.com.dealokav2.wifi.WifiDataUtils;
import codemagnus.com.dealokav2.wifi.WifiHelper;
import codemagnus.com.dealokav2.wifi.WifiObject;

/**
 * Created by codemagnus on 3/31/2015.
 */
public class BaseTabActivity extends ActionBarActivity {

    protected static final String TAG = "BaseHostActivity";
    private FragmentTabHost tabHost;
    private Toolbar toolbar;
    public Dealoka app;
    public SwitchCompat switchCompat;
    public List<WifiObject> tempResultList;
    public Location lastKnownLocation;

    public ArrayList<Tower> towers;
    private List<Tower> lastItemTowers = new ArrayList<>();

    public RequestDataSource dsRequest;
    public LocationUtils locationUtils;
    private WifiHelper wifiHelper;
    public WifiDataUtils wifiUtility;
    public TowerManager towerManager;

    //for timer
    private Spinner spinner;

    private String[] delayTitles = {"10sec","20sec","30sec","40sec","50sec","1min", "3min", "5min", "10min"};
    private int[] delayValues = {10000 , 20000, 30000, 40000, 50000, 60000, 180000, 300000, 600000};
    private SharedPreferences sharedpreferences;
    private CountDownTimer countDownTimer;
//    private boolean canPost = false;
    private TowerAdapter towerAdapter;
    private String randomSession = "";
    boolean viewActiveFirst = true;
    private String selectedTimeMilis = delayTitles[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (Dealoka) getApplicationContext();
        getDsRequest();

        setUpLocationUtils();
        initializeWifiHelper();
        setUpWifiDataUtils();
        getToolBar();
        setUpTimeSpinner();
        switchCompat = (SwitchCompat) getToolBar().findViewById(R.id.switchCompat);
        setupTabs();

        settUpTowerManager();

    }

    public void setUpTimeSpinner() {
        sharedpreferences = getSharedPreferences("Delay", Context.MODE_PRIVATE);
        spinner     = (Spinner) getToolBar().findViewById(R.id.spinner_nav);
        spinner     .setVisibility(View.VISIBLE);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, delayTitles));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                }
                setTowerRequestDelay(delayValues[position]);
                sharedpreferences.edit().putInt("spinner:position", position).apply();
                selectedTimeMilis = spinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTimeMilis = delayTitles[0];
            }
        });

        spinner.post(new Runnable() {
            @Override
            public void run() {
                spinner.setSelection(sharedpreferences.getInt("spinner:position", 1), true);
            }
        });
    }

    public int towerRequestDelay;

    private void setTowerRequestDelay(int value) {
        this.towerRequestDelay = value;
        startNewCountDown();
    }

    private void setUpWifiDataUtils() {
        wifiUtility = new WifiDataUtils(this, wifiHelper, lastKnownLocation);
    }

    private void initializeWifiHelper() {
        wifiHelper = new WifiHelper(this);
    }

    private void setUpLocationUtils() {
        locationUtils = new LocationUtils(this, 1000, 3);
        locationUtils.getLocationUpdates(LocationUtils.GPS_PROVIDER, locationListener);
        locationUtils.getLocationUpdates(LocationUtils.NETWORK_PROVIDER, locationListener);

        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        String best = locationUtils.getLocationManager().getBestProvider(crit, false);
        Log.d("TAG", "Best provider: " + best);
        locationUtils.getLocationUpdates(best, locationListener);
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(location != null) {
                lastKnownLocation = location;
                if(wifiUtility != null) {
                    wifiUtility.setLastKnownLocation(lastKnownLocation);
                }
                updateWifiViewsLatLong();
                updateTowerListLatLong();

            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(BaseTabActivity.this, provider.toUpperCase() + " location provider enabled.", Toast.LENGTH_LONG).show();
            locationUtils.getLocationUpdates(provider, this);
        }
        @Override
        public void onProviderDisabled(String provider) {
            if(getSwitchCompat() != null && getSwitchCompat().isChecked())
                Toast.makeText(BaseTabActivity.this, "Please enable your device " + provider.toUpperCase() + " location provider.", Toast.LENGTH_LONG).show();
        }
    };

    private void updateTowerListLatLong() {
        TowerListFragment towerFrag = getTowerListFragment();
        if(towerFrag != null) {
            towerFrag.getUserLastKnownLocation();
        }
    }

    private void updateWifiViewsLatLong() {
        WifiListFragment wifiFrag = getWifiFragment();
        if(wifiFrag != null) {
            wifiFrag.getUserLastKnownLocation();
        }
    }

    //set up tabs view
    private void setupTabs() {
        tabHost 	= (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost	.setup(this, getSupportFragmentManager(), R.id.tabcontent);

        View vTowers 	= LayoutInflater.from(this).inflate(R.layout.tabs_towers, null, false);
        View vMap 	    = LayoutInflater.from(this).inflate(R.layout.tabs_map, null, false);
        View vWifi 	    = LayoutInflater.from(this).inflate(R.layout.tabs_wifis, null, false);

        tabHost	.addTab(tabHost.newTabSpec(TowerListFragment.tag).setIndicator(vTowers),     TowerListFragment.class, null);
        tabHost	.addTab(tabHost.newTabSpec(VisualizationFragment.tag).setIndicator(vMap),    VisualizationFragment.class, null);
        tabHost	.addTab(tabHost.newTabSpec(WifiListFragment.tag).setIndicator(vWifi),        WifiListFragment.class, null);

        tabHost	.setCurrentTab(0);
        tabHost	.getTabWidget().setStripEnabled(false);
        tabHost	.getTabWidget().setDividerDrawable(null);
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (tabId.equals(VisualizationFragment.tag)) {
                    spinner.setVisibility(View.GONE);
                    switchCompat.setVisibility(View.GONE);
                }  else {
                    spinner.setVisibility(View.VISIBLE);
                    switchCompat.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public Toolbar getToolBar() {
        if(toolbar == null) {
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        }
        return toolbar;
    }

    public RequestDataSource getDsRequest() {
        if(dsRequest == null) {
            dsRequest = new RequestDataSource(this);
            dsRequest.open();
        }

        return dsRequest;
    }

    public Dealoka getApp() {
        return app;
    }

    public SwitchCompat getSwitchCompat() {
        return switchCompat;
    }

    public List<WifiObject> getCurrentResult() {
        return this.tempResultList;
    }

    public void setCurrentList(List<WifiObject> results) {
        this.tempResultList = results;
    }
    public ArrayList<Tower> getTowers() {
        return towers;
    }

    public void setTowers(ArrayList<Tower> towers) {
        this.towers = towers;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(dsRequest != null) {
            dsRequest.close();
            dsRequest = null;
        }

        if(wifiUtility != null) {
            if(wifiUtility.getWifiHelper() != null) {
                wifiUtility.getWifiHelper().removeUpdates();
            }
        }

        if(towerManager != null) {
            towerManager.stopTowerUpdate();
        }

        if(locationUtils != null ) {
            locationUtils.stopLocationUpdates();
        }

        if(towersurlConnection != null && towersurlConnection.getStatus() == AsyncTask.Status.RUNNING) {
            towersurlConnection.cancel(true);
        }

        if(urlConnection != null && urlConnection.getStatus() == AsyncTask.Status.RUNNING) {
            urlConnection.cancel(true);
        }
    }

    // The rest of the code from this line is for detecting wifi
    // Associated in WifiListFragment
    private long lastLongMillies = 0;
    private ScanResultAdapter adapter;
    private WebserviceRequest.HttpURLCONNECTION urlConnection;

    private List<WifiObject> wifis;

    @Override
    protected void onStart() {
        super.onStart();
        if(lastLongMillies == 0) {
            addPrimaryTowers();

            wifis = wifiUtility.getWifiObjects(wifiHelper.getScanResults(), "");
            adapter = new ScanResultAdapter(this, wifis);
            if(urlConnection == null)
                doSubmitWifisTask(wifis);

            wifiHelper.setScanResultsListener(new WifiHelper.ScanResultsListener() {
                @Override
                public void onChange(List<ScanResult> scanResults) {
                    if (scanResults != null) {
                        List<WifiObject> datas = wifiUtility.getWifiObjects(scanResults, "" + lastLongMillies);
                        adapter.setScanResults(datas);
                        wifis = datas;
                    }
                }
            });
        } else {
            setUpTowers(getTowers(), null, false);
        }
    }

    private void doSubmitWifisTask(List<WifiObject> wifis) {
//        if(canPost) {
            urlConnection = wifiUtility.sendWifiDatas(wifis, new WifiListFragment.WiFiSubmitCallback() {
                @Override
                public void onResult(int responseCode) {
                    urlConnection = null;
                    for (int x = 0; x < adapter.getCount(); x++) {
                        WifiObject savedWifi = adapter.getItem(x);
                        savedWifi.setStatus(responseCode == 200 ? "Success" : "Failed");
                        adapter.getWifis().set(x, savedWifi);
                    }
                    adapter.notifyDataSetChanged();
                    if(getWifiFragment() != null) {
                        getWifiFragment().setWifiName();
                        getWifiFragment().refreshListAdapter(adapter);
                    }
                }
            });
//        }
    }

    public ScanResultAdapter getScannedWifiAdapter(){
        return adapter;
    }

    private void startNewCountDown() {
        Log.e(TAG, countDownTimer != null ? "NOT NULL" : "NULL");
        if(countDownTimer == null) {
            Log.d(TAG, "Tower requested delay: " + towerRequestDelay);
            countDownTimer = new CountDownTimer(towerRequestDelay, 1000) {
                public void onTick(long millisUntilFinished) {
                    lastLongMillies = millisUntilFinished;
                    String currentTime = GeneralUtils.timeString(millisUntilFinished);
                    if(getWifiFragment() != null) {
                        getWifiFragment().setTimerText("" + currentTime);
                    }
                    if(getTowerListFragment() != null) {
                        getTowerListFragment().setTimerLabelText("" + currentTime);
                    }
                }
                public void onFinish() {

//                    Log.d(TAG, "Towers Primary: " + towerPrimary.size());
//                    Log.d(TAG, "Towers Neighbor: " + towerNeighbor.size());

                    if(towerPrimary != null && towerNeighbor != null)
                        initTowerListAdapter(towerPrimary, towerNeighbor);

                    if(wifis != null) {
                        setCurrentList(wifis);
                        if (urlConnection == null) {
                            doSubmitWifisTask(wifis);
                        }
                    }
                    countDownTimer.start();
                }
            };
            countDownTimer.start();
        }
    }

    private WifiListFragment getWifiFragment(){
        return (WifiListFragment) getSupportFragmentManager().findFragmentByTag(WifiListFragment.tag);
    }

    private TowerListFragment getTowerListFragment(){
        return (TowerListFragment) getSupportFragmentManager().findFragmentByTag(TowerListFragment.tag);
    }
    private VisualizationFragment getVisualizationFragment(){
        return (VisualizationFragment) getSupportFragmentManager().findFragmentByTag(VisualizationFragment.tag);
    }

    //TODO
    //for towers list goes here
    private WebserviceRequest.HttpURLCONNECTION towersurlConnection;

    private void submitTowers(final List<Tower> sendTowers) {
        JSONObject objTowers = GeneralUtils.submitTowerData(this, sendTowers, lastKnownLocation);
        towersurlConnection = towerManager.submitTowerCellsData(objTowers, false, new WebserviceRequest.NullCallback() {
            @Override
            public void onResult(int responseCode) {
                Log.d(TAG, "SubmitTowers resp: " + responseCode);
                towersurlConnection = null;
                for (int x = 0; x < towerAdapter.getTowers().size(); x++) {
                    towerAdapter.getTowers().get(x).setPosted(false);
                }
                for (int i = 0; i < sendTowers.size(); i++) {
                    Tower tower = sendTowers.get(i);
                    for (int x = 0; x < towerAdapter.getTowers().size(); x++) {
                        Tower savedTower = towerAdapter.getTowers().get(x);

                        if (GeneralUtils.checkIfTowersAreSame(tower, savedTower)) {
                            savedTower.setPosted(true);
                        }

                        tower.setPostStatus(responseCode == 200 ? "Success" : "Failed");
                        towerAdapter.getTowers().set(x, savedTower);
                    }
                }
                towerAdapter.notifyDataSetChanged();
                if(getTowerListFragment() != null) {
                    getTowerListFragment().setListAdapter(towerAdapter);
                }
            }
        });
    }

    private void settUpTowerManager() {
        towerManager = TowerManager.getInstance(this);
        towerManager.setOnPrimaryTowerChangedListener(towersChangedCallback);
        locationUtils = new LocationUtils(this, 1000, 3);

        locationUtils.getLocationUpdates(LocationUtils.GPS_PROVIDER, locationListener);
        locationUtils.getLocationUpdates(LocationUtils.NETWORK_PROVIDER, locationListener);

    }

    private void addPrimaryTowers() {
        try {
            if(getTowerManager() != null) {
                Tower primaryTower = getTowerManager().getPrimaryTower();
                if(primaryTower != null) {
                    primaryTower.setMillis(selectedTimeMilis);
                    if(getTowers() == null) {
                        setTowers(new ArrayList<Tower>());
                        getTowers().add(primaryTower);
                        lastItemTowers = getTowers();
                    }
                    Log.d(TAG, "Toggle add towers here: " + getTowers().size());
                    setUpTowers(getTowers(), new ArrayList<Tower>(), true);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Tower> towerPrimary, towerNeighbor;

    private TowerManager.PrimaryTowersChangeCallback towersChangedCallback = new TowerManager.PrimaryTowersChangeCallback() {
        @Override
        public void didTowersChanged(ArrayList<Tower> towersPrimary, ArrayList<Tower> towersNeighbor, Exception e) {
            Log.d(TAG, "Init tower list toggle: " + towersPrimary.size());
            Log.d(TAG, "Init tower list toggle neighbor: " + towersNeighbor.size());
//            if(| viewActiveFirst) {
//                Log.d(TAG, "expired");
//                viewActiveFirst = false;

            Log.e(TAG, "New Towers: " + towersPrimary.toString());
            if(getVisualizationFragment() != null){
                getVisualizationFragment().displayCurrentTower();
                getVisualizationFragment().saveTowers(towersPrimary);
                towerManager.getCurrentLocationByTowers(towersPrimary, getVisualizationFragment().locationRequestCallback);
                getVisualizationFragment().savedTowers = towersPrimary;
            }

            towerPrimary = towersPrimary;
            towerNeighbor = towersNeighbor;

//                initTowerListAdapter(towersPrimary, towersNeighbor);
//            }
        }
    };

    public void initTowerListAdapter(ArrayList<Tower> towersPrimary, ArrayList<Tower> towerNeighbor) {
        try{
            if(getTowers() == null) {
                setTowers(new ArrayList<Tower>());
            }
            if (getTowerListFragment() != null) {
                getTowerListFragment().initPhoneDetails();
            }

            if(getLastItemTowers() != null) {
                Tower towerSaved = getTowers().get(0);
                Tower lastTowers = getLastItemTowers().get(0);

                if (GeneralUtils.checkIfTowersAreSame(lastTowers, towerSaved)) {
                    towerSaved.setMillis("" + selectedTimeMilis);
                    getTowers().set(0, towerSaved);
                }
            }

            for(int i = 0; i < towersPrimary.size();i++) {
                Tower tower = towersPrimary.get(i);
                tower.setMillis("" +  selectedTimeMilis);
                getTowers().add(0, tower);
            }
        }finally {
            lastItemTowers = towersPrimary;
            setUpTowers(getTowers(), towerNeighbor, true);
        }
    }

    private void setUpTowers(ArrayList<Tower> towersPrimary,ArrayList<Tower> towersNeighbor, boolean pushData) {
        if(getTowers() == null) {
            setTowers(towersPrimary);
        }

        if(towerAdapter == null) {
            towerAdapter = new TowerAdapter(this, getTowers());
        } else {
            towerAdapter.clear();
            towerAdapter.setTowers(getTowers());
        }

        if (getTowerListFragment() != null) {
            getTowerListFragment().initPhoneDetails();
        }

        if(pushData) {
            if (towersPrimary.size() > 0) {
                if (lastKnownLocation != null) {
                    if(getSwitchCompat() != null && getSwitchCompat().isChecked()) {
                        if(towersurlConnection == null) {
                            //TODO here push the data for available towers
                            List<Tower> sendTowers = new ArrayList<>();
                            if(towersNeighbor != null && towersNeighbor.size() > 0) {
                                Log.e("Neighbor", "INSIDE HERE");
                                sendTowers.add(towersPrimary.get(0));
                                sendTowers.addAll(towersNeighbor);
                            } else {
                                Log.e("!Neighbor", "INSIDE HERE");
                                sendTowers = getTowersWithInRange(getTowers());
                            }
                            if(sendTowers.size() > 0) {
                                if(towersPrimary.size() > 0) {
                                    submitTowers(sendTowers);
                                    return;
                                }
                            }
                            towersurlConnection = null;
                        }
                    }
                } else {
                    if(getSwitchCompat() != null && getSwitchCompat().isChecked())
                        Toast.makeText(BaseTabActivity.this, "Please enable your device location provider.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private List<Tower> getTowersWithInRange(ArrayList<Tower> savedTowers) {
        List<Tower> pushTowers = new ArrayList<>();
        try {
            Calendar newestCalendar = GeneralUtils.stringTimeToCalendar(savedTowers.get(0).getTime(), TimeZone.getDefault());
            for(Tower tower: savedTowers) {
                if(GeneralUtils.checkIfWithInRange(newestCalendar, tower)) {
                    pushTowers.add(tower);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return pushTowers;
    }


    //Getters
    public WifiHelper getWifiHelper() {
        return wifiHelper;
    }

    public WifiDataUtils getWifiUtility(){
        return wifiUtility;
    }

    public TowerManager getTowerManager() {
        return towerManager;
    }

    public List<Tower> getLastItemTowers() {
        return lastItemTowers;
    }

    public String getRandomSession() {
        if(randomSession.equals("")) {
            randomSession =  new SessionIdentifierGenerator().nextSessionId();
        }
        return randomSession;
    }

    public TowerAdapter getTowerAdapter() {
        return towerAdapter;
    }

    public final class SessionIdentifierGenerator {
        private SecureRandom random = new SecureRandom();
        public String nextSessionId() {
            return new BigInteger(130, random).toString(32);
        }
    }

    public Spinner getSpinner() {
        return spinner;
    }
}
