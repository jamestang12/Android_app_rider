package com.example.riderapp.Commom;

import android.location.Location;

import com.example.riderapp.Remote.FCMClient;
import com.example.riderapp.Remote.GoogleMapAPI;
import com.example.riderapp.Remote.IFCMService;
import com.example.riderapp.Remote.IGoogleAPI;
import com.google.android.gms.maps.model.LatLng;

public class Common {

    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl = "RidersInformation";
    public static final String pickup_request_tbl = "PickupRequest";
    public static final String token_tbl = "Tokens";
    private static double base_fare = 2.55;
    private static double time_rate = 0.35;
    private static double distance_rate = 1.75;
    public static LatLng loc = null;
    public static LatLng dec = null;

    public static double getPrice(double km, int min){
        return (base_fare+(time_rate*min)+(distance_rate*km));
    }

    public static final String fcmURL = "https://fcm.googleapis.com/";
    public static final String googleAPIUrl = "https://maps.googleapis.com";


    public static IFCMService getFCMService(){
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }

    public static IGoogleAPI getGoogleService(){
        return GoogleMapAPI.getClient(googleAPIUrl).create(IGoogleAPI.class);
    }





}
