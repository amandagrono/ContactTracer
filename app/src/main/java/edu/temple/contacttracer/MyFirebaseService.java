package edu.temple.contacttracer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

//import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class MyFirebaseService extends FirebaseMessagingService {

    public static final String INTENT_ACTION = "edu.temple.contacttracer.INTENT_ACTION";
    private NotificationManagerCompat notificationManagerCompat;


    public MyFirebaseService() {
    }

    public static void subscribeToTopics(){
        subscribeToTopic("TRACKING");
    }
    private static void subscribeToTopic(String topic){
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(
                task -> Log.d("Firebase", "Successfully Subscribed to topic: " + topic)
        ).addOnFailureListener(
                task -> Log.d("Firebase" , "Failed to subscribe to topic: " + topic)
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        subscribeToTopics();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);



        String data = remoteMessage.getData().get("payload");
        Intent firebaseMessage = new Intent();
        firebaseMessage.setAction(MyFirebaseService.INTENT_ACTION);
        firebaseMessage.putExtra("data", data);
        sendBroadcast(firebaseMessage);


    }
}