package com.example.willyspinner.android_mobileapp;
import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    Button tap_zangsh;
    TextView zangsh_ometer;
    TextView zangshtap_text;
    TextView zpm_up_indicator;
    ObjectAnimator zpm_up_indicator_blink;
    TextView zpm_server_health_btn;
    TextView zpm_healthcheck_info_text;
    ProgressBar progressBar;

    // variables:
    final long ZPM_INTERVAL_MS = 15000; // milliseconds.
    final long HEALTHCHECK_INTERVAL_MS = 5000; // milliseconds.
    final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters
    final long MIN_TIME_BW_UPDATES = 2000; // 2 secs
    final String SERVER_PUT_URL = "https://bbb.homelinux.com/zangsh/zpm";
    final String SERVER_HEALTHCHECK_URL = "https://bbb.homelinux.com/zangsh/health";
    String mlocation_permission = Manifest.permission.ACCESS_FINE_LOCATION;
    String minternet_permission = Manifest.permission.INTERNET;
    final int REQUEST_CODE_PERMISSION = 2;

    // data structures & clients.
    ZangshListJson current_zlj; // zangsh list json. zangsh_taps, as well as timestamp_from.
    ArrayList<ZangshListJson> zlj_backlog_queue; // a queue of ZLJs that we failed to send in the past.
    GPSTracker gps;
    OkHttpClient http_client;
    int secs_since_last_healthcheck = 0;

    private void log_toast(String s){
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void insert_zangsh (ArrayList<ZangshTap> list){
        int before_zangsh = list.size();
        Location loc = gps.getLocation();
        if(!gps.canGetLocation) {
            gps.showSettingsAlert();
            return;
        }
        double current_lat = loc.getLatitude();
        double current_lng = loc.getLongitude();

        long currentUnixTime = System.currentTimeMillis() / 1000L;
        ZangshTap new_tap = new ZangshTap(current_lat, current_lng, currentUnixTime);

        list.add(new_tap);
        int zangsh_count = list.size();
        zangsh_ometer.setText(Integer.toString(zangsh_count));
        if (before_zangsh == 0 && zangsh_count == 1){
            zangshtap_text.setText("zangsh tap");
        } else if (before_zangsh == 1 && zangsh_count == 2){
            zangshtap_text.setText("zangsh taps");
        }
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

    private void http_request_healthcheck (Http_callback cb) {
        Request request = new Request.Builder()
                .url(SERVER_HEALTHCHECK_URL)
                .build();

        new Thread (new Runnable () {
            @Override
            public void run () {
                try {
                    Response response =  http_client.newCall (request).execute ();
                    if (response.code() == 200) {
                        cb.on_success();

                    } else {
                        cb.on_failure(new IOException());
                    }
                } catch (IOException e) {
                    Log.e("ERROR TING", e.getMessage());
                    e.printStackTrace ();
                    cb.on_failure(e);
                }
            }

        }).start ();
    }

    private void do_healthcheck () {
        http_request_healthcheck( new Http_callback(){
            @Override
            public void on_success() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        zpm_up_indicator.setText("UP");
                        zpm_up_indicator.setBackgroundColor(getResources().getColor(R.color.uptimeGreen, null));
                        manageBlinkEffect(false);
                    }
                });
            }
            @Override
            public void on_failure(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String msg = "Healthcheck failed.";
                        log_toast(msg);
                        zpm_up_indicator.setText("DOWN");
                        zpm_up_indicator.setBackgroundColor(getResources().getColor(R.color.uptimeRed, null));
                        manageBlinkEffect(true);
                    }
                });
            }
        });
    }

    private void publish_zangsh (ZangshListJson zlj, Http_callback cb) {
        // parse to json first.
        Gson gson = new Gson();
        String zlj_json = gson.toJson(zlj);
        // now do the http request.
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, zlj_json);
        Request request = new Request.Builder()
            .url(SERVER_PUT_URL)
            .put(body)
            .addHeader("X-BBB-Auth", "lalala")
            .build();

        new Thread (new Runnable () {
            @Override
            public void run () {
                try {
                    Response response =  http_client.newCall (request).execute ();
                    ServerResponse json_response = gson.fromJson(response.body().string(), ServerResponse.class);
                    if (json_response.status.equals("success")) {
                        cb.on_success();
                    } else {
                        cb.on_failure(new IOException());
                    }
                } catch (IOException e) {
                    Log.e("ERROR TING", e.getMessage());
                    e.printStackTrace ();
                    cb.on_failure(e);
                } catch (com.google.gson.JsonSyntaxException jse) {
                    Log.e("ERROR TING JSE", jse.getMessage());
                    jse.printStackTrace();
                    cb.on_failure(jse);
                }
            }

        }).start ();
    }
    private void manageBlinkEffect(boolean activated ) {
        if (activated) {
            zpm_up_indicator_blink.setDuration(500);
            zpm_up_indicator_blink.setEvaluator(new ArgbEvaluator());
            zpm_up_indicator_blink.setRepeatMode(ValueAnimator.REVERSE);
            zpm_up_indicator_blink.setRepeatCount(Animation.INFINITE);
            zpm_up_indicator_blink.start();
        } else {
            zpm_up_indicator_blink.cancel();

        }
    }

    public void sync_process_backlog () {
        //TODO: this is the incorrect way. Fix this.
        if (zlj_backlog_queue.size() > 0) {
            synchronized (zlj_backlog_queue) {
                for (Iterator<ZangshListJson> iterator = zlj_backlog_queue.iterator(); iterator.hasNext(); ) {
                    ZangshListJson past_zlj = iterator.next();
                    publish_zangsh(past_zlj, new Http_callback(){
                        @Override
                        public void on_success() {
                            zlj_backlog_queue.remove(past_zlj);
                        }
                    });
                }
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tap_zangsh = (Button) findViewById(R.id.zangsh_button);
        zangsh_ometer = (TextView) findViewById(R.id.zangsh_ometer);
        zangsh_ometer.setText("0");
        zangshtap_text = (TextView) findViewById(R.id.zangshtaps_text);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax((int)ZPM_INTERVAL_MS);
        zpm_up_indicator = (TextView) findViewById(R.id.textview_health_indicator);
        zpm_up_indicator_blink = ObjectAnimator.ofInt(zpm_up_indicator, "backgroundColor",
            getResources().getColor(R.color.uptimeRed, null),
            getResources().getColor(R.color.white, null)
       //     getResources().getColor(R.color.uptimeGreen, null)
        );
        zpm_server_health_btn = (TextView) findViewById(R.id.textview_healthcheck_button);
        zpm_healthcheck_info_text = (TextView) findViewById(R.id.textview_healthcheck_info);

        zlj_backlog_queue = new ArrayList<ZangshListJson>();
        /*      Requesting permissions here */
        try {
            if (ActivityCompat.checkSelfPermission(this, mlocation_permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{mlocation_permission},
                        REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (ActivityCompat.checkSelfPermission(this, minternet_permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{minternet_permission},
                        REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // permission granted to use gps. now good:
        gps = new GPSTracker(MainActivity.this,MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES);
        http_client = Http.client();

        do_healthcheck();
        CountDownTimer healthcheck_timer = new CountDownTimer(HEALTHCHECK_INTERVAL_MS, 1000) {
            public void onTick(long millisUntilFinished) {
                secs_since_last_healthcheck += 1;
                if (secs_since_last_healthcheck == 1) {
                    zpm_healthcheck_info_text.setText("1 second since last healthcheck.");
                } else {
                    zpm_healthcheck_info_text.setText(String.format("%s seconds since last healthcheck.", secs_since_last_healthcheck));
                }
            }
            public void onFinish() {
                do_healthcheck();
                secs_since_last_healthcheck = -1;
                start();
            }

        }.start();
        zpm_server_health_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                healthcheck_timer.cancel();
                do_healthcheck();
                healthcheck_timer.start();
                zpm_healthcheck_info_text.setText("0 seconds since last healthcheck.");
                secs_since_last_healthcheck = -1;
            }
        });


        tap_zangsh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                insert_zangsh(current_zlj.zangsh_taps);
            }
        });
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
                publish_zangsh(current_zlj, new Http_callback(){
                    @Override
                    public void on_success() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                log_toast(String.format("Post to server successful."));
                            }
                        });
                    }
                    @Override
                    public void on_failure(Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                log_toast(String.format("Post to server FAILED. %s : Adding to backlog..", e.getMessage()));
                            }
                        });
                        synchronized (zlj_backlog_queue) {
                            zlj_backlog_queue.add(current_zlj); // add to queue for later processing.
                        }
                    }
                });

                //TODO: process backlog queue here.
                // see https://stackoverflow.com/questions/24246783/okhttp-response-callbacks-on-the-main-thread
                start(); // start it again.
                current_zlj = new ZangshListJson(System.currentTimeMillis() / ((long)1000), (int)ZPM_INTERVAL_MS/ 1000);
                reset_zangsh(current_zlj);

                // remove in queue.
            }

        }.start();
        // initialize current_zlj.
        current_zlj = new ZangshListJson(System.currentTimeMillis()/((long)1000), (int)ZPM_INTERVAL_MS/ 1000);
        current_zlj.zangsh_taps = new ArrayList<ZangshTap>();
    }
}
