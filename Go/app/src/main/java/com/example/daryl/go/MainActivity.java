package com.example.daryl.go;

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
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dd.processbutton.iml.ActionProcessButton;
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
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity implements LocationListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private GoogleMap googleMap;
    private TextView uberPriceLabel, uberTimeLabel, lyftPriceLabel, lyftTimeLabel;
    private EditText uberPriceValue, uberTimeValue, lyftPriceValue, lyftTimeValue;
    private RequestQueue requestQueue;
    private LatLng sourceLatLng = null;
    private LatLng destinationLatLng = null;

    private TextView markerText;
    private AutoCompleteTextView sourceAutocomplete;
    private Geocoder geocoder;
    private List<Address> addressMarkerList;
    private LatLng center;

    protected GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private AutoCompleteTextView destinationAutocomplete;
    private static final String PLACETAG = "PlaceAutocomplete";

    private ImageButton uberImageButton;
    private ImageButton lyftImageButton;

    private JSONObject lyftPriceArray;
    private JSONArray lyftDrivers;
    private BigDecimal minimumPrice;
    private BigDecimal pickupFee;
    private BigDecimal perMileFee;
    private BigDecimal perMinuteFee;
    private final BigDecimal trustSafetyFee = new BigDecimal(1.50);
    private Button cheapestButton;
    private Button fastestButton;
    private jsonHelper jsonHelper;

    private Intent uberLaunchIntent;
    private Intent lyftLaunchIntent;

    private ActionProcessButton mainGoButton;
    private FrameLayout mapFrameLayout;
    private SlidingUpPanelLayout slidingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        markerText = (TextView) findViewById(R.id.locationMarkertext);

        uberPriceLabel = (TextView) findViewById(R.id.uberPriceLabel);
        uberTimeLabel = (TextView) findViewById(R.id.uberTimeLabel);
        lyftPriceLabel = (TextView) findViewById(R.id.lyftPriceLabel);
        lyftTimeLabel = (TextView) findViewById(R.id.lyftTimeLabel);

        uberPriceValue = (EditText) findViewById(R.id.uberPriceValue);
        uberTimeValue = (EditText) findViewById(R.id.uberTimeValue);
        lyftPriceValue = (EditText) findViewById(R.id.lyftPriceValue);
        lyftTimeValue = (EditText) findViewById(R.id.lyftTimeValue);

        //Make edittext not editable
        uberPriceValue.setKeyListener(null);
        uberTimeValue.setKeyListener(null);
        lyftPriceValue.setKeyListener(null);
        lyftTimeValue.setKeyListener(null);

        uberImageButton = (ImageButton) findViewById(R.id.uberLogo);
        lyftImageButton = (ImageButton) findViewById(R.id.lyftLogo);
        uberImageButton.setImageResource(R.drawable.uberlogo);
        lyftImageButton.setImageResource(R.drawable.lyftlogo);

        slidingLayout = (SlidingUpPanelLayout) findViewById(R.id.slidingLayout);
        mainGoButton = (ActionProcessButton) findViewById(R.id.mainGoButton);
        mapFrameLayout = (FrameLayout) findViewById(R.id.mapFrameLayout);

        uberLaunchIntent = getPackageManager().getLaunchIntentForPackage("com.ubercab");
        lyftLaunchIntent = getPackageManager().getLaunchIntentForPackage("me.lyft.android");

        uberImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launchUberApp();
            }
        });

        lyftImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launchLyftApp();
            }
        });

        requestQueue = Volley.newRequestQueue(this);

        zoomMapCurrentLocation();

        cheapestButton = (Button) findViewById(R.id.cheapestButton);
        cheapestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO add google maps address validator
                if (sourceAutocomplete.getText().toString().length() > 10 && destinationAutocomplete.getText().toString().length() > 10) {
                    String lyftPrice = lyftPriceValue.getText().toString().substring(1);
                    String uberPrice = uberPriceValue.getText().toString();
                    String[] uberPriceArray = uberPrice.split("-");
                    double uberMedianPrice = Integer.parseInt(uberPriceArray[0].substring(1));
                    if (uberPriceArray.length > 1) {
                        int uberLowPrice = Integer.parseInt(uberPriceArray[0].substring(1));
                        int uberHighPrice = Integer.parseInt(uberPriceArray[1]);
                        uberMedianPrice = (uberLowPrice + uberHighPrice) / 2;
                    }
                    Log.d("Lyft Price", lyftPrice);

                    String lyftTimeString = lyftTimeValue.getText().toString().split(" ")[0];
                    String uberTimeString = uberTimeValue.getText().toString().split(" ")[0];
                    int lyftTime = Integer.parseInt(lyftTimeString);
                    int uberTime = Integer.parseInt(uberTimeString);

                    if (Integer.parseInt(lyftPrice) < uberMedianPrice) {
                        launchLyftApp();
                    } else if (Integer.parseInt(lyftPrice) > uberMedianPrice) {
                        launchUberApp();
                    } else if (Integer.parseInt(lyftPrice) == uberMedianPrice && lyftTime < uberTime) {
                        launchLyftApp();
                    } else if (Integer.parseInt(lyftPrice) == uberMedianPrice && lyftTime > uberTime) {
                        launchUberApp();
                    } else {
                        Snackbar.make(mapFrameLayout, "Price/distance the same for all apps. " +
                                "Click logo to launch either app.", Snackbar.LENGTH_LONG)
                                .setAction("Dismiss", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                    }
                                })
                                .show();
                    }
                } else {
                    Snackbar.make(mapFrameLayout, "Please enter a valid pickup/dropoff address", Snackbar.LENGTH_LONG)
                            .setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            })
                            .show();
                }
            }
        });

        fastestButton = (Button) findViewById(R.id.closestButton);
        fastestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO add google maps address validator
                if (sourceAutocomplete.getText().toString().length() > 10 && destinationAutocomplete.getText().toString().length() > 10) {
                    String lyftPrice = lyftPriceValue.getText().toString().substring(1);
                    String uberPrice = uberPriceValue.getText().toString();
                    String[] uberPriceArray = uberPrice.split("-");
                    double uberMedianPrice = Integer.parseInt(uberPriceArray[0].substring(1));
                    if (uberPriceArray.length > 1) {
                        int uberLowPrice = Integer.parseInt(uberPriceArray[0].substring(1));
                        int uberHighPrice = Integer.parseInt(uberPriceArray[1]);
                        uberMedianPrice = (uberLowPrice + uberHighPrice) / 2;
                    }
                    Log.d("Lyft Price", lyftPrice);

                    String lyftTimeString = lyftTimeValue.getText().toString().split(" ")[0];
                    String uberTimeString = uberTimeValue.getText().toString().split(" ")[0];
                    int lyftTime = Integer.parseInt(lyftTimeString);
                    int uberTime = Integer.parseInt(uberTimeString);

                    if (lyftTime < uberTime) {
                        launchLyftApp();
                    } else if (lyftTime > uberTime) {
                        launchUberApp();
                    } else if (lyftTime == uberTime && Integer.parseInt(lyftPrice) < uberMedianPrice) {
                        launchLyftApp();
                    } else if (lyftTime == uberTime && Integer.parseInt(lyftPrice) > uberMedianPrice) {
                        launchUberApp();
                    } else {
                        Snackbar.make(mapFrameLayout, "Price/distance the same for all apps. " +
                                "Click logo to launch either app.", Snackbar.LENGTH_LONG)
                                .setAction("Dismiss", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                    }
                                })
                                .show();
                    }
                } else {
                    Snackbar.make(mapFrameLayout, "Please enter a valid pickup/dropoff address", Snackbar.LENGTH_SHORT)
                            .setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            })
                            .show();
                }
            }
        });

        //Allow panel to be collapsed when clicking outside panel area
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        jsonHelper = new jsonHelper();

        if (googleApiClient == null) {
            rebuildGoogleApiClient();
        }

        destinationAutocomplete = (AutoCompleteTextView) findViewById(R.id.dropEdit);
        sourceAutocomplete = (AutoCompleteTextView) findViewById(R.id.pickUpEdit);

