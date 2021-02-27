package edu.temple.contacttracer;

import java.io.Serializable;

public class MyLocation implements Serializable {
    private String uuid;
    private double latitude;
    private double longitude;
    private long sedentary_begin;
    private long sedentary_end;


    public MyLocation(String uuid, double latitude, double longitude, long sedentary_begin, long sedentary_end){
        this.uuid = uuid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sedentary_begin = sedentary_begin;
        this.sedentary_end = sedentary_end;
    }

    public long getSedentary_begin() {
        return sedentary_begin;
    }

    public long getSedentary_end() {
        return sedentary_end;
    }

    public String getUuid() {
        return uuid;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }
}
