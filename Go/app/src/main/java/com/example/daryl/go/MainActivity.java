package com.example.daryl.go;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends ActionBarActivity {

    private GoogleMap googleMap;
    private AutoCompleteTextView mSourceAutoComplete;
    private AutoCompleteTextView mDestinationAutoComplete;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mSourceAutoComplete = (AutoCompleteTextView) findViewById(R.id.pickUpEdit);
        mDestinationAutoComplete = (AutoCompleteTextView) findViewById(R.id.dropEdit);

        mSourceAutoComplete.setOnClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               String place = ((TextView) view).getText().toString();
               onItemSelected(place, mSourceAutoComplete);
           }
        });

        mDestinationAutoComplete.setOnClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String place = ((TextView) view).getText().toString();
                onItemSelected(place, mDestinationAutoComplete);
            }
        });

        mSourceAutoComplete.setThreshold(3);
        mDestinationAutoComplete.setThreshold(3);

        mSourceAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                handler.removeCallbacks(null);
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        searchPlaces(s, mSourceAutoComplete);
                    }
                }, 1000);
            }
        });

        mDestinationAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                handler.removeCallbacks(null);
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        searchPlaces(s, mDestinationAutoComplete);
                    }
                }, 1000);
            }
        });

        zoomMapCurrentLocation();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void zoomMapCurrentLocation() {
//      Get location from GPS if it's available
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

//        If Location wasn't found, find the next most accurate place
        if (myLocation == null){
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);

            String provider = locationManager.getBestProvider(criteria, true);
            myLocation = locationManager.getLastKnownLocation(provider);
        }

        if (myLocation != null) {
//          Animate camera to myLocation
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), 15));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()))
                    .zoom(15)
                    .bearing(90)
                    .tilt(40)
                    .build();

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    //TODO Refactor in order to use recommended google places api guidelines
    private void searchPlaces(Editable s, final AutoCompleteTextView view){

    }
}
