package com.example.jorge.gohome_v1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMapLongClickListener{

    private FusedLocationProviderClient mFusedLocationClient;

    private GoogleMap mMap;
    private LatLng coordenadasHOME, coordenadasME;

    private Marker marcadorME, marcadorHOME;

    private Button btnCalcular;
    private EditText xME, yME, xHOME, yHOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        inicializarUI();

    }

    private void inicializarUI(){

        btnCalcular = (Button) findViewById(R.id.btnCalcular); btnCalcular.setOnClickListener(this);

        xME = (EditText) findViewById(R.id.txt_xPosIM); xME.setOnClickListener(this);
        yME = (EditText) findViewById(R.id.txt_yPosIM); yME.setOnClickListener(this);

        xHOME = (EditText) findViewById(R.id.txt_xPosHOME); xHOME.setOnClickListener(this);
        yHOME = (EditText) findViewById(R.id.txt_yPosHOME); yHOME.setOnClickListener(this);

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {

            @Override
            public void onSuccess(Location location) {

                if (location != null) {

                    LatLng  myUbication = new LatLng(location.getLatitude(), location.getLongitude() );
                    CameraUpdate cm = CameraUpdateFactory.newLatLngZoom(myUbication, 16);

                    mMap.addMarker(new MarkerOptions()
                            .position(myUbication)
                            .title("Mi ubicación actual")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round)));

                    mMap.animateCamera(cm);

                    coordenadasME = myUbication;

                    xME.setText("(Lat):"+coordenadasME.latitude);
                    yME.setText("(Lng):"+coordenadasME.longitude);

                    Toast.makeText(MapsActivity.this,"Ubicando ...", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(MapsActivity.this,"No se puede obtener localización", Toast.LENGTH_LONG).show();

                }

            }

        });

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        coordenadasHOME = latLng;

        xHOME.setText("(Lat):"+coordenadasHOME.latitude);
        yHOME.setText("(Lng):"+coordenadasHOME.longitude);
        agregarMarkHOME(coordenadasHOME);

    }

    public void agregarMarkHOME(LatLng coordenadas) {

        if (marcadorHOME != null) marcadorHOME.remove();
        marcadorHOME = mMap.addMarker(new MarkerOptions()
                .position(coordenadas).title("Ubicación casa")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));

    }

    public void agregarMarkME(LatLng coordenadas) {

        if (marcadorME != null) marcadorME.remove();
        marcadorME = mMap.addMarker(new MarkerOptions()
                .position(coordenadas).title("Mi ubicación actual")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round)));

    }

    @Override
    public void onClick(View v) {

        if(v.equals(btnCalcular)){

            mMap.clear();

            agregarMarkHOME(coordenadasHOME);
            agregarMarkME(coordenadasME);

            Toast.makeText(this, "CALCULANDO RUTA ...", Toast.LENGTH_SHORT).show();

            String url = getDirectionsUrl(coordenadasME, coordenadasHOME);
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.execute(url);

        }

    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        String sensor = "sensor=false";

        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        String output = "json";

        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;

    }

    private String downloadUrl(String strUrl) throws IOException {

        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;

        try{

            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Error durante descarga", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }

        return data;

    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }

            return data;

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);

        }

    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{

                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);

            }catch(Exception e){
                e.printStackTrace();
            }

            return routes;

        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for(int i=0;i<result.size();i++){

                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for(int j=0;j<path.size();j++){

                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);

                }

                lineOptions.addAll(points);
                lineOptions.width(5);
                lineOptions.color(Color.RED);

            }

            mMap.addPolyline(lineOptions);

        }

    }

}
