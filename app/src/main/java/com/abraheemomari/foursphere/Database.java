package com.abraheemomari.foursphere;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {

    private static Database DBInstance;

    private static final String DATABASE_NAME = "Foursphere.db";

    private static final int DATABASE_VERSION = 1;

    private static final String VENUES_CREATE = "CREATE TABLE IF NOT EXISTS VENUES " +
            "(venue_id   TEXT    PRIMARY KEY, " +
            " venue_name   TEXT    NOT NULL, " +
            " venue_category  TEXT    NOT NULL, " +
            " latitude     REAL    NOT NULL, " +
            " longitude    REAL    NOT NULL, " +
            " photo_url    TEXT);";


    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized Database getInstance(Context context) {
        if (DBInstance == null) {
            DBInstance = new Database(context);
        }
        return DBInstance;
    }


    //Creates venues table if it doesn't already exist
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(VENUES_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Checks if there are any entries in the database
     * @return true if the database has data, false otherwise
     */
    public boolean isInitialized()
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("Select * from VENUES", null);

        if(cursor != null)
        {
            if(cursor.getCount() <= 0){
                return false;
            }
            else{
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a restaurant to the venu table of the database
     */
    public void addVenue(String venueId, String venueName, String venueCategory, double latitude, double longitude,
                         String photoUrl)
    {
        String stmt = "INSERT INTO VENUES VALUES(?, ?, ?, ?, ?, ?);";

        SQLiteDatabase db = this.getWritableDatabase();
        Object[] args = new Object[] {venueId, venueName, venueCategory, latitude, longitude, photoUrl};
        db.execSQL(stmt, args);

    }

    /**
     * Checks if the restaurant is already in the database
     * @return true if the restaurant already exists, false otherwise
     */
    public boolean venueExists(String venueId){
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = new String[]{venueId};
        Cursor result =  db.rawQuery( "SELECT * FROM VENUES WHERE venue_id = ?;", args);

        result.moveToFirst();
        result.getCount();

        if (result.getCount() != 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Gets all the restaurants available in the database
     * @return ArrayList of Restaurant objects for every restaurant in the database
     */
    public ArrayList<Restaurant> getVenues(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result =  db.rawQuery( "SELECT * FROM VENUES;", null);

        result.moveToFirst();
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        String venueName = "";
        String venueCategory = "";
        double latitude = 0;
        double longitude = 0;
        String photoUrl = "";

        for (int i = 0; i < result.getCount(); ++i)
        {
            venueName = result.getString(result.getColumnIndex("venue_name"));
            venueCategory = result.getString(result.getColumnIndex("venue_category"));
            latitude = result.getDouble(result.getColumnIndex("latitude"));
            longitude = result.getDouble(result.getColumnIndex("longitude"));
            photoUrl = result.getString(result.getColumnIndex("photo_url"));

            restaurants.add(new Restaurant(venueName, latitude, longitude, venueCategory, "", photoUrl));

            if (!result.isLast())
            {
                result.moveToNext();
            }
        }

        if(result != null) {
            result.close();
        }
        return restaurants;
    }
}