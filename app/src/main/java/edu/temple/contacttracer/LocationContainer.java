package edu.temple.contacttracer;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class LocationContainer implements Serializable {

    private final String LOCATION_FILE = "saved_locations";



    transient private Context context;
    private ArrayList<MyLocation> locations;
    private ArrayList<MyLocation> userLocations;

    private LocationContainer(Context context){
        locations = new ArrayList<>();
        userLocations = new ArrayList<>();

        this.context = context;

        LocationContainer locationContainer = null;

        FileInputStream fileInputStream = null;

        try{
            fileInputStream = context.openFileInput(LOCATION_FILE);
            ObjectInputStream is = new ObjectInputStream(fileInputStream);
            locationContainer = (LocationContainer) is.readObject();
            is.close();
            fileInputStream.close();
        }
        catch (Exception e ){
            e.printStackTrace();
        }

        if(locationContainer == null){
            locations = new ArrayList<>();
            userLocations = new ArrayList<>();
        }
        else{
            locations.addAll(locationContainer.getLocations());
            userLocations.addAll(locationContainer.getUserLocations());
        }
        removeExpiredLocations();
        Log.d("Total Locations", String.valueOf(locations.size()));
    }
    public void addLocation(MyLocation location){
        locations.add(location);
        saveLocations();
    }
    public void addUserLocation(MyLocation location){
        userLocations.add(location);
    }

    private ArrayList<MyLocation> getLocations(){
        return locations;
    }
    private ArrayList<MyLocation> getUserLocations(){
        return userLocations;
    }

    private void removeExpiredLocations(){
        long TWO_WEEKS = 1209600000;
        Date twoWeeksAgo = new Date((new Date()).getTime() - TWO_WEEKS);
        for(MyLocation location : locations){
            if(new Date(location.getSedentary_end()).before(twoWeeksAgo)){
                locations.remove(location);
            }
        }
        saveLocations();
    }

    public static LocationContainer getLocationContainer(Context context){
        return new LocationContainer(context);
    }

    private void saveLocations(){
        FileOutputStream fileOutputStream = null;
        try{
            fileOutputStream = context.openFileOutput(LOCATION_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
            fileOutputStream.close();
            Log.d("Location Saved", "Location data saved");
        }
        catch (Exception e ){
            e.printStackTrace();
        }
    }

}
