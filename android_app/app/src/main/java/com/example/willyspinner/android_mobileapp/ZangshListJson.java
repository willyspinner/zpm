package com.example.willyspinner.android_mobileapp;

import java.util.ArrayList;

public class ZangshListJson {
        public ZangshListJson(long timestamp_start){
                this.timestamp_start = timestamp_start;
        }
        public ArrayList<ZangshTap> zangsh_taps;
        public long timestamp_start;
        public String signature;
}
