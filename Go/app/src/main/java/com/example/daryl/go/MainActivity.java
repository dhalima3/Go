package com.example.daryl.go;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.daryl.go.helpers.Secrets;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.victorsima.uber.UberClient;
import com.victorsima.uber.model.Price;
import com.victorsima.uber.model.Prices;
import com.victorsima.uber.model.Time;
import com.victorsima.uber.model.Times;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit.RestAdapter;

public class MainActivity extends ActionBarActivity implements LocationListener {

    private GoogleMap googleMap;
    private AutoCompleteTextView sourceAutoComplete, destinationAutoComplete;
    private TextView uberPriceLabel, uberTimeLabel, lyftPriceLabel, lyftTimeLabel;
    private EditText uberPriceValue, uberTimeValue, lyftPriceValue, lyftTimeValue;
    private Button submitButton;
    private Handler handler;
    private ArrayList<String> mAddresses = new ArrayList<String>();
    private RequestQueue requestQueue;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private LatLng sourceLatLng = null;
    private LatLng destinationLatLng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        sourceAutoComplete = (AutoCompleteTextView) findViewById(R.id.pickUpEdit);
        destinationAutoComplete = (AutoCompleteTextView) findViewById(R.id.dropEdit);

        uberPriceLabel = (TextView) findViewById(R.id.uberPriceLabel);
        uberTimeLabel = (TextView) findViewById(R.id.uberTimeLabel);
        lyftPriceLabel = (TextView) findViewById(R.id.lyftPriceLabel);
        lyftTimeLabel = (TextView) findViewById(R.id.lyftTimeLabel);

        uberPriceValue = (EditText) findViewById(R.id.uberPriceValue);
        uberTimeValue = (EditText) findViewById(R.id.uberTimeValue);
        lyftPriceValue = (EditText) findViewById(R.id.lyftPriceValue);
        lyftTimeValue = (EditText) findViewById(R.id.lyftTimeValue);

