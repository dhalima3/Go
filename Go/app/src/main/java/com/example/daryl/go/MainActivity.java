package com.example.daryl.go;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.daryl.go.helpers.Money;
import com.example.daryl.go.helpers.PlaceAutocompleteAdapter;
import com.example.daryl.go.helpers.Secrets;
import com.example.daryl.go.helpers.api.UberApiClient;
import com.example.daryl.go.helpers.api.UberCallback;
import com.example.daryl.go.helpers.jsonHelper;
import com.example.daryl.go.helpers.model.PriceEstimate;
import com.example.daryl.go.helpers.model.PriceEstimateList;
import com.example.daryl.go.helpers.model.TimeEstimate;
import com.example.daryl.go.helpers.model.TimeEstimateList;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity implements LocationListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private GoogleMap googleMap;
    private AutoCompleteTextView destinationAutoComplete;
    private TextView uberPriceLabel, uberTimeLabel, lyftPriceLabel, lyftTimeLabel;
    private EditText uberPriceValue, uberTimeValue, lyftPriceValue, lyftTimeValue;
    private Handler handler;
    private ArrayList<String> mAddresses = new ArrayList<String>();
    private RequestQueue requestQueue;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private LatLng sourceLatLng = null;
    private LatLng destinationLatLng = null;

    private TextView markerText;
    private TextView sourceTextView;
    private Geocoder geocoder;
    private List<Address> addressMarkerList;
    private LatLng center;

    protected GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private AutoCompleteTextView destinationAutocomplete2;
    private static final String PLACETAG = "PlaceAutocomplete";

    private ImageButton uberImageButton;
    private ImageButton lyftImageButton;

    private ArrayList<JSONObject> lyftApiList;
    private JSONObject lyftPriceArray;
    private JSONArray lyftDrivers;
    private BigDecimal minimumPrice;
    private BigDecimal pickupFee;
    private BigDecimal perMileFee;
    private BigDecimal perMinuteFee;
    private final BigDecimal trustSafetyFee = new BigDecimal(1.50);
    private Button cheapestButton;
    private Button fastestButton;
    private ArrayList<Long> durationList;
    private jsonHelper jsonHelper;
    private long lyftDuration;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        sourceTextView = (TextView) findViewById(R.id.pickUpEdit);
