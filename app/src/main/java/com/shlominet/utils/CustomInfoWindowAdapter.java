package com.shlominet.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.shlominet.myplaces01.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

//Info Window display info above the marker on the map
public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
    // "title" and "snippet".
    private final View mWindow;
    private final View mContents;
    private Context context;
    private ArrayList<MapPointObj> pointObjList;

    public CustomInfoWindowAdapter(Context context, ArrayList<MapPointObj> pointObjList) {

        this.context = context;
        this.pointObjList = pointObjList;
        mWindow = ((Activity)context).getLayoutInflater().inflate(R.layout.custom_info_window, null);
        mContents = ((Activity)context).getLayoutInflater().inflate(R.layout.custom_info_window, null);
    }
    @Override
    public View getInfoWindow(Marker marker) {

        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {

        render(marker, mContents);
        return mContents;
    }

    private void render(Marker marker, View view) {

        //set image
        ImageView imageView = view.findViewById(R.id.badge);
        MapPointObj pointObj = (MapPointObj) marker.getTag();
        if(pointObj != null) {
            if (!pointObj.getImgUrl().equals(""))
                Picasso.get().load(pointObj.getImgUrl()).into(imageView);
        }

        //set title
        String title = marker.getTitle();
        TextView titleUi = view.findViewById(R.id.title);
        if (title != null) {
            // Spannable string allows us to edit the formatting of the text.
            SpannableString titleText = new SpannableString(title);
            titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
            titleUi.setText(titleText);
        } else {
            titleUi.setText("");
        }

        //set snippet
        String snippet = marker.getSnippet();
        TextView snippetUi = view.findViewById(R.id.snippet);
        if (snippet != null) {
            SpannableString snippetText = new SpannableString(snippet);
            snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, snippetText.length(), 0);
            snippetUi.setText(snippetText);
        } else {
            snippetUi.setText("");
        }
    }
}
