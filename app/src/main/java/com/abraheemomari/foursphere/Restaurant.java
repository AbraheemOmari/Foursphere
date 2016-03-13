package com.abraheemomari.foursphere;

public class Restaurant implements Comparable<Restaurant> {
    private String name;
    private double latitude;
    private double longitude;
    private double distance;
    private String category;

    private String isOpen;
    private String photoURL;

    public Restaurant(String name, double latitude, double longitude, String category, String isOpen, String photoURL) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;

        if (isOpen == null)
        {
            isOpen = "";
        }

        if (isOpen.equals("true"))
        {
            isOpen = "OPEN";
        }
        else if (isOpen.equals("false"))
        {
            isOpen = "CLOSED";
        }
        this.isOpen = isOpen;

        this.photoURL = photoURL;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getCategory() {
        return category;
    }


    public String isOpen() {
        return isOpen;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    @Override
    public int compareTo(Restaurant other) {
        return ((Double) this.getDistance()).compareTo(other.getDistance());
    }
}