//        destinationAutoComplete = (AutoCompleteTextView) findViewById(R.id.dropEdit);
        markerText = (TextView) findViewById(R.id.locationMarkertext);

        uberPriceLabel = (TextView) findViewById(R.id.uberPriceLabel);
        uberTimeLabel = (TextView) findViewById(R.id.uberTimeLabel);
        lyftPriceLabel = (TextView) findViewById(R.id.lyftPriceLabel);
        lyftTimeLabel = (TextView) findViewById(R.id.lyftTimeLabel);

        uberPriceValue = (EditText) findViewById(R.id.uberPriceValue);
        uberTimeValue = (EditText) findViewById(R.id.uberTimeValue);
        lyftPriceValue = (EditText) findViewById(R.id.lyftPriceValue);
        lyftTimeValue = (EditText) findViewById(R.id.lyftTimeValue);

        final Intent uberLaunchIntent = getPackageManager().getLaunchIntentForPackage("com.ubercab");
        final Intent lyftLaunchIntent = getPackageManager().getLaunchIntentForPackage("me.lyft.android");

        uberImageButton = (ImageButton) findViewById(R.id.uberLogo);
        lyftImageButton = (ImageButton) findViewById(R.id.lyftLogo);
        uberImageButton.setImageResource(R.drawable.uberlogo);
        lyftImageButton.setImageResource(R.drawable.lyftlogo);
        uberImageButton.setBackground(null);
        lyftImageButton.setBackground(null);

        uberImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(uberLaunchIntent);
            }
        });

        lyftImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(lyftLaunchIntent);
            }
        });

        handler = new Handler();
        requestQueue = Volley.newRequestQueue(this);

        zoomMapCurrentLocation();

        cheapestButton = (Button) findViewById(R.id.cheapestButton);
        cheapestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String lyftPrice = lyftPriceValue.getText().toString().substring(1);
                String uberPrice = uberPriceValue.getText().toString();
                String[] uberPriceArray = uberPrice.split("-");
                int uberLowPrice = Integer.parseInt(uberPriceArray[0].substring(1));
                int uberHighPrice = Integer.parseInt(uberPriceArray[1]);
                double uberMedianPrice = (uberLowPrice+uberHighPrice)/2;
                Log.d("Lyft Price", lyftPrice);
                Log.d("Uber Low Price", Integer.toString(uberLowPrice));
                Log.d("Uber High Price", Integer.toString(uberHighPrice));
                Log.d("Uber Median Price", Double.toString(uberMedianPrice));

                String lyftTimeString = lyftTimeValue.getText().toString().split(" ")[0];
                String uberTimeString = uberTimeValue.getText().toString().split(" ")[0];
                int lyftTime = Integer.parseInt(lyftTimeString);
                int uberTime = Integer.parseInt(uberTimeString);

                if (Integer.parseInt(lyftPrice) < uberMedianPrice) {
                    startActivity(lyftLaunchIntent);
                }
                else if (Integer.parseInt(lyftPrice) > uberMedianPrice) {
                    startActivity(uberLaunchIntent);
                }
                else if (Integer.parseInt(lyftPrice) == uberMedianPrice && lyftTime < uberTime) {
                    startActivity(lyftLaunchIntent);
                }
                else if (Integer.parseInt(lyftPrice) == uberMedianPrice && lyftTime > uberTime) {
                    startActivity(uberLaunchIntent);
                }
            }
        });

        fastestButton = (Button) findViewById(R.id.fastestButton);
        fastestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String lyftPrice = lyftPriceValue.getText().toString().substring(1);
                String uberPrice = uberPriceValue.getText().toString();
                String[] uberPriceArray = uberPrice.split("-");
                int uberLowPrice = Integer.parseInt(uberPriceArray[0].substring(1));
                int uberHighPrice = Integer.parseInt(uberPriceArray[1]);
                double uberMedianPrice = (uberLowPrice+uberHighPrice)/2;
                Log.d("Lyft Price", lyftPrice);
                Log.d("Uber Low Price", Integer.toString(uberLowPrice));
                Log.d("Uber High Price", Integer.toString(uberHighPrice));
                Log.d("Uber Median Price", Double.toString(uberMedianPrice));

                String lyftTimeString = lyftTimeValue.getText().toString().split(" ")[0];
                String uberTimeString = uberTimeValue.getText().toString().split(" ")[0];
                int lyftTime = Integer.parseInt(lyftTimeString);
                int uberTime = Integer.parseInt(uberTimeString);

                if (lyftTime < uberTime) {
                    startActivity(lyftLaunchIntent);
                }
                else if (lyftTime > uberTime) {
                    startActivity(uberLaunchIntent);
                }
                else if (lyftTime == uberTime && Integer.parseInt(lyftPrice) < uberMedianPrice) {
                    startActivity(lyftLaunchIntent);
                }
                else if (lyftTime == uberTime && Integer.parseInt(lyftPrice) > uberMedianPrice) {
                    startActivity(uberLaunchIntent);
                }
            }
        });

        durationList = new ArrayList<Long>();
        jsonHelper = new jsonHelper();

        if (googleApiClient == null) {
            rebuildGoogleApiClient();
        }

        destinationAutocomplete2 = (AutoCompleteTextView) findViewById(R.id.dropEdit2);
        destinationAutocomplete2.setOnItemClickListener(mAutocompleteClickListener);
        //TODO Change latLngBounds to update with current location
