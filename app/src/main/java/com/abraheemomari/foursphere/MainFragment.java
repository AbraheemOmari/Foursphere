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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.abraheemomari.foursphere.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 *  The main fragment of the app;
 *  Holds a scrollable RecyclerView which holds a view for each nearby restaurant
 */
public class MainFragment extends Fragment{
    public interface mainFragListener {
        public void restaurantSelected(Restaurant restaurant);
    }

    private Context context;
    private RecyclerView mRecyclerView;
    private ArrayList<Restaurant> restaurants;
    private mainFragListener listener;
    private Location lastKnownLocation;

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        context = this.getActivity().getApplicationContext();

        listener = (mainFragListener) this.getActivity();

        initialize(view);

        return view;
    }

    public void setLocation(Location location)
    {
        lastKnownLocation = location;
    }

    /**
     * Creates a request to Foursquare's servers for nearby restaurants,
     * and populates the RecyclerView with data
     * @param view The fragment's inflated layout
     */
    public void initialize(View view)
    {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));

        restaurants = new ArrayList<>();


        // Create adapter and set listener
        final RestaurantsAdapter.restaurantAdapterListener adapterListener = new RestaurantsAdapter.restaurantAdapterListener() {
            @Override
            public void restaurantSelected(Restaurant restaurant) {
                listener.restaurantSelected(restaurant);
            }
        };

        RestaurantsAdapter adapter = new RestaurantsAdapter(context, restaurants, adapterListener, lastKnownLocation);

        // Attach the adapter to the RecyclerView to populate views
        mRecyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        String latitude = String.valueOf(lastKnownLocation.getLatitude());
        String longitude = String.valueOf(lastKnownLocation.getLongitude());

            /***
             * API info redacted
             */
            //Uses the Foursquare's "explore" end-point which is intended for finding unspecific new venues
            String url = "https://api.foursquare.com/v2/venues/explore" +
                    "?client_id=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" +
                    "&client_secret=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" +
                    "&v=20130815" +
                    "&ll=" +
                    latitude + "," + longitude +
                    "&venuePhotos=1" +
                    "&query=food";

            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (response != null) {
                                try {
                                    JSONObject meta = response.getJSONObject("meta");
                                    //Check if request was successful
                                    if (meta.getString("code").equals("200")) {
                                        //JSON request was successful
                                        JSONObject fsResponse = response.getJSONObject("response");
                                        JSONArray groups = fsResponse.getJSONArray("groups");
                                        //The "items" array represents an array of venues
                                        JSONArray items = groups.getJSONObject(0).getJSONArray("items");

                                        for (int i = 0; i < items.length(); ++i) {
                                            String venueId = "";
                                            String venueName = "";
                                            String venueCategory = "";
                                            double latitude = 0;
                                            double longitude = 0;
                                            String photoURL = "";
                                            String isOpen = "";

                                            JSONObject item = items.getJSONObject(i);
                                            JSONObject venue = item.getJSONObject("venue");
                                            venueId = venue.getString("id");
                                            venueName = venue.getString("name");

                                            JSONObject location = venue.getJSONObject("location");
                                            latitude = Double.valueOf(location.getString("lat"));
                                            longitude = Double.valueOf(location.getString("lng"));

                                            JSONArray categories = venue.getJSONArray("categories");
                                            //First category in array is the primary category
                                            JSONObject category = categories.getJSONObject(0);
                                            venueCategory = category.getString("shortName");

                                            JSONObject photos = venue.getJSONObject("photos");
                                            int photoCount = photos.getInt("count");
                                            //Checks if venue photos are available before attempting to retrieve them
                                            if (photoCount > 0) {
                                                JSONArray photoGroup = photos.getJSONArray("groups");
                                                //Get first photo available for simplicity
                                                JSONArray photoItems = photoGroup.getJSONObject(0).getJSONArray("items");
                                                photoURL = photoJSONToURL(photoItems.getJSONObject(0));
                                            }

                                            //Hours aren't gauranteed to be available
                                            if (venue.has("hours")) {
                                                JSONObject hours = venue.getJSONObject("hours");
                                                isOpen = hours.getString("isOpen");
                                            }

                                            //Add the venue to the database if it doesn't already exist
                                            if (!Database.getInstance(context).venueExists(venueId)) {
                                                Database.getInstance(context).addVenue(venueId, venueName, venueCategory, latitude, longitude, photoURL);
                                            }

                                            //Finally, add the venue to restaurants array
                                            restaurants.add(new Restaurant(venueName, latitude, longitude, venueCategory, isOpen, photoURL));

                                        }

                                        //After processing each venue separately, update the RecyclerViews adapter
                                        RestaurantsAdapter adapter = new RestaurantsAdapter(context, restaurants, adapterListener, lastKnownLocation);
                                        mRecyclerView.swapAdapter(adapter, true);
                                    } else {
                                        //JSON request failed
                                        Log.e("Error", "Foursquare not returning code:200 in JSON Response");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e("Error", "Most likely a JSONObject or Array was not found");
                                }
                            }

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Error", "No response from foursquare");
                        }
                    });


        //Check for internet connectivity
        boolean connected = false;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Error", "Problem checking for internet connection");
        }

        // If we have access to the internet then add the request ot the volley queue
        // if we don't have internet access, then retrieve cached data from database
        if (connected)
        {
            VolleySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
        }
        else
        {
            //Check if the database actually has data in it already
            if (Database.getInstance(context).isInitialized()) {
                restaurants = Database.getInstance(context).getVenues();
                adapter = new RestaurantsAdapter(context, restaurants, adapterListener, lastKnownLocation);
                mRecyclerView.swapAdapter(adapter, true);
            }
        }
    }


    /**
     * Converts Foursquare's photo JSONObject into a usable URL
     * @param photo Foursquare's photo JSONObject
     * @return URL of photo
     */
    private String photoJSONToURL(JSONObject photo)
    {
        String prefix = null;
        try {
            prefix = photo.getString("prefix").replace("\\", "");
            String suffix = photo.getString("suffix").replace("\\", "");
            String width = photo.getString("width");
            String height = photo.getString("height");

            return prefix + width + "x" + height + suffix;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