//      TODO Change latLngBounds to update with current location
//        LatLngBounds latLngBounds =  new LatLngBounds(new LatLng(28.70, -127.50), new LatLng(48.85, -55.90));
//        LatLngBounds latLngBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
        LatLngBounds atlantaLatLngBounds = new LatLngBounds(new LatLng(33.294746, -84.928851), new LatLng(34.435028, -83.604998));
        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, android.R.layout.simple_list_item_1, atlantaLatLngBounds, null);

        sourceAutocomplete.setSelectAllOnFocus(true);
        sourceAutocomplete.setOnItemClickListener(mAutocompleteSourceClickListener);
        sourceAutocomplete.setAdapter(placeAutocompleteAdapter);

        // Showing edittext to show from the start
        sourceAutocomplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == false) {  // lost focus
                    sourceAutocomplete.setSelection(0, 0);
                }
            }
        });

        destinationAutocomplete.setSelectAllOnFocus(true);
        destinationAutocomplete.setOnItemClickListener(mAutocompleteDestinationClickListener);
        destinationAutocomplete.setAdapter(placeAutocompleteAdapter);
        // Showing edittext to show from the start
        destinationAutocomplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == false) {  // lost focus
                    destinationAutocomplete.setSelection(0, 0);
                }
            }
        });

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
//                locationManager.requestLocationUpdates(provider, 120000, 0, this);
            }
        }
    }

    public void closeKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void unFocusTextView() {
        sourceAutocomplete.clearFocus();
        destinationAutocomplete.clearFocus();
    }

    public void getLyftApiResponse(){
        mainGoButton.setProgress(1);
        StringBuilder urlBuilder = new StringBuilder("http://getlassu.com/api/2/lyft?")
                .append("originLat=" + sourceLatLng.latitude)
                .append("&originLng=" + sourceLatLng.longitude);

        String url = new String(urlBuilder);
        Log.d(getClass().getSimpleName(), url);

        Response.ErrorListener responseErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error != null && error.getMessage() != null) {
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
                        uberPriceLabel.setPadding(dpToPixel(15), dpToPixel(20), dpToPixel(2), dpToPixel(0));
                        uberTimeLabel.setPadding(dpToPixel(20), dpToPixel(20), dpToPixel(2), dpToPixel(0));
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
                        String uberXTimeInMinutes = (uberXTimeEstimate < 60) ? "< 1 min." :
                                Math.round(uberXTimeEstimate / 60) + " min.";

                        uberTimeValue.setText(String.valueOf(uberXTimeInMinutes));
                    }
                });
    }

    public void launchUberApp() {
        if (uberLaunchIntent == null) {
            launchPlayStore("com.ubercab");
        } else {
            startActivity(uberLaunchIntent);
        }
    }

    public void launchLyftApp() {
        if (lyftLaunchIntent == null) {
            launchPlayStore("me.lyft.android");
        } else {
            startActivity(lyftLaunchIntent);
        }
    }

    public void launchPlayStore(String appPackageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public int dpToPixel(int dp) {
        //TODO Move scale to global scope
        final float scale = getResources().getDisplayMetrics().density;
        int px = (int) (dp * scale + 0.5f);
        return px;
    }

    //TODO Remove
    public void hidePanel() {
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
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
        Snackbar.make(mapFrameLayout, "Could not connect to Google API Client: Error", Snackbar.LENGTH_SHORT)
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                })
                .show();

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
     private AdapterView.OnItemClickListener mAutocompleteSourceClickListener =
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
                    placeResult.setResultCallback(mUpdatePlaceSourceDetailsCallback);
                }
            };

    private ResultCallback<PlaceBuffer> mUpdatePlaceSourceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(PLACETAG, "Place query did not complete. Error: " + places.getStatus().toString());
                mainGoButton.setProgress(-1);
                return;
            }
            final Place place = places.get(0);
            //Set LatLng object for current location
            sourceLatLng = place.getLatLng();
            Log.d("Source LatLng", sourceLatLng.toString());
            if (destinationLatLng != null) {
                getLyftApiResponse();
                getPrices();
                getTimes();
            }
            //TODO Update next 4 lines to use onLocationChanged() method
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(sourceLatLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            setUpMarker();
            closeKeyboard();
            unFocusTextView();

        }
    };

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteDestinationClickListener =
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
                    placeResult.setResultCallback(mUpdatePlaceDestinationDetailsCallback);
                }
            };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDestinationDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(PLACETAG, "Place query did not complete. Error: " + places.getStatus().toString());
                mainGoButton.setProgress(-1);
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
            unFocusTextView();
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
            String time = (duration < 60) ? "< 1 min." : duration/60L + " min.";
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

            BigDecimal finalPrice;

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

            if (finalPrice.intValue() < 6) {
                finalPrice = new BigDecimal(6);
            }

            Log.d("Final Price", finalPrice.toString());
            //TODO Improve price formatter
            lyftPriceValue.setText("$" + finalPrice.setScale(0, RoundingMode.HALF_UP));
            lyftPriceLabel.setPadding(dpToPixel(15), dpToPixel(20), dpToPixel(2), dpToPixel(0));
            lyftTimeLabel.setPadding(dpToPixel(20), dpToPixel(20), dpToPixel(2), dpToPixel(0));
            mainGoButton.setProgress(100);
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
                sourceAutocomplete.setText(addressMarkerList.get(0).getAddressLine(0) + " " + addressMarkerList.get(0).getAddressLine(1) + " ");
                sourceLatLng = new LatLng(latitude, longitude);
                if (destinationLatLng != null) {
                    getLyftApiResponse();
                    getPrices();
                    getTimes();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}