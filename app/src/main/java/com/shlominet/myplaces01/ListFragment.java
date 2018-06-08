package com.shlominet.myplaces01;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import com.shlominet.utils.MapPointObj;
import com.shlominet.utils.MyAdapter;
import com.shlominet.utils.MyInterface;

import java.util.ArrayList;


public class ListFragment extends Fragment /*implements MyInterface.LocationHelper*/ {

    //prefix URL
    private final String urlMain1 = "https://maps.googleapis.com/maps/api/place/";
    //choose only 1 of 3
    private final String urlText2 = "textsearch/json?";
    private final String urlNearby2 = "nearbysearch/json?";
    private final String urlPhoto2 = "photo?";
    //key- MUST
    public static final String urlKey3 = "key=YOUR-API-KEY";
    //textsearch- MUST
    private final String urlQueryTM4= "&query=";//The text string on which to search, for example: "restaurant" or "123 Main Street".
    //textsearch- OPTIONAL
    private final String urlRadiusTO5 = "&radius=";//Defines the distance (in meters) within which to return place results.
    private final String urlLocationTO6 = "&location=";//The latitude/longitude around which to retrieve place information.
    private final String urlTypeTO7 = "&type=";//Restricts the results to places matching the specified type. see more: https://developers.google.com/places/web-service/supported_types
    //nearby- MUST
    private final String urlLocationNM4 = "&location=";//The latitude/longitude around which to retrieve place information.
    private final String urlRadiusNM5 = "&radius=";//Defines the distance (in meters) within which to return place results.
    //nearby- OPTIONAL
    private final String urlKeywordNO6 = "&keyword=";//A term to be matched against all content that Google has indexed for this place.
    private final String urlTypeNO7 = "&type=";//Restricts the results to places matching the specified type. see more: https://developers.google.com/places/web-service/supported_types
    //photo- MUST
    private final String urlPhotoRefP5 = "&photoreference=";//A string identifier that uniquely identifies a photo.
    private final String urlMaxwidth4 = "&maxwidth=";//Specifies the maximum desired width, in pixels.


    private Button searchBtn, nearmeBtn, radiusPBtn, radiusMBtn;
    private EditText searchET;
    private Spinner nearmeSpin;
    private TextView radiusTV;

    private ArrayList<MapPointObj> arrayList;
    private MyAdapter adapter;
    private RecyclerView recyclerView;

    private String selectedTypeSpin = "";
    private String lastResult = "";
    private double lat = 0, lng = 0;
    private boolean flagCallGetLatLng = false;
    private boolean flagNearmeCall = false;
    private boolean flagSearchCall = false;

    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        searchET = view.findViewById(R.id.list_search_et);
        searchBtn = view.findViewById(R.id.list_search_btn);
        nearmeSpin = view.findViewById(R.id.list_nearme_spinner);
        nearmeBtn = view.findViewById(R.id.list_nearme_btn);
        radiusTV = view.findViewById(R.id.list_radius_tv);
        radiusPBtn = view.findViewById(R.id.list_radius_p_btn);
        radiusMBtn = view.findViewById(R.id.list_radius_m_btn);

        //arrayList -> adapter -> recyclerView
        arrayList = new ArrayList<>();

