package com.shlominet.myplaces01;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import com.shlominet.utils.CustomInfoWindowAdapter;
import com.shlominet.utils.DirectionsJSONParser;
import com.shlominet.utils.MapPointObj;
import com.shlominet.utils.MyInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private MapPointObj pointObj;
    private ArrayList<MapPointObj> pointObjList;
    private GoogleMap mMap;
    private Marker myMarker;
    private Polyline polyline;

    private Spinner spinnerCat;

    private boolean flagCallGetLatLng = false;
    private double lat, lng;

    private Button mLocBtn;


    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //spinner for map type (normal, satellite, terrain, hybrid)
        spinnerCat = view.findViewById(R.id.map_spinner_cat);
        spinnerCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(mMap == null) return;
                switch (position) {
                    case 0:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                    case 1:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case 2:
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        break;
                    case 3:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                    default:
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //set point on map with data from main activity
        Bundle bundle = getArguments();
        if (bundle != null) {
            pointObj = bundle.getParcelable("mapPointObj");
            pointObjList = bundle.getParcelableArrayList("mapPointObjList");
        }

        //"my location button"
        mLocBtn = view.findViewById(R.id.location_btn);
        mLocBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flagCallGetLatLng = true;
                final MyInterface.FragHelper mainHelper = (MyInterface.FragHelper)getActivity();
                mainHelper.getGpsLocation();
                //goToLocation() will be called after main activity return result
            }
        });

        //get last location from previous time we opened the app
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if(sharedPref.getFloat("lastLat", 0) != 0) {
            lat = sharedPref.getFloat("lastLat", 0);
            lng = sharedPref.getFloat("lastLng", 0);
        }

        return view;
    }

    //make path (on foot) from "my-location" to "selected location"
    public void googleMapsPath(LatLng origin, LatLng dest) {

        String url = getDirectionsUrl(origin,dest);
        Ion.with(getActivity())
                .load(url)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if(result == null) {
                            Toast.makeText(getActivity(), "No Internet Connection", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        JSONObject jObject;
                        List<List<HashMap<String, String>>> routes = null;
                        try {
                            jObject = new JSONObject(result);

                            // Starts parsing data with helper class
                            DirectionsJSONParser parser = new DirectionsJSONParser();
                            routes = parser.parse(jObject);

                            ArrayList<LatLng> points;
                            PolylineOptions lineOptions = null;

                            // Traversing through all the routes
                            for (int i = 0; i < routes.size(); i++) {
                                points = new ArrayList<>();
                                lineOptions = new PolylineOptions();

                                // Fetching i-th route
                                List<HashMap<String, String>> path = routes.get(i);

                                // Fetching all the points in i-th route
                                for (int j = 0; j < path.size(); j++) {
                                    HashMap<String, String> point = path.get(j);

                                    double lat = Double.parseDouble(point.get("lat"));
                                    double lng = Double.parseDouble(point.get("lng"));
                                    LatLng position = new LatLng(lat, lng);

                                    points.add(position);
                                }

                                // Adding all the points in the route to LineOptions
                                lineOptions.addAll(points);
                                lineOptions.width(8);
                                lineOptions.color(Color.BLUE);

                            }

                            if(lineOptions != null && mMap != null) {
                                polyline = mMap.addPolyline(lineOptions);
                            }

                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
    }

    //build the URL for direction
    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // mode = driving | walking | bicycling | transit
        String mode = "mode=walking";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        String key =  "&key=AIzaSyAWJVpcs633Ah4a_trh4M0W3BWadOuCtYs";

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + key;

        return url;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(getActivity(), pointObjList));

        if(pointObj != null && pointObjList != null) {
            goToLocations(pointObjList);
            goToLocation(pointObj);
        }
        else if(pointObj != null)
            goToLocation(pointObj);
        else {
            LatLng place = new LatLng(lat, lng);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(place, 14.5f);
            mMap.animateCamera(cameraUpdate);
        }
    }

    //set multiple points on the map
    public void goToLocations(ArrayList<MapPointObj> pointObjList) {
        if(mMap == null || pointObjList == null) return;
        this.pointObjList = pointObjList;
        mMap.clear();
        if(myMarker != null)
            mMap.addMarker(new MarkerOptions().position(myMarker.getPosition()).title("My Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        for(MapPointObj pointObj : pointObjList) {
            if(pointObj == this.pointObj) continue; //don't want the chosen place in the same color
            double lat = pointObj.getLat();
            double lng = pointObj.getLng();
            LatLng place = new LatLng(lat, lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(place)
                    .title(pointObj.getName())
                    .snippet(pointObj.getAddress()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            marker.setTag(pointObj);
        }
    }

    //set one point on the map
    public void goToLocation(MapPointObj pointObj) {
        if(mMap == null || pointObj == null) return;
        double lat = pointObj.getLat();
        double lng = pointObj.getLng();
        LatLng place = new LatLng(lat, lng);
        final Marker marker = mMap.addMarker(new MarkerOptions()
                .position(place)
                .title(pointObj.getName())
                .snippet(pointObj.getAddress())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        marker.setTag(pointObj);
        marker.showInfoWindow();
        bounceMarker(marker);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(place, 15.0f);
        mMap.animateCamera(cameraUpdate);
    }

    //called from Main Activity when it get a location
    public void locationResult(double lat, double lng, boolean valid) {

        if(flagCallGetLatLng) {
            flagCallGetLatLng = false;
            if(!valid) {
                flagCallGetLatLng = true;
            }
            this.lat = lat;
            this.lng = lng;
            if(mMap == null) return;
            LatLng place = new LatLng(lat, lng);
            myMarker = mMap.addMarker(new MarkerOptions().position(place).title("My Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            bounceMarker(myMarker);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(place, 15.0f);
            mMap.animateCamera(cameraUpdate);
        }
    }

    // This causes the marker to bounce into position
    private void bounceMarker(final Marker marker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                marker.setAnchor(0.5f, 1.0f + 2 * t);

                if (t > 0.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    //make a route when click on Marker on the map
    @Override
    public boolean onMarkerClick(final Marker marker) {
        Toast.makeText(getActivity(), "click on " + marker.getTitle(), Toast.LENGTH_SHORT).show();
        marker.showInfoWindow();
        if(polyline != null) {
            polyline.remove();
        }
        if(myMarker != null && myMarker != marker) {
            LatLng orig = myMarker.getPosition();
            LatLng dest = marker.getPosition();
            googleMapsPath(orig, dest);
        }

        return true;
    }
}
