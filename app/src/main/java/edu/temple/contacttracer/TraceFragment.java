package edu.temple.contacttracer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class TraceFragment extends Fragment {

    private MyLocation location;
    private long date;
    private MapView mapView;


    public TraceFragment() {
        // Required empty public constructor
    }


    public static TraceFragment newInstance(MyLocation location, long date) {
        TraceFragment fragment = new TraceFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("location", location);
        bundle.putLong("date", date);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            location = (MyLocation) getArguments().getSerializable("location");
            date = getArguments().getLong("date");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trace, container, false);

        Button okButton = view.findViewById(R.id.okButton);
        okButton.setOnClickListener(view1 -> {
            getParentFragment().getFragmentManager().popBackStack();
        });

        Date startDate = new Date(location.getSedentary_begin());
        Date endDate = new Date(location.getSedentary_end());

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        DateFormat timeFormat = new SimpleDateFormat("h:mm", Locale.US);

        TextView date = view.findViewById(R.id.date);
        date.setText(String.format("%s - %s", dateFormat.format(startDate), dateFormat.format(endDate)));

        TextView time = view.findViewById(R.id.time);
        time.setText(String.format("%s - %s", timeFormat.format(startDate), timeFormat.format(endDate)));

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();


        mapView.getMapAsync(map ->{
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 20);
            map.addMarker(new MarkerOptions().position(latLng).title("Contact Point"));
            map.animateCamera(cameraUpdate);
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}