        submitButton = (Button) findViewById(R.id.submitButton);

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getData();
            }
        });

        handler = new Handler();
        requestQueue = Volley.newRequestQueue(this);

        sourceAutoComplete.setThreshold(3);
        destinationAutoComplete.setThreshold(3);

        zoomMapCurrentLocation();

        if (destinationLatLng != null) {
            Log.d("Latitude", Double.toString(destinationLatLng.latitude));
            Log.d("Longitude", Double.toString(destinationLatLng.longitude));
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        sourceAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String place = ((TextView) view).getText().toString();
                onItemSelected(place, sourceAutoComplete);
            }
        });

        destinationAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String place = ((TextView) view).getText().toString();
                onItemSelected(place, destinationAutoComplete);
            }
        });

        sourceAutoComplete.addTextChangedListener(new TextWatcher() {
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
                        searchPlaces(s, sourceAutoComplete);
                    }
                }, 1000);
            }
        });

        destinationAutoComplete.addTextChangedListener(new TextWatcher() {
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
                        searchPlaces(s, destinationAutoComplete);
                    }
                }, 1000);
            }
        });
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
        if (googleMap == null) {
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

//        Check to see that map was obtained successfully
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
                googleMap.setTrafficEnabled(true);
                googleMap.setBuildingsEnabled(true);
                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, true);
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    onLocationChanged(location);
                }
                locationManager.requestLocationUpdates(provider, 120000, 0, this);
            }
        }
    }

    public void closeKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void onItemSelected(String place, AutoCompleteTextView view) {
        closeKeyboard();
        view.clearListSelection();
        view.dismissDropDown();

        Log.d("Place", place);
        view.clearFocus();
        Geocoder geocoder = new Geocoder(this);
        List<Address> addressList = null;
        try {
            addressList = geocoder.getFromLocationName(place, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (addressList.size() > 0) {
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));

            if (view.getId() == R.id.pickUpEdit) {
                if (sourceMarker != null) {
                    sourceMarker.remove();
                }
                sourceMarker = googleMap.addMarker(new MarkerOptions().title("Pickup -" + place).position(latLng));
                sourceLatLng = latLng;
                uberPriceValue.setText(Double.toString(latLng.latitude));
                uberTimeValue.setText(Double.toString(latLng.longitude));
            } else {
                if (destinationMarker != null) {
                    destinationMarker.remove();
                }
                destinationMarker = googleMap.addMarker(new MarkerOptions().title("Destination - " + place).position(latLng));
                destinationLatLng = latLng;
                lyftPriceValue.setText(Double.toString(latLng.latitude));
                lyftTimeValue.setText(Double.toString(latLng.longitude));
//                TODO Change when getUberTime is activated
//                getUberPrice(latLng);
            }
        }
    }

    //TODO Refactor in order to use recommended google places api guidelines
    private void searchPlaces(Editable s, final AutoCompleteTextView view) {
        String inputQuery = s.toString();
        if (inputQuery.isEmpty()) {
            return;
        }
        StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/autocomplete/json?input=")
                .append(Uri.encode(inputQuery))
                .append("&key=" + Secrets.PLACES_API_KEY)
                .append("&location=")
                .append(googleMap.getMyLocation().getLatitude() + "," + googleMap.getMyLocation().getLongitude());

        String url = new String(urlBuilder);
        Log.d(getClass().getSimpleName(), url);

//        TODO: Use Retrofit instead of Volley
//        Request a string response from the provided URL

        Response.ErrorListener responseErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error != null) {
                    Log.d("Error Response", error.getMessage());
                }
            }
        };

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener() {

                    @Override
                    public void onResponse(Object response) {
                        try {
                            JSONObject responsePlaces = new JSONObject((String) response);
                            JSONArray predictionsArray = responsePlaces.getJSONArray("predictions");
                            mAddresses.clear();
                            for (int i = 0; i < predictionsArray.length(); i++) {
                                JSONObject predictionObject = predictionsArray.getJSONObject(i);
                                String description = predictionObject.getString("description");
                                mAddresses.add(description);
                            }
//                            TODO: Multiple adapters?
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, mAddresses);
                            view.setAdapter(adapter);
//                            view.showDropDown();
                            Log.d(getClass().getSimpleName(), mAddresses.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, responseErrorListener);

        requestQueue.add(stringRequest);
    }

    public void getData() {
        double sourceLatitude = sourceLatLng.latitude;
        double sourceLongitude = sourceLatLng.longitude;
        double destinationLatitude = destinationLatLng.latitude;
        double destinationLongitude = destinationLatLng.longitude;
        UberClient uberClient = new UberClient(Secrets.UBER_SERVER_TOKEN, RestAdapter.LogLevel.BASIC);
        Prices prices = uberClient.getApiService().getPriceEstimates(sourceLatitude, sourceLongitude, destinationLatitude, destinationLongitude);

//        List<Price> uberPriceList = getUberPriceList(uberClient, sourceLatitude, sourceLongitude, destinationLatitude, destinationLongitude);
//        Log.d("Price List", uberPriceList.toString());
        List<Time> uberTimeList = getUberTimeList(uberClient, sourceLatitude, sourceLongitude);
        double lyftPrice = getLyftPrice(sourceLatitude, sourceLongitude, destinationLatitude, destinationLongitude);
    }

    public List<Price> getUberPriceList(UberClient uberClient, double sourceLatitude, double sourceLongitude, double destinationLatitude, double destinationLongitude) {

        Prices prices = uberClient.getApiService().getPriceEstimates(sourceLatitude, sourceLongitude, destinationLatitude, destinationLongitude);
        List<Price> priceList = prices.getPrices();
        return priceList;
    }

    public List<Time> getUberTimeList(UberClient uberClient, double sourceLatitude, double sourceLongitude) {

        Times times = uberClient.getApiService().getTimeEstimates(sourceLatitude, sourceLongitude, null, null);
        List<Time> timeList = times.getTimes();
        return timeList;
    }

//    TODO This is a backup for the API call.  Need to implement for Lyft Plus
    public double getLyftPrice(double sourceLatitude, double sourceLongitude, double destinationLatitude, double destinationLongitude){
        double baseCharge = 1.35;
        double buffer = .75;
        double costPerMile = 1.29;
        double costPerMinute = .17;
        double trustSafetyFee = 1.50;
        double finalPrice = 0;

//        finalPrice =

        if (finalPrice < 6.00) {
            finalPrice = 6.00;
        }

        return finalPrice;
    }

//    TODO Implement for java
    public double getLyftTime(double sourceLatitude, double sourceLongitude) { return 0; }



    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
