package com.example.savioubuntu.patientgeofenceapplication;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.savioubuntu.patientgeofenceapplication.base.App;
import com.example.savioubuntu.patientgeofenceapplication.model.GeoFenceDetail;
import com.example.savioubuntu.patientgeofenceapplication.service.GeofenceTransitionsIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class GeofenceUpdateActivity extends FragmentActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        OnMapReadyCallback{

    private static final int MY_PERMISSIONS_REQUEST_LOCATION_PERMISSION = 8;
    private static final int REQUEST_CHECK_SETTINGS = 33;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private List<Geofence> mGeofences;
    private PendingIntent mGeofencePendingIntent;


    private GeoFenceDetail detail;
    private final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence_update);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
    }


    public void buildGoogleApiClient(){
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        boolean isPermissionEnabled = true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            isPermissionEnabled = getLocationPermissions();
        }
        //Permissions are enabled. So start monitoring the location.
        if(isPermissionEnabled){
            // Turn on location without opening location settings.
            enableLocationServices(this.getBaseContext());
        }
    }

    public boolean getLocationPermissions(){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                // Show a dialog asking the user for permissions.
                try {
                    new AlertDialog.Builder(this)
                            .setTitle("Location Permission Needed")
                            .setMessage("This app needs the Location permission, please accept " +
                                    "to use location functionality")
                            .setPositiveButton("OK", (dialogInterface, i) -> {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION_PERMISSION );
                            })
                            .create()
                            .show();

                } catch (Exception e){
                    Log.e(TAG, e.getMessage());
                }


            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION_PERMISSION);

                // MY_PERMISSIONS_REQUEST_LOCATION_PERMISSION is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return false;
        }
        return true;
    }

    /**
     * Called when a result is received after requesting permissions.
     * @param requestCode The request code submitted for obtaining location.
     * @param permissions Stores the permissions granted.
     * @param grantResults Stores the results of permission requests.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_LOCATION_PERMISSION:

                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //Permission granted
                    Log.d(TAG, "Location permission granted");
                    enableLocationServices(this);
                } else {
                    //Permission denied
                    Log.d(TAG, "Location permission denied");
                }
        }
    }

    private void enableLocationServices(Context context) {

        if(!isLocationEnabled(context)){
            //Location needs to be enabled via a dialog box.
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(10000 / 2);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                    .checkLocationSettings(mGoogleApiClient, builder.build());
            result.setResultCallback(result1 -> {
                final Status status = result1.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the " +
                                "user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(TAG, "Location settings are inadequate, and cannot be fixed " +
                                "here. Dialog not created.");
                        break;
                }
            });
        } else{
            // Location is already ON. Start monitoring.
            startLocationUpdates();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    public void startLocationUpdates(){
        // Research how to implement.
        // Location permissions are enabled and location is switched on.
        try {
            this.mMap.setMyLocationEnabled(true);
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            populateGeoFenceDetail();

        } catch (SecurityException e){
            Log.e(TAG, e.getMessage());
        }
    }


    private void populateGeoFenceDetail(){

        String url = String.format("%s%s", getResources().getString(R.string.service_url),
                getResources().getString(R.string.geo_fence_id));
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        detail = new GeoFenceDetail(response.getString("id"),
                                response.getString("firstName"), response.getString("lastName"),
                                response.getDouble("lat"), response.getDouble("lng"),
                                response.getLong("radius"));

                        setupGeofence();

                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }
                },
                error -> Log.e(TAG, error.getMessage()));
        App.addRequest(request, TAG);
    }


    public void setupGeofence(){
        // GFDetail is now populated. Set up the geo-fence.
        if(mGeofences == null)
            mGeofences = new ArrayList<>();

        mGeofences.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(detail.getId())

                .setCircularRegion(
                        detail.getLat(),
                        detail.getLng(),
                        detail.getRadius()
                )
                .setExpirationDuration(1000 * 60 * 60)
                .setLoiteringDelay(Toast.LENGTH_LONG + 4)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT
                        | Geofence.GEOFENCE_TRANSITION_ENTER)
                .build());

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(status ->
            {
                Log.d(TAG, "Geo-fences have been successfully added.");
                //Add a visual geo-fence on the map
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng detailLatLng = new LatLng(detail.getLat(), detail.getLng());
                markerOptions.position(detailLatLng);
                markerOptions.title("Geo-fence center");
                mMap.addMarker(markerOptions);
                CircleOptions circleOptions = new CircleOptions();
                circleOptions.center(detailLatLng);
                circleOptions.fillColor(0x5500ff00);
                circleOptions.strokeWidth(1);
                circleOptions.radius(detail.getRadius());
                mMap.addCircle(circleOptions);
                CameraPosition position = new CameraPosition.Builder()
                        .target(detailLatLng)
                        .zoom(17).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));

            });

        } catch (SecurityException e){
            Log.e(TAG, e.getMessage());
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL
                | GeofencingRequest.INITIAL_TRIGGER_ENTER
                | GeofencingRequest.INITIAL_TRIGGER_EXIT);
        builder.addGeofences(mGeofences);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, String.format("Connection failed : %s", connectionResult.getErrorMessage()));

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, String.format("Connection suspended : %d", i));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Connection started :");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}
