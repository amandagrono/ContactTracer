package edu.temple.contacttracer;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class MyLocationService extends Service {

    LocationManager locationManager;
    LocationListener locationListener;
    BroadcastReceiver broadcastReceiver;

    Location lastLocation;

    SharedPreferences sharedPreferences;
    public UUIDContainer uuidContainer;
    LocationContainer locationContainer;

    private int tracingTime;
    private final int LOCATION_UPDATE_DISTANCE = 10;

    public MyLocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();


        tracingTime = 10;

        uuidContainer = UUIDContainer.getUUIDContainer(this);
        locationContainer = LocationContainer.getLocationContainer(this);

        locationManager = getSystemService(LocationManager.class);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (lastLocation != null) {
                    if (location.getTime() - lastLocation.getTime() >= (tracingTime * 1000)) {
                        tracePointDetected(lastLocation.getTime(), location.getTime());
                    }
                }
                lastLocation = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String text = intent.getStringExtra("data");
                Log.d("Broadcast Test", text);
                onBroadcastReceived(text);


            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(MyFirebaseService.INTENT_ACTION));

    }


    private void onBroadcastReceived(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);

            String uuid = jsonObject.getString("uuid");
            double latitude = Double.parseDouble(jsonObject.getString("latitude"));
            double longitude = Double.parseDouble(jsonObject.getString("longitude"));
            long sedentary_begin = Long.parseLong(jsonObject.getString("sedentary_begin"));
            long sedentary_end = Long.parseLong(jsonObject.getString("sedentary_end"));
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(latitude);
            location.setLongitude(longitude);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }


            if(!jsonObject.getString("uuid").equals(uuidContainer.getCurrentUUID().getUuid().toString())){
                Log.d("UUID filter test","UUID filter works");


                //Log.d("Distance", String.valueOf(location.distanceTo(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))));
                //changed the distanceTo Comparisons to 50 to test and make sure it worked.
                if(lastLocation == null){
                    if(location.distanceTo(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)) <= 50){
                        Log.d("Location Save", "Distance To Function Called");
                        saveData(uuid, latitude, longitude, sedentary_begin, sedentary_end);
                    }
                }
                else{
                    if(location.distanceTo(lastLocation) <= 50){
                        //save data of location
                        Log.d("Location Save", "Distance To Function Called");
                        saveData(uuid, latitude, longitude, sedentary_begin, sedentary_end);
                    }
                }



            }


        }
        catch (JSONException e){
            e.printStackTrace();
        }


    }


    private void saveData(String uuid, double latitude, double longitude, long sedentary_begin, long sedentary_end){
        MyLocation myLocation = new MyLocation(uuid, latitude, longitude, sedentary_begin, sedentary_end);
        Log.d("Location Save", "Save Data Called");
        locationContainer.addLocation(myLocation);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // GPS is the only really useful provider here, since we need
            // high fidelity meter-level accuracy
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    0,
                    LOCATION_UPDATE_DISTANCE,
                    locationListener);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this, "default")
                .setContentTitle("Contact Tracing Active")
                .setContentText("Click to change app settings")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    /** Should be called when a user has loitered in a location for
     * longer than the designated time.
     */
    private void tracePointDetected(long began, long stopped)  {
        String message = lastLocation.getLatitude() + " - " + lastLocation.getLongitude() + " at " + lastLocation.getTime();
        Log.i("Trace Data", message);

        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "https://kamorris.com/lab/ct_tracking.php";

        StringRequest request = new StringRequest(Request.Method.POST, url, response -> {
            if(response.contains("OK")) Log.d("API","Successfully sent location to server");
            else{
                Log.d("API", "Failed to send location to remote server");
            }
        }, error -> {
            Log.d("API", "ERROR failed to send location to remote server");
            Log.d("API ERROR", error.toString());
        }){
            @Override
            protected Map<String, String> getParams() {

                return new HashMap<String, String>(){{
                    put("uuid", uuidContainer.getCurrentUUID().getUuid().toString());
                    put("latitude", String.valueOf(lastLocation.getLatitude()));
                    put("longitude", String.valueOf(lastLocation.getLongitude()));
                    put("sedentary_begin", String.valueOf(began));
                    put("sedentary_end", String.valueOf(stopped));
                }};
            }

            @Override
            public Map<String, String> getHeaders() {
                return new HashMap<String, String>(){{
                    put("Content-Type", "application/x-www-form-urlencoded");
                }};
            }
        };
        queue.add(request);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        // Memory leaks are bad, m'kay?
        locationManager.removeUpdates(locationListener);
    }
}