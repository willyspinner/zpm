package com.example.willyspinner.android_mobileapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.willyspinner.android_mobileapp.ZangshTap;
import com.example.willyspinner.android_mobileapp.GPSTracker;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    Button tap_zangsh;
    TextView zangsh_ometer;
    TextView zangshtap_text;
    ProgressBar progressBar;

    //android managers
    LocationManager locationManager;

    // variables:
    long ZPM_INTERVAL_MS = 15000; // milliseconds.
    ArrayList<ZangshTap> current_zpm_list;
    double current_lat = -999;
    double current_lng = -999;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int REQUEST_CODE_PERMISSION = 2;

    GPSTracker gps;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 2000; // 2 secs



    private void log_toast(String s){
       Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
    }

    private void insert_zangsh (){
        int before_zangsh = current_zpm_list.size();
        Location loc = gps.getLocation();

        if(!gps.canGetLocation) {
            gps.showSettingsAlert();
            return;
        }

        current_lat = loc.getLatitude();
        current_lng = loc.getLongitude();
       /*
        if (current_lat == -999 || current_lng == -999) {
            Toast.makeText(MainActivity.this, "Lat Lng error. Need to enable permissions or location services.", Toast.LENGTH_LONG).show();
            return;
        }*/
        Log.i("TING", "here now..");
        long currentUnixTime = System.currentTimeMillis() / 1000L;
        ZangshTap new_tap = new ZangshTap(current_lat, current_lng, currentUnixTime);
        current_zpm_list.add(new_tap);
       int  zangsh_count = current_zpm_list.size();
        zangsh_ometer.setText(Integer.toString(zangsh_count));
        if (before_zangsh == 0 && zangsh_count == 1){
            zangshtap_text.setText("zangsh tap");
        } else if (before_zangsh == 1 && zangsh_count == 2){
            zangshtap_text.setText("zangsh taps");
        }
        log_toast(String.format("Inserting zangsh of lat lng : %f %f, at time: %d", current_lat, current_lng, currentUnixTime));
    }
    private void reset_zangsh () {
        current_zpm_list.clear();
        zangsh_ometer.setText(Integer.toString(0));
        progressBar.setProgress(0);
    }

    /*
    returns true if successful, returns false if not.

     */
    private boolean publish_zangsh () {
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
        current_zpm_list = new ArrayList<ZangshTap>();

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
                insert_zangsh();
            }
        });
        progressBar.setMax((int)ZPM_INTERVAL_MS);
        new CountDownTimer(ZPM_INTERVAL_MS, 20) {
            public void onTick(long millisUntilFinished) {
                progressBar.setProgress((int) (ZPM_INTERVAL_MS - millisUntilFinished));
            }
            public void onFinish() {
                start(); // start it again.
                boolean is_successful = publish_zangsh();
                if (is_successful) {
                    reset_zangsh();
                    Toast.makeText(MainActivity.this, "Sent zangsh data to server.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "FAILURE: zangsh data not to server.", Toast.LENGTH_SHORT).show();
                }
            }

        }.start();


    }
}