        adapter = new MyAdapter(getActivity(), arrayList, new MyInterface.OnItemClickListener() {
            @Override
            public void onItemClick(MapPointObj item) {
                final MyInterface.FragHelper mainHelper = (MyInterface.FragHelper) getActivity();
                mainHelper.passDataL2M(item, arrayList);
                //mainHelper.passDataL2M(item);
            }
        }, new MyInterface.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(MapPointObj item) {}
        }, "list");

        recyclerView = view.findViewById(R.id.list_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);

        searchBtn.setOnClickListener(new MyClickListener());
        nearmeBtn.setOnClickListener(new MyClickListener());

        nearmeSpin.setOnItemSelectedListener(new MyItemSelectedListener());
        radiusPBtn.setOnClickListener(new MyClickListener());
        radiusMBtn.setOnClickListener(new MyClickListener());

        //restore the list of last results
        if (savedInstanceState != null) {
            lat = savedInstanceState.getDouble("lastLan");
            lng = savedInstanceState.getDouble("lastLng");
            lastResult = savedInstanceState.getString("lastResult", "");
            if(lastResult != null && !lastResult.equals("")) {
                arrayList.clear();
                parseJsonToList(lastResult);
                for(MapPointObj item : arrayList)
                    item.setDistance(lat, lng);
                adapter.notifyDataSetChanged();
            }
        }

        return view;
    }

    //get Json string, parse it and put its values in a list
    private void parseJsonToList(String data) {
        try {
            JSONObject root = new JSONObject(data);
            //check is status is OK
            String status = root.getString("status");
            if(!status.equals("OK")) {
                Toast.makeText(getActivity(), "No Results", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray results = root.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                JSONObject location = result.getJSONObject("geometry").getJSONObject("location");

                //get (lat, lng)
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");

                //get address
                String address = "";
                if(!result.isNull("formatted_address")) {
                    address = result.getString("formatted_address");
                } else if(!result.isNull("vicinity")) {
                    address = result.getString("vicinity");
                }

                //get name
                String name = result.getString("name");

                //get place_id for extra info
                String placeid = result.getString("place_id");

                //get photo url
                String photoUrl = "";
                if(!result.isNull("photos")) {
                    JSONArray photos = result.getJSONArray("photos");
                    JSONObject photo = photos.getJSONObject(0);
                    photoUrl = photo.getString("photo_reference");
                    photoUrl = urlMain1 + urlPhoto2 + urlKey3 + urlMaxwidth4 + 240 + urlPhotoRefP5 + photoUrl;
                }

                //get rating
                float rating = 0;
                if(!result.isNull("rating")) {
                    rating = (float)result.getDouble("rating");
                }

                //make the object from the data above
                MapPointObj point;
                point = new MapPointObj(lat, lng, address, name, photoUrl);
                point.setRating(rating);
                point.setPlaceid(placeid);

                arrayList.add(point);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //set spinner select item from "types" (cafe, atm...)
    class MyItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            switch (adapterView.getId()) {
                case R.id.list_nearme_spinner:
                    String[] typeItems = getResources().getStringArray(R.array.types_spinner);
                    selectedTypeSpin = typeItems[i].replaceAll(" ", "_").toLowerCase();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    }

    //set buttons click listener
    private class MyClickListener implements View.OnClickListener {
        final MyInterface.FragHelper mainHelper = (MyInterface.FragHelper)getActivity();
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                //case click on- free text search
                case R.id.list_search_btn:
                    String text = searchET.getText().toString();
                    text = text.replaceAll("\\s+","+");
                    if(text.trim().length() == 0) {
                        Toast.makeText(getActivity(), "Enter Place Name to Search", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    arrayList.clear();
                    adapter.notifyDataSetChanged();
                    flagCallGetLatLng = true;
                    flagSearchCall = true;

                    //call Main Activity method to return my GPS location
                    mainHelper.getGpsLocation();

                    /*findSearch() will be called after main activity will return result*/
                    break;

                //case click on- near me search
                case R.id.list_nearme_btn:
                    flagCallGetLatLng = true;
                    flagNearmeCall = true;
                    arrayList.clear();
                    adapter.notifyDataSetChanged();

                    //call Main Activity method to return my GPS location
                    mainHelper.getGpsLocation();

                    /*findNearby() will be called after main activity will return result*/
                    break;

                //case click on "+" on radius
                case R.id.list_radius_p_btn:
                    String str1 = radiusTV.getText().toString();
                    int rad1 = Integer.valueOf(str1);
                    if(rad1 >= 16000) return;
                    rad1 *= 2;
                    radiusTV.setText(rad1 + "");

                    break;

                //case click on "-" on radius
                case R.id.list_radius_m_btn:
                    String str2 = radiusTV.getText().toString();
                    int rad2 = Integer.valueOf(str2);
                    if(rad2 <= 250) return;
                    rad2 /= 2;
                    radiusTV.setText(rad2 + "");

                    break;

                default:
                    break;
            }
        }
    }

    //called from Main Activity- get my (lat, lng)
    public void locationResult(double lat, double lng, boolean valid) {

        //determine what function called for GPS location
        if(flagCallGetLatLng) {
            flagCallGetLatLng = false;
            this.lat = lat;
            this.lng = lng;

            //"nearBy" api was called
            if(flagNearmeCall) {
                flagNearmeCall = false;
                if(!valid) {
                    flagCallGetLatLng = true;
                    flagNearmeCall = true;
                }
                findNearby();
            }

            //"searchText" api was called
            if(flagSearchCall) {
                flagSearchCall = false;
                if(!valid) {
                    flagCallGetLatLng = true;
                    flagSearchCall = true;
                }
                findSearch();
            }
        }
    }

    //build the proper URL from "text search"
    private void findSearch() {
        String text = searchET.getText().toString();
        text = text.replaceAll("\\s+","+");
        String radiusStr = radiusTV.getText().toString();
        String fullUrlT = urlMain1 + urlText2 + urlKey3 + urlQueryTM4 + text + urlRadiusTO5 + radiusStr;

        urlToList(fullUrlT);
    }

    //build the proper URL from "near me" search
    private void findNearby() {
        String radiusStr = radiusTV.getText().toString();
        String fullUrlN = urlMain1 + urlNearby2 + urlKey3 + urlLocationNM4 + lat + "," + lng
                + urlRadiusNM5 + radiusStr + urlTypeNO7 + selectedTypeSpin;

        urlToList(fullUrlN);
    }

    //http request of the "place" api, get Json, parse it and show on the list
    private void urlToList(String url) {

        //Ion is Android library for Asynchronous Networking and Image Loading
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
                        lastResult = result;
                        arrayList.clear();
                        parseJsonToList(result);
                        for(MapPointObj item : arrayList)
                            item.setDistance(lat, lng);
                        adapter.notifyDataSetChanged();
                        //if in landscape -> show results on map
                        if(!getResources().getBoolean(R.bool.portrait)) {
                            final MyInterface.FragHelper mainHelper = (MyInterface.FragHelper) getActivity();
                            mainHelper.passDataL2M(null, arrayList);
                        }

                    }
                });
    }

    //save last results and last location for future fragments replacement
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastResult" , lastResult);
        outState.putDouble("lastLan" , lat);
        outState.putDouble("lastLng" , lng);
    }

    //save last results for next time we open the app
    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("lastResult", lastResult);
        editor.commit();
    }

    //restore last results and last location from previous time we opened the app
    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if(sharedPref.getFloat("lastLat", 0) != 0) {
            lat = sharedPref.getFloat("lastLat", 0);
            lng = sharedPref.getFloat("lastLng", 0);
            lastResult = sharedPref.getString("lastResult", "");
            arrayList.clear();
            parseJsonToList(lastResult);
            for(MapPointObj item : arrayList)
                item.setDistance(lat, lng);
            adapter.notifyDataSetChanged();
        }
    }

    //restore last results from previous fragments replacement
    @Override
    public void onResume() {
        super.onResume();
        if(lastResult != null && !lastResult.equals("")) {
            arrayList.clear();
            parseJsonToList(lastResult);
            for(MapPointObj item : arrayList)
                item.setDistance(lat, lng);
            adapter.notifyDataSetChanged();
        }
    }
}
