package com.example.daeng.exam_gps_map;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    SQLiteDatabase db;
    DB helper;
    ArrayList<LatLng> list = new ArrayList<LatLng>();
    PolylineOptions polylineOptions;


    public void initialize(){
        Cursor rs = db.rawQuery("select * from Location;", null);
        while(rs.moveToNext()){
            mMap.addCircle(new CircleOptions().center(new LatLng(rs.getDouble(0),rs.getDouble(1))).radius(3).strokeColor(Color.RED).fillColor(Color.BLUE));
            list.add(new LatLng(rs.getDouble(0), rs.getDouble(1)));
        }
        drawPolyline();
    }

    public void drawPolyline(){
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(5);
        polylineOptions.addAll(list);
        mMap.addPolyline(polylineOptions);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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
        // ↑매개변수로 GoogleMap 객체가 넘어옵니다.
        mMap = googleMap;
        helper = new DB(MapsActivity.this, "Location",null,1);
        db = helper.getWritableDatabase(); // 쓸 수 있는 db를 리턴

        GPSListener gps = new GPSListener(db, mMap, list);
        initialize();

        if(list.size()!=0){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(list.get(0)));
        }else{
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.610215, 126.997202)));
        }

        startLocationService();
    }

    private void startLocationService() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        GPSListener gpsListener = new GPSListener(db,mMap,list);
        long minTime = 10000;
        float minDistance = 10;

        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, gpsListener);

            Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                Double latitude = lastLocation.getLatitude();
                Double longitude = lastLocation.getLongitude();

                Toast.makeText(getApplicationContext(), "Last Known Location : " + "Latitude : " + latitude + "\nLongitude:" + longitude, Toast.LENGTH_LONG).show();
            }
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    class GPSListener implements LocationListener {
        SQLiteDatabase db;
        GoogleMap mMap;
        ArrayList<LatLng> save;


        GPSListener(SQLiteDatabase db, GoogleMap mMap, ArrayList<LatLng> save){
            this.db = db;
            this.mMap = mMap;
            this.save = save;
        }

        public void onLocationChanged(Location location) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

            db.execSQL( "insert into  Location(x,y) values ("+Double.toString(latitude)+"," +Double.toString(longitude)+");" );

            list.add(new LatLng(latitude,longitude));
            drawPolyline();

            mMap.addCircle(new CircleOptions().center(new LatLng(latitude, longitude)).radius(3).strokeColor(Color.RED).fillColor(Color.BLUE));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));


            String msg = "Latitude : "+ latitude + "\nLongitude:"+ longitude;

            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }


        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    class DB extends SQLiteOpenHelper{
        Context context;

        public DB(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
            super(context, name, factory, version);
            this.context = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String a = "create table Location(x double,y double);";
            db.execSQL(a);
            Toast.makeText(context,"DB가 생성되었습니다!! 와아~~", Toast.LENGTH_LONG).show();
        }//table생성

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String a = "create table Location(x double,y double);";
            db.execSQL(a);
            Toast.makeText(context,"DB가 업데이트 되었습니다.", Toast.LENGTH_LONG).show();
        }
    }

}


