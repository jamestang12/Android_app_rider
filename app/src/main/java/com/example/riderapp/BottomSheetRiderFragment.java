package com.example.riderapp;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.riderapp.Commom.Common;
import com.example.riderapp.Remote.IGoogleAPI;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BottomSheetRiderFragment extends BottomSheetDialogFragment {
    String mLocation, mDestination;
    IGoogleAPI mService;
    TextView txtCalculate,txtLocation,txtDestination;
    LatLng location, destination;
    String requestUrl = null;
    boolean isTapOnMap;

    public static BottomSheetDialogFragment newInstance(String location, String destination,boolean isTapOnMap){
        BottomSheetDialogFragment f = new BottomSheetRiderFragment();
        Bundle args = new Bundle();
        args.putString("location",location);
        args.putString("destination",destination);
        args.putBoolean("isTapOnMap",isTapOnMap);
        f.setArguments(args);
        return f;


    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = getArguments().getString("location");
        mDestination = getArguments().getString("destination");
        isTapOnMap = getArguments().getBoolean("isTapOnMap");

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_rider,container,false);
        txtLocation = (TextView)view.findViewById(R.id.txtLocation);
        txtDestination = (TextView)view.findViewById(R.id.txtDestination);
        txtCalculate = (TextView)view.findViewById(R.id.txtCalculate);

        if(!isTapOnMap){
            txtLocation.setText(mLocation);
            txtDestination.setText(mDestination);
        }

        mService = Common.getGoogleService();
        getPrice();

        return view;
    }
    private void getPrice(){

        try {

             requestUrl = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"
                    +"transit_routing_preference=less_driving&"
                    +"origin="+Common.loc.latitude+","+Common.loc.longitude+"&"
                    +"destination="+Common.dec.latitude+","+Common.dec.longitude+"&"
                    +"key="+"AIzaSyAS3krkQHp53NUbQHleBCji6XoXNCl_6lk";
            Log.d("Link",requestUrl);
            mService.getPath(requestUrl).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray routes = jsonObject.getJSONArray("routes");

                        JSONObject object = routes.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");

                        JSONObject legsObject = legs.getJSONObject(0);

                        JSONObject distance = legsObject.getJSONObject("distance");
                        String distance_text = distance.getString("text");

                        Double distance_value = Double.parseDouble(distance_text.replaceAll("[^0-9\\\\.]+",""));
                        Log.e("distance_value",String.valueOf(distance_value));

                        JSONObject time = legsObject.getJSONObject("duration");
                        String time_text = time.getString("text");
                        Integer time_value = Integer.parseInt(time_text.replaceAll("\\D+",""));
                        String final_caculate = String.format("%s + %s = $%.2f",distance_text,time_text,Common.getPrice(distance_value,time_value));
                        txtCalculate.setText(final_caculate);
                        if(isTapOnMap)
                        {
                            String start_address = legsObject.getString("start_address");
                            String end_address = legsObject.getString("end_address");

                            txtLocation.setText(start_address);
                            txtDestination.setText(end_address);

                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("ERROR",t.getMessage());
                }
            });
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }


    }
}
