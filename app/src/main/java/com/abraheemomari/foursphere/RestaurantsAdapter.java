package com.abraheemomari.foursphere;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.abraheemomari.foursphere.R;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Binds the restaurant data to views which can be retrieved by the RecyclerView
 */
public class RestaurantsAdapter extends
        RecyclerView.Adapter<RestaurantsAdapter.ViewHolder> {

    public interface restaurantAdapterListener {
        public void restaurantSelected(Restaurant restaurant);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public NetworkImageView imageView;
        public TextView nameTextView;
        public TextView distanceTextView;
        public TextView categoryTextView;

        public TextView isOpenTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (NetworkImageView) itemView.findViewById(R.id.imageView);

            nameTextView = (TextView) itemView.findViewById(R.id.text_name);
            categoryTextView = (TextView) itemView.findViewById(R.id.text_distance);
            distanceTextView = (TextView) itemView.findViewById(R.id.text_category);

            isOpenTextView = (TextView) itemView.findViewById(R.id.text_isOpen);
        }
    }

    private Context context;
    private ArrayList<Restaurant> restaurants;
    private LocationManager locationManager;
    private Location lastKnownLocation;
    private restaurantAdapterListener listener;


    public RestaurantsAdapter(Context context, ArrayList<Restaurant> restaurants, restaurantAdapterListener listener, Location lastKnownLocation) {

        this.context = context;
        this.restaurants = restaurants;
        this.listener = listener;
        this.lastKnownLocation = lastKnownLocation;


        //Get distance of each restaurant from the user in meters and convert it to miles
        for (Restaurant restaurant : restaurants)
        {
            Location loc1 = new Location("");

            loc1.setLatitude(lastKnownLocation.getLatitude());
            loc1.setLongitude(lastKnownLocation.getLongitude());

            Location loc2 = new Location("");
            loc2.setLatitude(restaurant.getLatitude());
            loc2.setLongitude(restaurant.getLongitude());

            float distanceInMeters = loc1.distanceTo(loc2);
            float miles = distanceInMeters / 1609;
            restaurant.setDistance((double) miles);

        }

        //Sort be distance (nearest first)
        Collections.sort(restaurants);


    }


    @Override
    public RestaurantsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View restaurantView = inflater.inflate(R.layout.item_restaurant, parent, false);

        ViewHolder viewHolder = new ViewHolder(restaurantView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RestaurantsAdapter.ViewHolder viewHolder, final int position) {
        final Restaurant restaurant = restaurants.get(position);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.restaurantSelected(restaurant);
            }
        });

        //Retrieve image from photo's url
        ImageLoader imageLoader = VolleySingleton.getInstance(context).getImageLoader();
        NetworkImageView imageView = viewHolder.imageView;
        imageView.setImageUrl(restaurant.getPhotoURL(), imageLoader);

        TextView textView = viewHolder.nameTextView;
        textView.setText(restaurant.getName());

        textView = viewHolder.distanceTextView;
        textView.setText(String.valueOf((double)Math.round((restaurant.getDistance()) * 10d) / 10d) + " miles away");

        textView = viewHolder.categoryTextView;
        textView.setText(restaurant.getCategory());

        textView = viewHolder.isOpenTextView;
        textView.setText(restaurant.isOpen());

        if (restaurant.isOpen().equals("OPEN")) {
            //Set color to green
            textView.setTextColor(Color.parseColor("#00CC00"));
        }
        else if (restaurant.isOpen().equals("CLOSED"))
        {
            //Set color to red
            textView.setTextColor(Color.parseColor("#E50000"));
        }

    }

    /**
     * @return Number of restaurants
     */
    @Override
    public int getItemCount() {
        return restaurants.size();
    }



}