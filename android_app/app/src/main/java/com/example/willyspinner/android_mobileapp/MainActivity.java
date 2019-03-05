package com.example.willyspinner.android_mobileapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;


public class MainActivity extends AppCompatActivity {
    Button tap_zangsh;
    TextView zangsh_ometer;
    TextView zangshtap_text;
    ProgressBar progressBar;

    // variables:
    long ZPM_INTERVAL_MS = 15000; // milliseconds.
    ZangshListJson current_zlj; // zangsh list json. zangsh_taps, as well as timestamp_from.
    ArrayList<ZangshListJson> zlj_queue; // a queue of ZLJs that we failed to send in the past.
    GPSTracker gps;

    double current_lat = -999;
    double current_lng = -999;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int REQUEST_CODE_PERMISSION = 2;


    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 2000; // 2 secs


    private void log_toast(String s){
       Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    private void insert_zangsh (ArrayList<ZangshTap> list){
        int before_zangsh = list.size();
        Location loc = gps.getLocation();
        if(!gps.canGetLocation) {
            gps.showSettingsAlert();
            return;
        }
        current_lat = loc.getLatitude();
        current_lng = loc.getLongitude();

        long currentUnixTime = System.currentTimeMillis() / 1000L;
        ZangshTap new_tap = new ZangshTap(current_lat, current_lng, currentUnixTime);

        list.add(new_tap);
       int  zangsh_count = list.size();
        zangsh_ometer.setText(Integer.toString(zangsh_count));
        if (before_zangsh == 0 && zangsh_count == 1){
            zangshtap_text.setText("zangsh tap");
        } else if (before_zangsh == 1 && zangsh_count == 2){
            zangshtap_text.setText("zangsh taps");
        }
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        log_toast(String.format("INSERTING zangsh of %s", gson.toJson(new_tap)));

    }
    private void reset_zangsh (ZangshListJson zlj) {
        if (zlj.zangsh_taps== null){
            zlj.zangsh_taps= new ArrayList<ZangshTap>();
        } else if (zlj.zangsh_taps.size() != 0) {
            zlj.zangsh_taps.clear();
        }
        zangsh_ometer.setText(Integer.toString(0));
        progressBar.setProgress(0);
    }

    /*
    returns true if successful, returns false if not.
     */
    private boolean publish_zangsh (ZangshListJson zlj) {
        Gson gson = new Gson();
        String zlj_json = gson.toJson(zlj);
        log_toast(String.format("PUBLISHING zangsh list of %s, length: %d", zlj_json, zlj.zangsh_taps.size()));
        //TODO: do the HTTP request here.
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tap_zangsh = (Button) findViewById(R.id.zangsh_button);
        zangsh_ometer = (TextView) findViewById(R.id.zangsh_ometer);
        zangshtap_text = (TextView) findViewById(R.id.zangshtaps_text);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        zangsh_ometer.setText("0");
        zlj_queue = new ArrayList<ZangshListJson>();
        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            // permission granted to use gps. now good:
        gps = new GPSTracker(MainActivity.this,MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES);

        tap_zangsh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                insert_zangsh(current_zlj.zangsh_taps);
            }
        });
        progressBar.setMax((int)ZPM_INTERVAL_MS);
        new CountDownTimer(ZPM_INTERVAL_MS, 20) {
            public void onTick(long millisUntilFinished) {
                progressBar.setProgress((int) (ZPM_INTERVAL_MS - millisUntilFinished));
            }
            public void onFinish() {
                SecureRandom secRand = new SecureRandom();
                byte[] objIdBytes = new byte[16]; //128-bit
                secRand.nextBytes(objIdBytes);
                String objId = Base64.getEncoder().encodeToString(objIdBytes);
                current_zlj.signature = objId;
                boolean is_successful = publish_zangsh(current_zlj);
                if (is_successful) {
                    //log_toast("SUCCESS: zangsh data sent to server.");
                } else {
                    log_toast("FAILURE: zangsh data not sent to server.");
                    zlj_queue.add(current_zlj); // add to queue for later processing.
                }
                start(); // start it again.
                current_zlj = new ZangshListJson(System.currentTimeMillis() / ((long)1000));
                reset_zangsh(current_zlj);

                // remove in queue.
                if (zlj_queue.size() > 0) {
                    for(ZangshListJson past_zlj: zlj_queue){
                        boolean queue_removal_successful = publish_zangsh(past_zlj);
                        if (queue_removal_successful) {
                            zlj_queue.remove(past_zlj);
                        }
                    }
                }
            }

        }.start();
        // initialize current_zlj.
        current_zlj = new ZangshListJson(System.currentTimeMillis()/((long)1000));
        current_zlj.zangsh_taps = new ArrayList<ZangshTap>();

    }
}
