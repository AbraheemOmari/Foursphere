package com.abraheemomari.foursphere;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MainFragment.mainFragListener, LocationListener {

    MapUpdate mapUpdate;
    MainFragment mainFragment;
    SupportMapFragment mapFragment;
    LocationManager locationManager;
    Location lastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //We want to start listening for locations as soon as the app starts so that we hopefully get an update before we need to use it
        updateLocation();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null)
        {
            return;
        }

        mainFragment = new MainFragment();
        mainFragment.setLocation(lastKnownLocation);

        mapFragment = SupportMapFragment.newInstance();

        //Add main fragment which is a scrollable RecyclerView listing nearby restaurants
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, mainFragment);
        ft.addToBackStack(null);
        ft.commit();
        fm.executePendingTransactions();
    }

    @Override
    public void onBackPressed() {
        //Allows us to return to our main fragment from the MapView or exit the app if we are on our map fragment
        if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
            this.finish();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }


    /**
     * Called when the MapView has been created in the map fragment and is ready to be used.
     *
     * @param map The map that will be viewable
     */
    @Override
    public void onMapReady(final GoogleMap map) {

        //Create markers showing the user's location and the selected restaurant from the mapUpdate object
        MarkerOptions userMarker = new MarkerOptions()
                .position(mapUpdate.getUserLatLng())
                .title("You");

        MarkerOptions restaurantMarker = new MarkerOptions()
                .position(mapUpdate.getRestaurantLatLng())
                .title(mapUpdate.getRestaurantName());

        //Add the markers to the map
        map.addMarker(userMarker);
        map.addMarker(restaurantMarker).showInfoWindow();

        //Fit the camera of the MapView to our markers
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(userMarker.getPosition());
        builder.include(restaurantMarker.getPosition());
        LatLngBounds bounds = builder.build();

        final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 250);
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                map.moveCamera(cameraUpdate);
            }
        });
    }

    /**
     * Enable GPS if it isn't already, check for location permissions, get last known location, and start listening for new location updates
     */
    public void updateLocation()
    {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //Checks if GPS is enabled and opens settings if not
        boolean GPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!GPSEnabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
        }

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);

        //Check if app has permissions for location services and request them if not
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }

        /*Get the device's last known location and start listening for an updated one.
          If the device has just turned on then getLastKnownLocation may return null,
          so we start listening for a location and loop until we get it. This is not
          great practice because it is looping on the main UI thread, but this app is
          supposed to be a simple demo, this is a niche case, and it shouldn't take
          long in most cases to get a location, so it it's fine.
        */
        lastKnownLocation = locationManager.getLastKnownLocation(provider);
        locationManager.requestLocationUpdates(provider, 15000, 0, this);
        while (lastKnownLocation == null) {
        }
    }

    /**
     * Called when a restaurant has been selected from the ReyclerView list and will
     * transition to a MapView of the restaurant selected
     * @param restaurant The restaurant the user has selected to view on a map
     */
    @Override
    public void restaurantSelected(Restaurant restaurant) {
        //Get locations for the user and restaurant markers and then pass them to a new mapUpdate object
        LatLng userLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        LatLng restaurantLatLng = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
        mapUpdate = new MapUpdate(restaurantLatLng, userLatLng, restaurant.getName());

        if (mapFragment == null)
        {
            mapFragment = SupportMapFragment.newInstance();
        }
        //Add the MapView's fragment to the screen now that the mapUpdate is ready, and call getMapAsync() to update the map
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
        ft.add(R.id.fragment_container, mapFragment);
        ft.addToBackStack(null);
        ft.commit();

        fm.executePendingTransactions();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }
        //Stop listening for updates when we leave the app
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        //We want to update our last known location to the new location
        lastKnownLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}

/**
 *  Stores information necessary for updating camera and markers on the map in the MapView fragment
 */
class MapUpdate{

    private LatLng restaurantLatLng;
    private LatLng userLatLng;
    private String restaurantName;

    public MapUpdate(LatLng restaurantLatLng, LatLng userLatLng, String restaurantName)
    {
        this.restaurantLatLng = restaurantLatLng;
        this.userLatLng = userLatLng;
        this.restaurantName = restaurantName;
    }

    public LatLng getRestaurantLatLng()
    {
        return restaurantLatLng;
    }

    public LatLng getUserLatLng()
    {
        return userLatLng;
    }

    public String getRestaurantName()
    {
        return restaurantName;
    }
}
