package com.example.willyspinner.android_mobileapp;

import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button tap_zangsh;
    TextView zangsh_ometer;
    TextView zangshtap_text;
    int zangsh_count;
    ProgressBar progressBar;
    long ZPM_INTERVAL_MS = 15000; // milliseconds.

    private void incr_zangsh_ometer(){
        int before_zangsh = zangsh_count;
        zangsh_count++;
        zangsh_ometer.setText(Integer.toString(zangsh_count));
        if (zangsh_count == 1){
            zangshtap_text.setText("zangsh tap");
        } else {
            zangshtap_text.setText("zangsh taps");
        }
    }
    private void reset_zangsh_ometer() {
        zangsh_count = 0;
        zangsh_ometer.setText(Integer.toString(zangsh_count));
        progressBar.setProgress(0);
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

        tap_zangsh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                incr_zangsh_ometer();
            }
        });
        progressBar.setMax((int)ZPM_INTERVAL_MS);
        new CountDownTimer(ZPM_INTERVAL_MS, 20) {
            public void onTick(long millisUntilFinished) {
                progressBar.setProgress((int) (ZPM_INTERVAL_MS - millisUntilFinished));
            }

            public void onFinish() {
                //TODO: do the HTTP request here.
                reset_zangsh_ometer();
                Toast.makeText(MainActivity.this, "Sent zangsh data to server.", Toast.LENGTH_SHORT).show();
                start(); // start it again.
            }

        }.start();

    }
}
