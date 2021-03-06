package edu.temple.contacttracer;

import android.app.DatePickerDialog;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class DashboardFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    FragmentInteractionInterface parent;
    Button startButton, stopButton, reportPositiveButton;
    DatePickerDialog dialog;

    public DashboardFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This is how we let the activity know that we have an ActionBar
        // item that we would like to have displayed
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FragmentInteractionInterface) {
            parent = (FragmentInteractionInterface) context;
        } else {
            throw new RuntimeException("Please implement FragmentInteractionInterface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        startButton = v.findViewById(R.id.startButton);
        stopButton = v.findViewById(R.id.stopButton);
        reportPositiveButton = v.findViewById(R.id.reportPositiveButton);
        Calendar calendar = new GregorianCalendar();
        dialog = new DatePickerDialog(getContext(), this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.setOnDateSetListener(this::onDateSet);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.startService();

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.stopService();
            }
        });

        reportPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });


        return v;
    }

    @Override
    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
        Log.d("Date Dialog", "Dialog");
        sendPositiveReport( getDateFromDatePicker(datePicker));

    }


    public void sendPositiveReport( long date){

        Log.d("API", "Sending positive report to the server");

        RequestQueue queue = Volley.newRequestQueue(getContext());

        String url = "https://kamorris.com/lab/ct_tracing.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, response -> {

            if(response.contains("OK")) Log.d("API","Successfully sent location to server");
            else{
                Log.d("API", "Failed to send location to remote server");
            }

        } , error -> {

            Log.d("API", "ERROR failed to send location to remote server");
            Log.d("API ERROR", error.toString());

        }){
            @Override
            protected Map<String, String> getParams() {
                JSONArray jsonArray = new JSONArray();

                UUIDContainer uuidContainer = UUIDContainer.getUUIDContainer(getContext());
                ArrayList<String> arrayList = convertUUIDListToString(uuidContainer.getUUIDs());

                for(int i = 0; i < arrayList.size(); i ++){
                    jsonArray.put(arrayList.get(i));
                }


                Log.d("API","Date: " + String.valueOf(date) + ", UUIDs: " + jsonArray.toString());

                return new HashMap<String, String>(){{

                    put("date", String.valueOf(date));
                    put("uuids", jsonArray.toString());
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


    ArrayList<String> convertUUIDListToString(ArrayList<MyUUID> arrayList){

        ArrayList<String> convertedUUIDs = new ArrayList<>();

        for(int i = 0; i < arrayList.size(); i ++){
            convertedUUIDs.add(arrayList.get(i).getUuid().toString());
        }
        return convertedUUIDs;

    }

    long getDateFromDatePicker(DatePicker datePicker){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
        calendar.set(Calendar.MONTH, datePicker.getMonth());
        calendar.set(Calendar.YEAR, datePicker.getYear());
        return calendar.getTimeInMillis();
    }

    interface FragmentInteractionInterface {
        void startService();
        void stopService();
    }
}