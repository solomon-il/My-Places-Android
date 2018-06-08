package com.shlominet.utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

//class for the item object of "point of interest"
//Parcelable because Bundle, Serializable because SharedPref
public class MapPointObj implements Parcelable, Serializable{

    private double lat, lng;
    private String address;
    private String name;
    private String imgUrl;

    private int distanceMeter = 0;
    private double distanceMile = 0;
    private float rating;
    private String placeid;


    public MapPointObj(double lat, double lng, String address, String name, String imgUrl) {
        this.lat = lat;
        this.lng = lng;
        this.address = address;
        this.name = name;
        this.imgUrl = imgUrl;
    }
    //empty constructor
    public MapPointObj() {}

    //getters & setters
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public LatLng getLatLng() {
        return new LatLng(this.lat, this.lng);
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getDistanceMeter() {
        return distanceMeter;
    }

    public double getDistanceMile() {
        return distanceMile;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public float getRating() {
        return rating;
    }

    public String getPlaceid() {
        return placeid;
    }

    public void setPlaceid(String placeid) {
        this.placeid = placeid;
    }


    //method for calc the distance
    public void setDistance(double lat2, double lng2) {
        //if user didn't enable gps
        if (lat2 == 0 && lng2 == 0) {
            return;
        }
        double pk = (double) (180.f/Math.PI);

        double a1 = lat / pk;
        double a2 = lng / pk;
        double b1 = lat2 / pk;
        double b2 = lng2 / pk;

        double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
        double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
        double t3 = Math.sin(a1) * Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        this.distanceMeter = (int)(6366000 * tt);
        this.distanceMile = this.distanceMeter * 0.00062137119;
    }

    //for parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(imgUrl);
    }

    public static final Parcelable.Creator<MapPointObj> CREATOR = new Parcelable.Creator<MapPointObj>() {
        public MapPointObj createFromParcel(Parcel pc) {
            return new MapPointObj(pc);
        }
        public MapPointObj[] newArray(int size) {
            return new MapPointObj[size];
        }
    };

    public MapPointObj(Parcel in){
        lat = in.readDouble();
        lng = in.readDouble();
        address = in.readString();
        name = in.readString();
        imgUrl = in.readString();
    }

}