//        LatLngBounds latLngBounds =  new LatLngBounds(new LatLng(28.70, -127.50), new LatLng(48.85, -55.90));
//        LatLngBounds latLngBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
          LatLngBounds atlantaLatLngBounds = new LatLngBounds(new LatLng(33.294746, -84.928851), new LatLng(34.435028, -83.604998));
        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, android.R.layout.simple_list_item_1, atlantaLatLngBounds, null);
        destinationAutocomplete2.setAdapter(placeAutocompleteAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
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

    public void getLyftApiResponse(){
        StringBuilder urlBuilder = new StringBuilder("http://getlassu.com/api/2/lyft?")
                .append("originLat=" + sourceLatLng.latitude)
                .append("&originLng=" + sourceLatLng.longitude);

        String url = new String(urlBuilder);
        Log.d(getClass().getSimpleName(), url);

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
                            JSONObject lyftApiResponse = new JSONObject((String) response);
                            parseLyftApiResponse(lyftApiResponse);
                            setLyftTimes();
                            setLyftPrice();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, responseErrorListener);

        requestQueue.add(stringRequest);
    }

    public void parseLyftApiResponse(JSONObject lyftApiResponse) {
//        JSONObject lyftApiResponse = lyftApiList.get(0);
        try {
            lyftPriceArray = lyftApiResponse.getJSONObject("pricing");
            minimumPrice = Money.parse((String) lyftPriceArray.get("minimum"), Locale.US);
            pickupFee = Money.parse((String) lyftPriceArray.get("pickup"), Locale.US);
            perMileFee = Money.parse((String) lyftPriceArray.get("perMile"), Locale.US);
            perMinuteFee = Money.parse((String) lyftPriceArray.get("perMinute"), Locale.US);

            lyftDrivers = lyftApiResponse.getJSONArray("drivers");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    public void setLyftTimes() {
        double driverLatitude, driverLongitude;

        if (lyftDrivers.length() == 0) {
            return;
        }

        float longestDistance = Float.MAX_VALUE;
        float[] distanceArray = new float[1];
        double shortestDriverLatitude = 0;
        double shortestDriverLongitude = 0;

        float distance = 0;

        for (int i = 0; i < lyftDrivers.length(); i++) {
            try {
                JSONObject driver = (JSONObject) lyftDrivers.get(i);
                JSONObject location = driver.getJSONObject("location");
                driverLatitude = location.getDouble("lat");
                driverLongitude = location.getDouble("lng");

                Location.distanceBetween(sourceLatLng.latitude, sourceLatLng.longitude,
                        driverLatitude, driverLongitude, distanceArray);

                distance = distanceArray[0];
                if (distance < longestDistance) {
                    longestDistance = distance;
                    shortestDriverLatitude = driverLatitude;
                    shortestDriverLongitude = driverLongitude;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("Latitude Before", Double.toString(shortestDriverLatitude));
            Log.d("Longitude Before", Double.toString(shortestDriverLongitude));
        }

        new GetDurationTimeAsync(shortestDriverLatitude, shortestDriverLongitude).execute();
    }

    public void setLyftPrice() {
        new GetDurationPriceAsync().execute();
    }

    public long getDuration(double destinationLatitude, double destinationLongitude) {
        long duration = 0;

        StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/distancematrix/json?")
                //TODO replace origin latitude/longitude
                    .append("origins=" + 37.781955 + "," + -122.402367)
                .append("&destinations=" + destinationLatitude + "," + destinationLongitude)
                .append("&key=" + Secrets.PLACES_API_KEY);

        String url = new String(urlBuilder);
        HttpResponse response = null;

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(url));
            response = client.execute(request);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            InputStream inputStream = response.getEntity().getContent();
            String inputStreamString = convertStreamToString(inputStream);
            JSONObject responseDuration = new JSONObject(inputStreamString);

            JSONObject rows = responseDuration.getJSONObject("rows");
            JSONObject elementsArray = rows.getJSONObject("elements");
            JSONObject distanceJSON = elementsArray.getJSONObject("distance");
            JSONObject durationJSON = elementsArray.getJSONObject("duration");
            //TODO Can access distance from this request too (Refractor)
            duration = durationJSON.getLong("value");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return duration;
    }

    public static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        setUpMarker();
    }

    public void setUpMarker() {
        googleMap.clear();

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                center = googleMap.getCameraPosition().target;
                markerText.setText(" Drag to your location ");
                googleMap.clear();
                try {
                    new GetLocationAsync(center.latitude, center.longitude).execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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

    public void getPrices() {
        UberApiClient.getUberV1APIClient().getPriceEstimates(("Token " + Secrets.UBER_SERVER_TOKEN),
                sourceLatLng.latitude,
                sourceLatLng.longitude,
                destinationLatLng.latitude,
                destinationLatLng.longitude,
                new UberCallback<PriceEstimateList>() {
                    @Override
                    public void success(PriceEstimateList priceEstimateList, retrofit.client.Response response) {
                        PriceEstimate uberX = priceEstimateList.getPrices().get(0);
                        String uberXPriceEstimate = uberX.getEstimate();
                        uberPriceValue.setText(uberXPriceEstimate);
                    }
                });
    }

    public void getTimes() {
        UberApiClient.getUberV1APIClient().getTimeEstimates(("Token " + Secrets.UBER_SERVER_TOKEN),
                sourceLatLng.latitude,
                sourceLatLng.longitude,
                new UberCallback<TimeEstimateList>() {
                    @Override
                    public void success(TimeEstimateList timeEstimateList, retrofit.client.Response response) {
                        TimeEstimate uberX = timeEstimateList.getTimes().get(0);
                        double uberXTimeEstimate = (double) uberX.getEstimate();
                        String uberXTimeInMinutes = Math.round(uberXTimeEstimate/60) + " min.";
                        uberTimeValue.setText(String.valueOf(uberXTimeInMinutes));
                    }
                });
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Successfully connected to the API client. Pass it to the adapter to enable API access.
        placeAutocompleteAdapter.setGoogleApiClient(googleApiClient);
        Log.i(PLACETAG, "GoogleApiClient connected.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to the API client has been suspended. Disable API access in the client.
        placeAutocompleteAdapter.setGoogleApiClient(null);
        Log.e(PLACETAG, "GoogleApiClient connection suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e(PLACETAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();

        // Disable API access in the adapter because the client was not initialised correctly.
        placeAutocompleteAdapter.setGoogleApiClient(null);

    }

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
     private AdapterView.OnItemClickListener mAutocompleteClickListener =
            new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final PlaceAutocompleteAdapter.PlaceAutocomplete item = placeAutocompleteAdapter.getItem(position);
                    final String placeId = String.valueOf(item.placeId);
                    Log.i(PLACETAG, "Autocomplete item selected: " + item.description);

                    /*
                     Issue a request to the Places Geo Data API to retrieve a Place object with additional
                      details about the place.
                    */
                    PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                            .getPlaceById(googleApiClient, placeId);
                    placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
                }
            };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(PLACETAG, "Place query did not complete. Error: " + places.getStatus().toString());

                return;
            }
            final Place place = places.get(0);
            //Set LatLng object for current location
            destinationLatLng = place.getLatLng();
            Log.d("Destination LatLng", destinationLatLng.toString());
            getLyftApiResponse();
            getPrices();
            getTimes();
            closeKeyboard();
        }
    };

    protected synchronized void rebuildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and connection failed
        // callbacks should be returned, which Google APIs our app uses and which OAuth 2.0
        // scopes our app requests.
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addConnectionCallbacks(this)
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    public class GetDurationTimeAsync extends AsyncTask<String, Void, Long> {
        double destinationLatitude, destinationLongitude;

        public GetDurationTimeAsync(double destinationLatitude, double destinationLongitude) {
            Log.d("Destination Latitude", Double.toString(destinationLatitude));
            Log.d("Destination Longitude", Double.toString(destinationLongitude));
            this.destinationLatitude = destinationLatitude;
            this.destinationLongitude = destinationLongitude;
        }

        @Override
        protected Long doInBackground(String... params) {
            Log.d("Source Latitude", Double.toString(sourceLatLng.latitude));
            Log.d("Source Longitude", Double.toString(sourceLatLng.longitude));

            StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/distancematrix/json?")
                    //TODO replace origin latitude/longitude
                    .append("origins=" + sourceLatLng.latitude + "," + sourceLatLng.longitude)
                    .append("&destinations=" + destinationLatitude + "," + destinationLongitude)
                    .append("&key=" + Secrets.PLACES_API_KEY);

            String url = new String(urlBuilder);
            JSONObject jsonObject = jsonHelper.getJSONFromURL(url);
            JSONArray rows = null;
            long duration = 1;


            try {
                rows = jsonObject.getJSONArray("rows");
                JSONObject elements = rows.getJSONObject(0);
                JSONArray elementsArray = elements.getJSONArray("elements");
                JSONObject elementsObject = elementsArray.getJSONObject(0);
                JSONObject distanceJSON = elementsObject.getJSONObject("distance");
                JSONObject durationJSON = elementsObject.getJSONObject("duration");
                //TODO Can access distance from this request too (Refractor)
                duration = durationJSON.getInt("value")-1;
                Log.d("Duration", Long.toString(duration));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return duration;

        }

        @Override
        protected void onPostExecute(Long duration) {
            String time = duration/60L + " min.";
            lyftTimeValue.setText(time);
        }
    }

    public class GetDurationPriceAsync extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            Log.d("Price Source Latitude", Double.toString(sourceLatLng.latitude));
            Log.d("Price Source Longitude", Double.toString(sourceLatLng.longitude));

            StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/distancematrix/json?")
                    //TODO replace origin latitude/longitude
                    .append("origins=" + sourceLatLng.latitude + "," + sourceLatLng.longitude)
                    .append("&destinations=" + destinationLatLng.latitude + "," + destinationLatLng.longitude)
                    .append("&key=" + Secrets.PLACES_API_KEY);

            String url = new String(urlBuilder);
            JSONObject jsonObject = jsonHelper.getJSONFromURL(url);
            JSONArray rows = null;
            long duration = 1;
            JSONObject elementsObject = new JSONObject();

            try {
                rows = jsonObject.getJSONArray("rows");
                JSONObject elements = rows.getJSONObject(0);
                JSONArray elementsArray = elements.getJSONArray("elements");
                elementsObject = elementsArray.getJSONObject(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return elementsObject;

        }

        @Override
        protected void onPostExecute(JSONObject elementsObject) {
            float distance = 0;
            long duration = 0;
            float distanceInMiles;
            long durationInMinutes;

            try {
                JSONObject distanceJSON = elementsObject.getJSONObject("distance");
                JSONObject durationJSON = elementsObject.getJSONObject("duration");
                distance = distanceJSON.getLong("value");
                duration = durationJSON.getLong("value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("Distance Before conv", Float.toString(distance));
            distanceInMiles = distance/1000f/1.609344f;
            durationInMinutes = duration/60L;

//            float[] distanceArray = new float[1];
            BigDecimal finalPrice;
//
//            Location.distanceBetween(sourceLatLng.latitude, sourceLatLng.longitude,
//                    destinationLatLng.latitude, destinationLatLng.longitude, distanceArray);
//
//            float distance = distanceArray[0];
            Log.d("Distance Lyft Price", Float.toString(distanceInMiles));
            Log.d("Duration Lyft Price", Long.toString(durationInMinutes));
            Log.d("Pick up fee", pickupFee.toString());
            Log.d("Per mile fee", perMileFee.toString());
            Log.d("Per minute fee", perMinuteFee.toString());
            Log.d("Trust/safety fee", trustSafetyFee.toString());

            finalPrice = pickupFee
                    .add(perMileFee.multiply(new BigDecimal(distanceInMiles)))
                    .add(perMinuteFee.multiply(new BigDecimal(durationInMinutes)))
                    .add(trustSafetyFee);

            Log.d("Final Price", finalPrice.toString());
            //TODO Improve price formatter
            lyftPriceValue.setText("$" + finalPrice.setScale(0, RoundingMode.HALF_UP));
        }
    }

    public class GetLocationAsync extends AsyncTask<String, Void, String> {
        double latitude, longitude;

        public GetLocationAsync(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected String doInBackground(String... params) {

            try {
                geocoder = new Geocoder(MainActivity.this, Locale.ENGLISH);
                addressMarkerList = geocoder.getFromLocation(latitude, longitude, 1);
            } catch (IOException e) {
                Log.e("tag", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                sourceTextView.setText(addressMarkerList.get(0).getAddressLine(0) + " " + addressMarkerList.get(0).getAddressLine(1) + " ");
                sourceLatLng = new LatLng(latitude, longitude);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}