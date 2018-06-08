package com.shlominet.myplaces01;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.shlominet.utils.FirebaseUtil;
import com.shlominet.utils.MapPointObj;
import com.shlominet.utils.MyAdapter;
import com.shlominet.utils.MyInterface;
import com.shlominet.utils.ObjectSerializer;

import java.io.IOException;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class FavFragment extends Fragment {

    private ArrayList<MapPointObj> arrayList;
    private MyAdapter adapter;
    private RecyclerView recyclerView;

    private double lastLat = 0, lastLng = 0;

    public FavFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_fav, container, false);

        //get last (lat , lng) to measure the distance
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if(sharedPref.getFloat("lastLat", 0) != 0) {
            lastLat = sharedPref.getFloat("lastLat", 0);
            lastLng = sharedPref.getFloat("lastLng", 0);
        }

        arrayList = new ArrayList<>();

        adapter = new MyAdapter(getActivity(), arrayList, new MyInterface.OnItemClickListener() {
            @Override
            public void onItemClick(MapPointObj item) {
                final MyInterface.FragHelper mainHelper = (MyInterface.FragHelper) getActivity();
                mainHelper.passDataL2M(item);
            }
        }, new MyInterface.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(MapPointObj item) {}
        }, "fav");

        recyclerView = view.findViewById(R.id.fav_recycleview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);

        showFavs();

        return view;
    }

    //extract all the saves favorites from Firebase(online) or from local(offline)
    private void showFavs() {

        //has network connection
        if(haveNetworkConnection()) {
            FirebaseAuth mAuth = FirebaseUtil.getAuth();
            FirebaseUser user = mAuth.getCurrentUser();
            FirebaseDatabase database = FirebaseUtil.getDatabase();
            DatabaseReference usersRef = database.getReference("users/" + user.getUid());
            DatabaseReference favorRef = usersRef.child("favorites");
            favorRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    arrayList.clear();
                    for (DataSnapshot fav : dataSnapshot.getChildren()) {
                        MapPointObj pointObj = fav.getValue(MapPointObj.class);
                        arrayList.add(pointObj);
                    }
                    for (MapPointObj item : arrayList)
                        item.setDistance(lastLat, lastLng);

                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }

        //no network connection
        else {
            //get local list
            //get the current favArray from shared-pref
            ArrayList<MapPointObj> favList = new ArrayList<>();
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            try {
                favList = (ArrayList) ObjectSerializer.deserialize(sharedPref.getString("favArray", ObjectSerializer.serialize(new ArrayList())));
                arrayList = favList;
                for (MapPointObj item : arrayList)
                    item.setDistance(lastLat, lastLng);
                adapter = new MyAdapter(getActivity(), arrayList, new MyInterface.OnItemClickListener() {
                    @Override
                    public void onItemClick(MapPointObj item) {
                        final MyInterface.FragHelper mainHelper = (MyInterface.FragHelper) getActivity();
                        mainHelper.passDataL2M(item);
                    }
                }, new MyInterface.OnItemLongClickListener() {
                    @Override
                    public void onItemLongClick(MapPointObj item) {}
                }, "fav");
                recyclerView.setAdapter(adapter);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    //check if there is connection to the network
    private boolean haveNetworkConnection() {

        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
