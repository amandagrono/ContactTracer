package edu.temple.contacttracer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements DashboardFragment.FragmentInteractionInterface {

    FragmentManager fm;
    Intent serviceIntent;
    public UUIDContainer uuidContainer;
    public LocationContainer locationContainer;
    BroadcastReceiver broadcastReceiver2;

    public static final String NEW_TRACE_EVENT = "new_trace_event";
    public static final String TRACE_TIMESTAMP = "timestamp";
    public static final String TRACE_LOCATION = "location";
    public static final String TRACE_FILTER = "trace_filter";

    public static final String TRACE_CHANNEL = "tracechannel";

    boolean isInForeground;

    NotificationManagerCompat notificationManagerCompat;

    Intent firebaseIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve UUID container from storage
        uuidContainer = UUIDContainer.getUUIDContainer(this);

        // Get today's date with the time set to 12:00 AM
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If no IDs generated today or at all, generate new ID
        if (uuidContainer.getCurrentUUID() == null || uuidContainer.getCurrentUUID().getDate().before(calendar.getTime()))
            uuidContainer.generateUUID();


        // Notification channel created for foreground service
        NotificationChannel defaultChannel = new NotificationChannel("default",
                "Default",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        getSystemService(NotificationManager.class).createNotificationChannel(defaultChannel);


        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        serviceIntent = new Intent(this, MyLocationService.class);
        firebaseIntent = new Intent(this, MyFirebaseService.class);

        fm = getSupportFragmentManager();

        if (fm.findFragmentById(R.id.frameLayout) == null)
            fm.beginTransaction()
                    .add(R.id.frameLayout, new DashboardFragment())
                    .commit();
        startService(firebaseIntent);
        MyFirebaseService.subscribeToTopics();

        broadcastReceiver2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("Broadcast Test 2", "you know just testing this stuff out");
                String text = intent.getStringExtra("data");
                onBroadcastReceivedPositive(text);
            }
        };
        registerReceiver(broadcastReceiver2, new IntentFilter(MyFirebaseService.INTENT_POSITIVE));
        createNotificationChannel();
        notificationManagerCompat = NotificationManagerCompat.from(this);

        Intent intent = getIntent();
        if(intent.hasExtra(TRACE_LOCATION)){
            showTraceFragment((MyLocation) intent.getSerializableExtra(TRACE_LOCATION), intent.getLongExtra(TRACE_TIMESTAMP, 0));
        }


    }

    @Override
    public void startService() {
        startService(serviceIntent);

    }

    @Override
    public void stopService() {
        stopService(serviceIntent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_DENIED){
            Toast.makeText(this, "You must grant Location permission", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void onBroadcastReceivedPositive(String data){

        try{
            JSONObject jsonObject = new JSONObject(data);

            JSONArray jsonArray = jsonObject.getJSONArray("uuids");
            long date = jsonObject.getLong("date");

            // Get own uuids
            ArrayList<MyUUID> myUUIDS = uuidContainer.getUUIDs();
            ArrayList<String> myUUIDStrings = new ArrayList<>();

            for(int i = 0; i < myUUIDS.size(); i++){
                myUUIDStrings.add(myUUIDS.get(i).getUuid().toString());
            }

            //Get uuids from data
            ArrayList<String> otherUUIDs = new ArrayList<>();

            for(int i = 0; i < jsonArray.length(); i++){
                otherUUIDs.add(jsonArray.getString(i));
            }

            //check if uuids match own saved uuids
            Log.d("Current UUID",uuidContainer.getCurrentUUID().getUuid().toString());
            for(int i = 0; i < myUUIDStrings.size(); i++){

                for(int j = 0; j < otherUUIDs.size(); j++){

                    Log.d("My UUID Current: ", myUUIDStrings.get(i));
                    Log.d("Other UUID current: ", otherUUIDs.get(j));

                    if(myUUIDStrings.get(i).equals(otherUUIDs.get(j))){
                        Log.d("Own UUID", "Self report");
                        return;
                    }

                }
            }
            //check if uuids match uuids received from other people's broadcasts
            locationContainer = LocationContainer.getLocationContainer(this);
            ArrayList<MyLocation> savedLocations = locationContainer.getLocations();
            ArrayList<String> savedUUIDs = new ArrayList<>();

            for(int i = 0; i < savedLocations.size(); i++){
                savedUUIDs.add(savedLocations.get(i).getUuid());
            }
            int index = -1;

            for(int i = 0; i < savedUUIDs.size(); i ++){
                if(otherUUIDs.contains(savedUUIDs.get(i))){
                    index = i;
                    break;
                }
            }
            if(index >= 0){
                notifyUser(savedLocations.get(index), date);
            }



        }
        catch (JSONException e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(TRACE_FILTER);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
        isInForeground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        isInForeground = false;
    }


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MyLocation location = intent.getParcelableExtra(TRACE_LOCATION);
            long date = intent.getLongExtra(TRACE_TIMESTAMP, 0);
            showTraceFragment(location, date);

        }
    };

    private void showTraceFragment(MyLocation location, long date){
        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, TraceFragment.newInstance(location, date))
                .addToBackStack(null)
                .commit();
    }

    private void createNotificationChannel(){
        NotificationChannel channel = new NotificationChannel(TRACE_CHANNEL, "Exposure Alerts", NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    public void notifyUser(MyLocation location, long date){
        if(isInForeground = true){
            Intent intent = new Intent(NEW_TRACE_EVENT);
            intent.putExtra(TRACE_TIMESTAMP, date);
            intent.putExtra(TRACE_LOCATION, location);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
        else{
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra(TRACE_TIMESTAMP, date);
            intent.putExtra(TRACE_LOCATION, location);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(this, TRACE_CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Virus Exposure Alert")
                    .setContentText("You may have had contact with someone who tested positive for the Virus")
                    .setContentIntent(pendingIntent)
                    .build();
            notificationManagerCompat.notify(0, notification);
        }
    }
}