package com.example.willyspinner.android_mobileapp;

import java.util.ArrayList;

public class ZangshListJson {
        public ZangshListJson(long timestamp_start, int interval){
                this.timestamp_start = timestamp_start;
                this.interval = interval;
        }
        public ArrayList<ZangshTap> zangsh_taps;
        public int interval;
        public long timestamp_start;
        public String signature;
}
