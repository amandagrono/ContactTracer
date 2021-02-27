package edu.temple.contacttracer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Calendar;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements DashboardFragment.FragmentInteractionInterface {

    FragmentManager fm;
    Intent serviceIntent;
    UUIDContainer uuidContainer;

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
}