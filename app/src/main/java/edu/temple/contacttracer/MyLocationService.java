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
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

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


public class MyLocationService extends Service {

    LocationManager locationManager;
    LocationListener locationListener;
    BroadcastReceiver broadcastReceiver;

    Location lastLocation;
    SharedPreferences sharedPreferences;

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


        tracingTime = 60;


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

            public void onStatusChanged(String provider, int status, Bundle extras) { }
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String text = intent.getStringExtra("data");
                Log.d("Broadcast Test", text);
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(MyFirebaseService.INTENT_ACTION));

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

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("uuid", UUIDContainer.getUUIDContainer(this).getCurrentUUID().getUuid());
            jsonBody.put("latitude", lastLocation.getLatitude());
            jsonBody.put("longitude", lastLocation.getLongitude());
            jsonBody.put("sedentary_start", began);
            jsonBody.put("sedentary_stop", stopped);

            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d("POST", "Post response given: " + response);
                }
            },new Response.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){

                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try{
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException e) {
                        VolleyLog.wtf("Unsupported Encoding");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if(response != null){
                        responseString = String.valueOf(response.statusCode);

                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            queue.add(stringRequest);
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        // Memory leaks are bad, m'kay?
        locationManager.removeUpdates(locationListener);
    }
}