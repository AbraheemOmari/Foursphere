package com.abraheemomari.foursphere;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.abraheemomari.foursphere.MainFragment;
import com.abraheemomari.foursphere.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MainFragment.mainFragListener {

    MapUpdate mapUpdate;
    MainFragment mainFragment;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null)
        {
            return;
        }

        mainFragment = new MainFragment();
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
        //Allows us to return to our main fragment from the MapView
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
     * Called when a restaurant has been selected from the ReyclerView list and will
     * transition to a MapView of the restaurant selected
     * @param restaurant The restaurant the user has selected to view on a map
     * @param lastKnownLocation The user's last known location to be viewed on a map
     */
    @Override
    public void restaurantSelected(Restaurant restaurant, Location lastKnownLocation) {
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
}

/**
 *  Stores information necessary for updating camera and markers of the map in the MapView fragment
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
