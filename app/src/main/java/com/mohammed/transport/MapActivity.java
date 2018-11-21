package com.mohammed.transport;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 987;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    public Location mLastLocation;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback = getLocationCallback();

    private FirebaseAuth mAuth;
    private SupportMapFragment mapFragment;

    private LinearLayout mDriverInfo;
    private TextView mDriverName;
    private TextView mDriverPhone;
    private TextView  mDriverCar;

    private ImageView mUserIcon;
    private TextView mUserName, mEmailAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();

        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverName = findViewById(R.id.driverName);
        mDriverPhone = findViewById(R.id.driverPhone);
        mDriverCar = findViewById(R.id.driverCar);

        FloatingActionButton fab = findViewById(R.id.floating_call_my_taxi);
        FloatingActionButton floatingSearchDriver = findViewById(R.id.floating_search_driver);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fab.setOnClickListener((View v) -> callMyTaxi(mAuth.getUid()));
        floatingSearchDriver.setOnClickListener((View v) -> getDriversLocation());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);


        mUserIcon = headerView.findViewById(R.id.mUserIcon);
        mUserName = headerView.findViewById(R.id.mUserName);
        mEmailAddress = headerView.findViewById(R.id.mEmailAddress);
        updateDrawerUI(mAuth.getCurrentUser());


    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_settings:
                Intent intent = new Intent(this, CustomerSettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_history:
                Intent intentH = new Intent(this, HistoryActivity.class);
                intentH.putExtra("customerOrDriver", "Customers");
                startActivity(intentH);
                break;
            case R.id.nav_logout:
                logoutFromFirebase();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mLocationRequest = new LocationRequest()
                .setInterval(5000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {// Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLongitude(), location.getLatitude());
                            mLastLocation = location;
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                        }
                    });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(this, location -> {// Got last known location. In some rare situations this can be null.
                                    if (location != null) {
                                        LatLng latLng = new LatLng(location.getLongitude(), location.getLatitude());
                                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                                    }
                                });
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                    requestLocationPermissions();
                }
            }
        }
    }

    private void callMyTaxi(String currentUserID) {
        if (mLastLocation!=null){
            DatabaseReference customerRequestdb = FirebaseDatabase.getInstance().getReference("customerRequest");
            GeoLocation userCurrentGeolocation = new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            GeoFire geoFire = new GeoFire(customerRequestdb);
            geoFire.setLocation(currentUserID, userCurrentGeolocation,
                    (key, error) -> {
                Toast.makeText(getApplicationContext(),R.string.calling_taxi, Toast.LENGTH_SHORT).show();
                LatLng customerLocationRequest = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(customerLocationRequest).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
            });


            GeoQuery geoQuery = geoFire.queryAtLocation(userCurrentGeolocation, 0.6);
            geoQuery.removeAllListeners();
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    DatabaseReference driverRequestdb = FirebaseDatabase.getInstance().getReference("StaticDriverLocation").child("l");
                    driverRequestdb.addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    ArrayList<Object> driverLocation = (ArrayList<Object>) dataSnapshot.getValue();
                                    if (driverLocation != null) {
                                        String latitude = driverLocation.get(0).toString();
                                        String longitude = driverLocation.get(1).toString();
                                        GeoLocation currentDriverGeolocation = new GeoLocation(Double.valueOf(latitude), Double.valueOf(longitude));

                                        if(isDistanceBetweenUserAndDriverClose(userCurrentGeolocation,currentDriverGeolocation)){
                                            Toast.makeText(getApplicationContext(), R.string.driver_close, Toast.LENGTH_SHORT).show();
                                            showDriverInfo();
                                        } else {
                                            Toast.makeText(getApplicationContext(), R.string.driver_far, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(getApplicationContext(), databaseError.getCode(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                @Override
                public void onKeyExited(String key) {
                    System.out.println(String.format("Key %s is no longer in the search area", key));
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
                }

                @Override
                public void onGeoQueryReady() {
                    System.out.println("All initial data has been loaded and events have been fired!");
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    System.err.println("There was an error with this query: " + error);
                }
            });
        }


    }

    private void showDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        mDriverName.setText("DRIVER_NAME");
        mDriverPhone.setText("DRIVER_PHONE");
        mDriverCar.setText("DRIVER_CAR");

    }

    private boolean isDistanceBetweenUserAndDriverClose(GeoLocation userCurrentGeolocation, GeoLocation currentDriverGeolocation ){
        Location loc1 = new Location("");
        loc1.setLatitude(userCurrentGeolocation.latitude);
        loc1.setLongitude(userCurrentGeolocation.longitude);

        Location loc2 = new Location("");
        loc2.setLatitude(currentDriverGeolocation.latitude);
        loc2.setLongitude(currentDriverGeolocation.longitude);

        //getting the distance between the driver and vehicle
        float distance = loc1.distanceTo(loc2);

        return distance < 200; // if distance < 200 return true else false

    }


    @NonNull
    private LocationCallback getLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (getApplicationContext() != null) {
                        mLastLocation = location;
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

                    }
                }
            }
        };
    }

    private void logoutFromFirebase() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    Intent intent = new Intent(MapActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
    }


    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    private void updateDrawerUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            String userName = currentUser.getDisplayName();
            String userEmail = currentUser.getEmail();
            Uri userIcon = currentUser.getPhotoUrl();
            if (userName != null) {
                mUserName.setText(userName);
            }
            if (userEmail != null) {
                mEmailAddress.setText(userEmail);
            }
            if (userIcon != null) {
                Glide.with(getApplicationContext())
                        .load(userIcon)
                        .apply(RequestOptions.circleCropTransform())
                        .into(mUserIcon);
            }
        }
    }

    private void getDriversLocation() {
        DatabaseReference staticDriverLocationDB = FirebaseDatabase.getInstance().getReference("StaticDriverLocation").child("l");
        staticDriverLocationDB.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ArrayList<String> driverLocation = (ArrayList<String>) dataSnapshot.getValue();
                        if (driverLocation != null) {
                            String latitude = driverLocation.get(0);
                            String longitude = driverLocation.get(1);
                            LatLng driverLocationLatLng = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
                            mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getApplicationContext(), databaseError.getCode(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
