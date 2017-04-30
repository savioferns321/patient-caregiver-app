package com.example.savioubuntu.patientgeofenceapplication.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.savioubuntu.patientgeofenceapplication.R;
import com.example.savioubuntu.patientgeofenceapplication.error.GeofenceErrorMessages;
import com.example.savioubuntu.patientgeofenceapplication.location.LocationTracker;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GeofenceTransitionsIntentService extends IntentService implements LocationListener,
        LocationTracker.LocationCallback{

    private final String TAG = getClass().getSimpleName();

    private LocationTracker locationTracker;
    private Geocoder geocoder;

    private volatile boolean isExited = false;

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationTracker = new LocationTracker(getApplicationContext(), this);
        geocoder = new Geocoder(getApplicationContext());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                String errorMessage = GeofenceErrorMessages.getErrorString(this,
                        geofencingEvent.getErrorCode());
                Log.e(TAG, errorMessage);
                return;
            }

            // Get the transition type.
            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this, geofenceTransition, triggeringGeofences);
            switch (geofencingEvent.getGeofenceTransition()){
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    // TODO: 4/28/17 Notify server that patient has entered geo-fence.
                    Log.i(TAG, geofenceTransitionDetails);
                    locationTracker.disconnect();
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    // TODO: 4/28/17 Send periodic messages to the server.
                    isExited= true;
                    locationTracker.connect();

                    Log.i(TAG, geofenceTransitionDetails);
                    break;
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    break;
                default:
                    //Error
                    Log.e(TAG, getString(R.string.geofence_transition_invalid_type,
                            geofenceTransition));
                    break;
            }
        }
    }

/*    private Location getCurrentLocation(){
        if(locationManager == null){
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
        // Register the listener with the Location Manager to receive location updates
        *//*locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,)  (LocationManager.NETWORK_PROVIDER, 0, 0,
                getApplicationContext());*//*
    }*/

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param context               The app context.
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList =
                null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            triggeringGeofencesIdsList = triggeringGeofences.stream().map(Geofence::getRequestId)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed from IntentService "+location.getLatitude()
                +" "+location.getLongitude());
    }

    @Override
    public void handleNewLocation(Location location) {

        try {
            Address address = geocoder.getFromLocation
                    (location.getLatitude(), location.getLongitude(), 1 ).get(0);
            Log.d(TAG, String.format("Last reported location of patient is %s %s : %s" ,
                    location.getLongitude(), location.getLongitude(), address.getAddressLine(0)));
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(getApplicationContext(),
                    String.format("Last reported location of patient is %s : (%s, %s)",
                            address.getAddressLine(0),
                            location.getLatitude(), location.getLongitude()),
                    Toast.LENGTH_LONG).show());

            //TODO Make a server call to store last location of patient.

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

}

