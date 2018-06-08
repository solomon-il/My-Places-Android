package com.shlominet.myplaces01;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.squareup.picasso.Picasso;

import com.shlominet.utils.FirebaseUtil;
import com.shlominet.utils.MapPointObj;
import com.shlominet.utils.MyInterface;
import com.shlominet.utils.PrefClass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MyInterface.FragHelper, LocationListener {

    private static final int REQ_CODE_GPS = 123;
    private static final int REQ_CAM = 0;

    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private Fragment listFragment, mapFragment;
    private LocationManager locationManager;

    private ImageView navImage;

    private BroadcastReceiver receiver;
    private Intent batteryStatus;
    private static int lastReceiverStatus = -2;

    private double lat=0, lng=0;
    private double lastLat=0, lastLng=0;
    private static boolean flagFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //build receiver for get the charging status
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Are we charging / charged?
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;

                if(lastReceiverStatus != status) {
                    lastReceiverStatus = status;

                    if(!isCharging) {
                        Toast.makeText(context,"Charger Disconnected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // How are we charging?
                    int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                    boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

                    Toast.makeText(context,
                            acCharge? "AC Charger Connected": (usbCharge?"USB Charger Connected": "Charger Connected"),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        //determine the screen orientation for phone (portrait) and tablet (landscape)
        if (getResources().getBoolean(R.bool.portrait))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //init fireBase
        mAuth = FirebaseUtil.getAuth();
        mStorageRef = FirebaseUtil.getStorageRef();

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //drawer layout
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //navigation view
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //username on nav bar
        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = headerView.findViewById(R.id.nav_username_tv);
        navUsername.setText(mAuth.getCurrentUser().getEmail());

        //photo on nav bar
        navImage = headerView.findViewById(R.id.nav_imageView);
        //grab the photo from fireBase
        StorageReference storageRef = FirebaseUtil.getStorageRef();
        storageRef.child("img/" + mAuth.getCurrentUser().getEmail()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL
                // Pass it to Picasso to download, show in ImageView and caching
                Picasso.get().load(uri.toString()).into(navImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                navImage.setImageResource(R.drawable.default_avatar);
            }
        });

        //make click to take new photo
        navImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePicture, REQ_CAM);
            }
        });

        initFragments();
    }

    private void initFragments() {

        listFragment = new ListFragment();
        mapFragment = new MapFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.main_frame, listFragment);
        //if in tablet -> add map fragment also
        if(!getResources().getBoolean(R.bool.portrait)) {
            transaction.add(R.id.main_frame2, mapFragment);
        }
        transaction.commit();
    }

    //called from other fragments to get the GPS location
    @Override
    public void getGpsLocation() {

        if(!enableGps()) {
            ((ListFragment)listFragment).locationResult(lastLat, lastLng, false);
            ((MapFragment)mapFragment).locationResult(lastLat, lastLng, false);
        }
    }

    //check permission for GPS and then request for location (listener)
    @SuppressLint("MissingPermission")
    private boolean enableGps() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_GPS);
                return false;
            }
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //String provider = locationManager.getBestProvider(new Criteria(), true);
        String netProvider = LocationManager.NETWORK_PROVIDER;
        String gpsProvider = LocationManager.GPS_PROVIDER;

        boolean gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network_enabled = isNetworkAvailable();

        if(!gps_enabled && flagFirstTime) {
            showGPSDisabledAlertToUser();
            flagFirstTime = false;
        }

        //get location from GPS if available
        if(gps_enabled) {
            locationManager.requestLocationUpdates(gpsProvider, 0, 25, this);
        }
        //get location from network if available and if GPS not available
        else if(network_enabled) {
            locationManager.requestLocationUpdates(netProvider, 0, 25, this);
        }

        return true;
    }

    //check if the device is connected to network
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //show dialog to enable the gps if it is off
    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enable GPS");
        builder.setMessage("Please enable gps for better results");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQ_CODE_GPS:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    //if permissions granted so call again the method getGpsLocation() to complete the task
                    getGpsLocation();
                }
                break;
            default:
                break;
        }
    }

    //get GPS location, return it to the fragments, and disable the "on location changed"
    @SuppressLint("MissingPermission")
    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();

            lastLat = lat;
            lastLng = lng;

            //return result of gps to the frags
            ((ListFragment)listFragment).locationResult(lat, lng, true);
            ((MapFragment)mapFragment).locationResult(lat, lng, true);

            locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            //camera
            case REQ_CAM:
                if (resultCode == RESULT_OK) {

                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    navImage.setImageBitmap(imageBitmap);

                    Uri filePath = bitmapToUri(imageBitmap);

                    //progress dialog for upload the image
                    final ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle("Uploading...");
                    progressDialog.show();

                    //fireBase storage reference
                    StorageReference storageRef = FirebaseUtil.getStorageRef();
                    StorageReference userImagesRef = storageRef.child("img/" + mAuth.getCurrentUser().getEmail());

                    userImagesRef.putFile(filePath)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    progressDialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                            .getTotalByteCount());
                                    progressDialog.setMessage("Uploaded "+(int)progress+"%");
                                }
                            });

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                        navImage.setImageBitmap(bitmap);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    //save small bitmap to internal memory and return the uri
    private Uri bitmapToUri(Bitmap photo) {
        String fileName = "Pic_" + new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date()) + ".png"; //file: "Pic_23042018_210058.png"
        File picsDir = getFilesDir(); //picsDir is a directory on the file system that's uniquely associated with the app
        File file = new File(picsDir, fileName); //file is created inside picsDir

        //save image in that file
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            photo.compress(Bitmap.CompressFormat.PNG, 70, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //set the URI in url_editText
        Uri uri = Uri.fromFile(file);
        return uri;
    }

    //called from List Fragment to deliver the items location to Map Fragment
    @Override
    public void passDataL2M(MapPointObj pointObj, ArrayList<MapPointObj> pointObjList) {
        //portrait
        if(getResources().getBoolean(R.bool.portrait)) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.addToBackStack(null);
            transaction.replace(R.id.main_frame, mapFragment);
            Bundle bundle = new Bundle();
            bundle.putParcelable("mapPointObj", pointObj);
            bundle.putParcelableArrayList("mapPointObjList", pointObjList);
            mapFragment.setArguments(bundle);
            transaction.commit();
        }
        //land
        else {
            ((MapFragment)mapFragment).goToLocations(pointObjList);
            ((MapFragment)mapFragment).goToLocation(pointObj);
        }
    }
    @Override
    public void passDataL2M(MapPointObj mapPointObj) {
        //portrait
        if(getResources().getBoolean(R.bool.portrait)) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.addToBackStack(null);
            transaction.replace(R.id.main_frame, mapFragment);
            Bundle bundle = new Bundle();
            bundle.putParcelable("mapPointObj", mapPointObj);
            mapFragment.setArguments(bundle);
            transaction.commit();
        } else {
            //land
            ((MapFragment)mapFragment).goToLocation(mapPointObj);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onBackPressed() {
        //close drawer if open
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            PrefClass prefClass = new PrefClass();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_up, R.anim.exit_to_down, R.anim.enter_from_down, R.anim.exit_to_up);
            transaction.addToBackStack(null);
            transaction.replace(R.id.main_frame, prefClass);
            transaction.commit();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //click "favorites"
        if (id == R.id.nav_favorite) {
            FavFragment favFragment = new FavFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_up, R.anim.exit_to_down, R.anim.enter_from_down, R.anim.exit_to_up);
//            transaction.addToBackStack(null);
            transaction.replace(R.id.main_frame, favFragment);
            transaction.commit();
        }

        //click "map"
        else if (id == R.id.nav_map) {
            //portrait
            if(getResources().getBoolean(R.bool.portrait)) {
                //in in map
                if (mapFragment != null && mapFragment.getView() != null && mapFragment.getView().getGlobalVisibleRect(new Rect())) {
                    return true;
                }
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
//                transaction.addToBackStack(null);
                transaction.replace(R.id.main_frame, mapFragment);
                transaction.commit();
            }
        }

        //click "search"
        else if (id == R.id.nav_search) {
            //portrait
            if(getResources().getBoolean(R.bool.portrait)) {
                //if in list
                if (listFragment != null && listFragment.getView() != null && listFragment.getView().getGlobalVisibleRect(new Rect())) {
                    return true;
                }
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                transaction.replace(R.id.main_frame, listFragment);
                transaction.commit();
            }
        }

        //click "settings"
        else if (id == R.id.nav_settings) {
            PrefClass prefClass = new PrefClass();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_up, R.anim.exit_to_down, R.anim.enter_from_down, R.anim.exit_to_up);
            transaction.addToBackStack(null);
            transaction.replace(R.id.main_frame, prefClass);
            transaction.commit();

        }

        //click "log out"
        else if (id == R.id.nav_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition( R.anim.fade_out, R.anim.fade_in);
            finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //restore last location from previous app operation
    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        if(sharedPref.getFloat("lastLat", 0) != 0) {
            lastLat = sharedPref.getFloat("lastLat", 0);
            lastLng = sharedPref.getFloat("lastLng", 0);
        }

        //battery register receiver
        batteryStatus = registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    //save last location for next app operation
    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("lastLat", (float)lastLat);
        editor.putFloat("lastLng", (float)lastLng);
        editor.commit();

        //battery unregister
        unregisterReceiver(receiver);
    }
}
