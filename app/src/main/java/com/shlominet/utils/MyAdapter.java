package com.shlominet.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.shlominet.myplaces01.R;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static com.shlominet.myplaces01.ListFragment.urlKey3;

//custom adapter for the recycler view
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyHolder> {

    private static MapPointObj selectedItem;
    private Context context;
    private ArrayList<MapPointObj> arrayList;
    private final MyInterface.OnItemClickListener listenerC;
    private final MyInterface.OnItemLongClickListener listenerLC;
    private String selectedUnits = "";
    private String frag;
    private final String TAG = "aaa";

    public MyAdapter(Context context, ArrayList<MapPointObj> arrayList
            , MyInterface.OnItemClickListener listenerC
            , MyInterface.OnItemLongClickListener listenerLC
            , String frag) {
        this.context = context;
        this.arrayList = arrayList;
        this.listenerC = listenerC;
        this.listenerLC = listenerLC;
        this.frag = frag;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        getPrefUnits();
        return new MyHolder(itemView);
    }

    private void getPrefUnits() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String[] unitsArr = context.getResources().getStringArray(R.array.unit_list_pref);
        String unitsArrIndex = sharedPref.getString("units", "0");
        selectedUnits = unitsArr[Integer.valueOf(unitsArrIndex)];
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        final MapPointObj itemObj = arrayList.get(position);
        Log.d("aaa", "onBindViewHolder:position " + position);
        holder.bind(itemObj, listenerC, listenerLC);
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        public TextView nameTV, addressTV, distTV, ratingTV;
        public ImageView imageView, imageTel;
        public RatingBar ratingBar;


        public MyHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_img);
            imageTel = itemView.findViewById(R.id.item_tel_img);
            nameTV = itemView.findViewById(R.id.item_name_tv);
            addressTV = itemView.findViewById(R.id.item_address_tv);
            distTV = itemView.findViewById(R.id.item_dist_tv);
            ratingTV = itemView.findViewById(R.id.rating_tv);
            ratingBar = itemView.findViewById(R.id.rating_rb);
            //itemView.setOnCreateContextMenuListener(this);
        }

        public void bind(final MapPointObj item, final MyInterface.OnItemClickListener listenerC, final MyInterface.OnItemLongClickListener listenerLC) {
            //set name
            nameTV.setText(item.getName());

            //set address
            if(item.getAddress().equals("")) {
                addressTV.setText(item.getDistanceMeter() + " m");
                Geocoder gc = new Geocoder(context);
                try {
                    Address place = gc.getFromLocation(item.getLat(), item.getLng(), 1).get(0);
                    String address = place.getAddressLine(0);
                    item.setAddress(address);
                    addressTV.setText(address);
                } catch (IOException e) { e.printStackTrace(); }
            }
            else
                addressTV.setText(item.getAddress());

            //set distance
            if(selectedUnits.equals("Kilometer")) {
                int distI = item.getDistanceMeter();
                if(distI >= 1000) {
                    float distF = distI / 1000f;
                    distTV.setText(new DecimalFormat("##.##").format(distF) + " km");
                } else
                    distTV.setText(item.getDistanceMeter() + " m");
            } else
                distTV.setText(new DecimalFormat("##.##").format(item.getDistanceMile()) + " miles");

            //set image
            if(!item.getImgUrl().equals(""))
                Picasso.get().load(item.getImgUrl()).into(imageView);
            //enlarge image on click
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showImgDialog();
                }

                private void showImgDialog() {
                    if(item.getImgUrl().equals("")){
                        return;
                    }
                    Dialog dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.dialog_image);
                    ImageView image = dialog.findViewById(R.id.dialog_img);
                    Picasso.get().load(item.getImgUrl()).into(image);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                }
            });

            //call number
            imageTel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: imageTel");
                    String url = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + item.getPlaceid() + "&" + urlKey3;
                    Log.d(TAG, "url: " + url);
                    Ion.with(context)
                            .load(url)
                            .asString()
                            .setCallback(new FutureCallback<String>() {
                                @Override
                                public void onCompleted(Exception e, String result) {
                                    String phoneNum = "";
                                    Log.d(TAG, "setCallback: result= " + result);
                                    if (result == null) {
                                        Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
                                        Log.d("aaa", "no phone number");
                                        return;
                                    }
                                    try {
                                        JSONObject root = new JSONObject(result);
                                        String status = root.getString("status");
                                        if (!status.equals("OK")) {
                                            Log.d("aaa", "Json status: " + status);
                                            Toast.makeText(context, "Json status: " + status, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        JSONObject jsonObject = root.getJSONObject("result");
                                        if (jsonObject.isNull("international_phone_number")) {
                                            Toast.makeText(context, "No Phone Number", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        phoneNum = jsonObject.getString("international_phone_number");

                                        Intent intent4 = new Intent(Intent.ACTION_DIAL);

                                        intent4.setData(Uri.parse("tel: " + phoneNum));
                                        //check if dialer is ok
                                        if (intent4.resolveActivity(context.getPackageManager()) != null)
                                            context.startActivity(intent4);

                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            });
                }
            });

            //set rating
            if(item.getRating() != 0) {
                ratingTV.setText(item.getRating() +"");
                ratingBar.setRating(item.getRating());
            }

            //click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listenerC.onItemClick(item);
                    //selectedItem = item;
                }
            });
            //long-click listener
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //Toast.makeText(context, "Adapter:onLongClick: " + item.toString(), Toast.LENGTH_SHORT).show();
                    listenerLC.onItemLongClick(item);
                    selectedItem = item;
                    return false;
                }
            });
            //long-click menu
            itemView.setOnCreateContextMenuListener(this);
        }

        //long click open context menu (share && (add to favorites || remove from favorites))
        //depend on which fragment is it (List or Favorites)
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Select Action");

            MenuItem share = menu.add(Menu.NONE,1,1,"Share");
            MenuItem addFav = null;
            MenuItem delFav = null;

            if(frag.equals("list"))
                addFav = menu.add(Menu.NONE,2,2,"Add to Favorite");

            else /*frag.equals("fav")*/
                delFav = menu.add(Menu.NONE,2,2,"Remove from Favorite");

            share.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    String url = String.format("https://www.google.com/maps/search/?api=1&query=%f,%f&query_place_id=%s"
                            , selectedItem.getLat(), selectedItem.getLng(), selectedItem.getPlaceid());
                    String msg = "I shared " + selectedItem.getName() + " location with you:\n" + url;
                    sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                    sendIntent.setType("text/plain");
                    context.startActivity(sendIntent);

                    /*//open location in google maps
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + selectedItem.getAddress()));
                    context.startActivity(intent);*/

                    return true;
                }
            });
            if(frag.equals("list")) {
                addFav.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Toast.makeText(context, "click on: " + item.getTitle() + " " +selectedItem.getName(), Toast.LENGTH_SHORT).show();

                        FirebaseAuth mAuth = FirebaseUtil.getAuth();
                        FirebaseUser user = mAuth.getCurrentUser();
                        FirebaseDatabase database = FirebaseUtil.getDatabase();
                        DatabaseReference usersRef = database.getReference("users/" + user.getUid());
                        DatabaseReference favorRef = usersRef.child("favorites");
                        favorRef.child(selectedItem.hashCode() + "").setValue(selectedItem);

                        //save locally
                        //get the current favArray from shared-pref
                        ArrayList<MapPointObj> favList = new ArrayList<>();
                        SharedPreferences sharedPref = ((Activity)context).getPreferences(Context.MODE_PRIVATE);
                        try {
                            favList = (ArrayList) ObjectSerializer.deserialize(sharedPref.getString("favArray", ObjectSerializer.serialize(new ArrayList())));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        //add the item to array (if not exist)
                        for(int i = 0; i<favList.size(); i++) {
                            if(favList.get(i).getAddress().equals(selectedItem.getAddress())) {
                                return true;
                            }
                        }
                        favList.add(selectedItem);
                        //save the list back to shared-pref
                        SharedPreferences.Editor editor = sharedPref.edit();
                        try {
                            editor.putString("favArray", ObjectSerializer.serialize(favList));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        editor.commit();

                        return true;
                    }
                });
            }
            else if(frag.equals("fav")) {
                //delete item instead of add item
                delFav.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        FirebaseAuth mAuth = FirebaseUtil.getAuth();
                        FirebaseUser user = mAuth.getCurrentUser();

                        FirebaseDatabase database = FirebaseUtil.getDatabase();
                        DatabaseReference usersRef = database.getReference("users/" + user.getUid());
                        DatabaseReference favorRef = usersRef.child("favorites");
                        favorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for(DataSnapshot fav : dataSnapshot.getChildren()) {
                                    String address = fav.child("address").getValue().toString();
                                    if (address.equals(selectedItem.getAddress())) {
                                        fav.getRef().removeValue();
                                        break;
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });

                        //remove locally
                        //get the current favArray from shared-pref
                        ArrayList<MapPointObj> favList = new ArrayList<>();
                        SharedPreferences sharedPref = ((Activity)context).getPreferences(Context.MODE_PRIVATE);
                        try {
                            favList = (ArrayList) ObjectSerializer.deserialize(sharedPref.getString("favArray", ObjectSerializer.serialize(new ArrayList())));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        //remove the item from array
                        for(int i = 0; i<favList.size(); i++) {
                            if(favList.get(i).getAddress().equals(selectedItem.getAddress())) {
                                favList.remove(i);
                                arrayList.remove(i--);
                            }
                        }
                        notifyDataSetChanged();
                        //save the list back to shared-pref
                        SharedPreferences.Editor editor = sharedPref.edit();
                        try {
                            editor.putString("favArray", ObjectSerializer.serialize(favList));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        editor.commit();

                        return true;
                    }
                });
            }
        }
    }
}

