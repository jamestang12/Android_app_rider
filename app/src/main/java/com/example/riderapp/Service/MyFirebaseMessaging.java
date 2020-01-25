package com.example.riderapp.Service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

public class MyFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
        if(remoteMessage.getNotification().getTitle().equals("Cancel")){
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyFirebaseMessaging.this,""+remoteMessage.getNotification().getBody(),Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if(remoteMessage.getNotification().getTitle().equals("Arrived")){
            showArrivedNotification(remoteMessage.getNotification().getBody());
        }
    }

    private void showArrivedNotification(String body){
        //PendingIntent contentIntent = PendingIntent.getActivities(getBaseContext(),0,new Intent(),PendingIntent.FLAG_ONE_SHOT);
    }
}
