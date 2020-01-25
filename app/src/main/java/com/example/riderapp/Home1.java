package com.example.riderapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;

import com.example.riderapp.Commom.Common;
import com.example.riderapp.Helper.CustomInfoWindow;
import com.example.riderapp.Model.FCMResponse;
import com.example.riderapp.Model.Notification;
import com.example.riderapp.Model.Sender;
import com.example.riderapp.Model.Token;
import com.example.riderapp.Model.User;
import com.example.riderapp.Remote.IFCMService;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Objects;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Home1 extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {

    SupportMapFragment mapFragment;

    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference ref;
    GeoFire geoFire;
    Marker mUserMarker,markerDestination;
    LatLng location;
    //Bottomsheet
    ImageView imgExpandable;
    BottomSheetRiderFragment mBottomSheet;
    Button btnPickupRequest;

    boolean isDriveFound = false;
    String driverId = "";
    int radius = 1;
    int distance = 1;
    private static final int LIMIT = 3;

    DatabaseReference driver6969 = FirebaseDatabase.getInstance().getReference(Common.token_tbl);

    AutocompleteSupportFragment place_location, place_destination;


    String mPlaceLocation, mPlaceDestion;


    //Send Alert
    IFCMService mService;

    //Presense system
    DatabaseReference driversAvailable;

    PlacesClient placesClient;

    private AppBarConfiguration mAppBarConfiguration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home1);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
                R.id.nav_tools, R.id.nav_share, R.id.nav_send)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        mService = Common.getFCMService();

        //Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Geo Fire
        //ref = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        //geoFire = new GeoFire(ref);

        btnPickupRequest = (Button)findViewById(R.id.btnPickupRequest);
        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isDriveFound)
                    requestPickupHere(FirebaseAuth.getInstance().getCurrentUser().getUid());
                else
                    sendRequesToDriver(driverId);
            }
        });

        place_location = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_location);
        place_destination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_destination);

        setUpLocation();

        updateFirebaseToken();



        //Init view
        imgExpandable = (ImageView)findViewById(R.id.imgExpandable);



        Places.initialize(getApplicationContext(),"AIzaSyDqmiAt4qW-FKPJmrozOiKt0Asas-Z4j6A");
        PlacesClient placesClient = Places.createClient(this);
        if (!Places.isInitialized()){
            Places.initialize(getApplicationContext(),"AIzaSyDqmiAt4qW-FKPJmrozOiKt0Asas-Z4j6A");
        }

        placesClient = Places.createClient(this);
        //AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_location);
        //AutocompleteSupportFragment autocompleteSupportFragment1 = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_destination);
       place_location.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG));
       place_destination.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG));


        place_location.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                mMap.clear();
                Log.i("PlacesApi","onPlaceSelected: "+ place.getName()+", "+place.getId()+String.valueOf(place.getLatLng()));
                mPlaceLocation = place.getName();
                mPlaceLocation = mPlaceLocation.replace(" ","+");
                Common.loc = place.getLatLng();
                mUserMarker = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).icon(BitmapDescriptorFactory.defaultMarker()));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15.0f));
                driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
                driversAvailable.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //If have any change happens on drivers table, it will reload all drivers available
                        loadAllAvailableDriver2(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i("PlacesApi","An error occurred " + status);
            }
        });

        place_destination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.i("PlacesApi","onPlaceSelected: "+ place.getName()+", "+place.getId());
                mPlaceDestion = place.getName();
                mPlaceDestion = mPlaceDestion.replace(" ","+");
                mMap.addMarker(new MarkerOptions().position(place.getLatLng()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15.0f));
                Common.dec = place.getLatLng();
                BottomSheetRiderFragment mBottomSheet = (BottomSheetRiderFragment) BottomSheetRiderFragment.newInstance(mPlaceLocation,mPlaceDestion,false);
                mBottomSheet.show(getSupportFragmentManager(),mBottomSheet.getTag());
                //Test
                driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
                driversAvailable.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //If have any change happens on drivers table, it will reload all drivers available
                        loadAllAvailableDriver2(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i("PlacesApi","An error occurred " + status);
            }
        });


    }

    private void updateFirebaseToken(){
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);

    }

    private void sendRequesToDriver(String driverId){
        //Toast.makeText(Home1.this,driverId,Toast.LENGTH_SHORT).show();

        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.token_tbl);
        tokens.orderByKey().equalTo(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //for (DataSnapshot postSnapShot:dataSnapshot.getChildren()){
                for (DataSnapshot postSnapShot:dataSnapshot.getChildren()){
                    Token token = postSnapShot.getValue(Token.class);
                    //Make raw payload - convert LatLng to json
                    String json_lat_lng = new Gson().toJson(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                    String riderToken = FirebaseInstanceId.getInstance().getToken();
                    Toast.makeText(Home1.this,FirebaseAuth.getInstance().getCurrentUser().getUid(),Toast.LENGTH_SHORT).show();

                    Notification data = new Notification(riderToken, json_lat_lng); // Send it to Drover app and we will deserialize it again
                    //Notification data = new Notification(FirebaseAuth.getInstance().getCurrentUser().getUid(),json_lat_lng);
                    Sender content = new Sender(data,token.getToken());

                    mService.snedMessage(content).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if(response.body().success ==1){
                                Toast.makeText(Home1.this,"Request sent!",Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(Home1.this,"Failed to send request!",Toast.LENGTH_SHORT).show();

                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.e("ERROR",t.getMessage());
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void sendReq2(String d2){
        DatabaseReference tokens =  FirebaseDatabase.getInstance().getReference(Common.token_tbl);
        tokens.orderByKey().equalTo(d2).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot post:dataSnapshot.getChildren()){
                    Token token = post.getValue(Token.class);
                    String json_lat_lng = new Gson().toJson(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                    Notification data = new Notification("EDMTDEV", json_lat_lng);
                    Sender content = new Sender(data, token.getToken());
                    mService.snedMessage(content).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body().success ==1 ){
                                Toast.makeText(Home1.this,"Request sent",Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(Home1.this,"Failed to send request",Toast.LENGTH_SHORT).show();

                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }




    private void requestPickupHere(String uid){
        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
        if(mUserMarker.isVisible())
            mUserMarker.remove();
        //Add new marker
        mUserMarker = mMap.addMarker(new MarkerOptions()
                .title("Pickup Here")
                .snippet("")
                .position(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mUserMarker.showInfoWindow();

        btnPickupRequest.setText("Getting you a DRIVER...");

        findDriver();


    }

    private void findDriver(){
        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gfDrivers = new GeoFire(drivers);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()),radius);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //If found
                if (!isDriveFound){
                    isDriveFound = true;
                    driverId = key;
                    btnPickupRequest.setText("CALL DRIVER");
                    //Toast.makeText(Home1.this,""+key,Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //If still not found driver, increase distance
                if(!isDriveFound && radius < LIMIT){
                    radius++;
                    findDriver();
                }
                else {
                    Toast.makeText(Home1.this,"No available driver near you",Toast.LENGTH_SHORT).show();
                    btnPickupRequest.setText("REQUEST PICKUP");
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home1, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        try {
            boolean ISuccess = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this,R.raw.my_map_style)
            );
        }catch (Resources.NotFoundException ex){
            ex.printStackTrace();
        }

        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (markerDestination != null){
                    markerDestination.remove();
                }
                mMap.clear();
                mUserMarker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker()).position(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude())));
                markerDestination = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).position(latLng).title("Destination"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15.0f));
                Common.loc = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                Common.dec = new LatLng(latLng.latitude,latLng.longitude);

                BottomSheetRiderFragment mBottomSheet = (BottomSheetRiderFragment) BottomSheetRiderFragment.newInstance(String.format("%f,%f",mLastLocation.getLatitude(),mLastLocation.getLongitude()),
                        String.format("%f,%f",latLng.latitude,latLng.longitude),true);
                mBottomSheet.show(getSupportFragmentManager(),mBottomSheet.getTag());
                //Test
                driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
                driversAvailable.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //If have any change happens on drivers table, it will reload all drivers available
                        loadAllAvailableDriver2(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    private void setUpLocation(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //Requst runtime permission
            ActivityCompat.requestPermissions(this , new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }
        else{
            if(checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }

    }

    private  boolean checkPlayServices(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError((resultCode)))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICE_RES_REQUEST);
            else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void displayLocation(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null){
            //Presense System
            driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
            driversAvailable.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //If have any change happens on drivers table, it will reload all drivers available
                    loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
                final double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();


                //Add Marker
                if(mUserMarker != null) {
                    mUserMarker.remove();
                }

                mUserMarker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2)).position(new LatLng(latitude, longitude)).title("Your Location"));

                //LatLng test = new LatLng(-34,151);
                //mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car1)).position(test).title("You"));


                //Move camera to this postion
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));
                //Draw animation rotate marker

                loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                Log.d("EDMTDEV",String.format("Your location was changes : %f / %f",latitude,longitude));
        }
        else{
            Log.d("ERROR"," Cannot get your location");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (checkPlayServices()){
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }


    private void loadAllAvailableDriver(final LatLng location){
        //Delete all markers on the map include rider location marker and available drivers marker
        mMap.clear();
        //Add marker back for rider location
        mMap.addMarker(new MarkerOptions().position(location));

        //Load all avilable Driver in distance 3km
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gf = new GeoFire(driverLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.latitude,location.longitude),distance);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                //Use key to get email from table Users
                //Table Users is table when driver register account and update information
                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //Rider and User model is same properties
                        //Use Rider model to get User
                       // User rider = dataSnapshot.getValue(User.class);
                        //User rider = dataSnapshot.getValue(User.class);
                        User rider = dataSnapshot.getValue(User.class);
                        //String name = rider.getName();
                        //Toast.makeText(Home1.this,name,Toast.LENGTH_LONG).show();
                        //Add driver to map
                       mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude,location.longitude)).flat(true).title(rider.getName()).snippet("Phone : " + rider.getPhone()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                       //mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude,location.longitude)).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <= LIMIT ){ //distance just find for 3km
                    distance++;
                    loadAllAvailableDriver(location);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void loadAllAvailableDriver2(final LatLng location){
        //Add marker back for rider location
        //mMap.addMarker(new MarkerOptions().position(location));

        //Load all avilable Driver in distance 3km
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gf = new GeoFire(driverLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.latitude,location.longitude),distance);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                //Use key to get email from table Users
                //Table Users is table when driver register account and update information
                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //Rider and User model is same properties
                        //Use Rider model to get User
                        // User rider = dataSnapshot.getValue(User.class);
                        //User rider = dataSnapshot.getValue(User.class);
                        User rider = dataSnapshot.getValue(User.class);
                        //String name = rider.getName();
                        //Toast.makeText(Home1.this,name,Toast.LENGTH_LONG).show();
                        //Add driver to map
                        mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude,location.longitude)).flat(true).title(rider.getName()).snippet("Phone : " + rider.getPhone()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                        //mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude,location.longitude)).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <= LIMIT ){ //distance just find for 3km
                    distance++;
                    loadAllAvailableDriver(location);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }


    private void startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }
}
