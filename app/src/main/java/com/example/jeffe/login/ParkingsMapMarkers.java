package com.example.jeffe.login;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

import static com.loopj.android.http.AsyncHttpClient.LOG_TAG;


public class ParkingsMapMarkers extends AppCompatActivity implements interfaceListener,
                                                                     OnMapReadyCallback,
                                                                     GoogleMap.OnInfoWindowClickListener,
                                                                     GoogleMap.OnMyLocationButtonClickListener,
                                                                     GoogleMap.OnMyLocationClickListener,
                                                                     ActivityCompat.OnRequestPermissionsResultCallback{

    ConnectionDetector cd;

    private SharedPreferences pref;
    private final String MyPREFERENCES = "MyPrefs";
    ArrayList count     = new ArrayList();
    ArrayList idparki   = new ArrayList();
    ArrayList fini      = new ArrayList();
    ArrayList ffin      = new ArrayList();
    ArrayList parkiname = new ArrayList();
    ArrayList cost = new ArrayList();
    RelativeLayout mainL;
    ProgressBar progress;

    http http;

    PopupReservation popupReservation;

    private GoogleMap mMap;
    private Marker[] markers;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_maps_markers);

        pref= getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        mainL    = findViewById(R.id.mainlayout);
        progress = findViewById(R.id.progress);
        relativeLayout =findViewById(R.id.capaRelativL);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        mapFragment.getMapAsync(this);
        pref     = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);


        BottomNavigationView navigationActivity= (BottomNavigationView) findViewById(R.id.navigation);
        navigationActivity.setOnNavigationItemSelectedListener(AbrirActivity);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener AbrirActivity=
             new BottomNavigationView.OnNavigationItemSelectedListener() {
                 @Override
                 public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                     switch (menuItem.getItemId()) {
                         case R.id.nav_home:
                             Snackbar snackbar;
                             snackbar= Snackbar.make(relativeLayout,"Desea cerrar sesión?",Snackbar.LENGTH_LONG);
                             View snackBarView = snackbar.getView();
                             snackbar.setAction("SI", new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             Intent intentExit = new Intent(ParkingsMapMarkers.this, LoginMain.class);

                                             SharedPreferences.Editor editor = pref.edit();
                                             editor.putString(Constants.ID,"");
                                             editor.putString(Constants.EMAIL,"");
                                             editor.putString(Constants.NAME,"");
                                             editor.putString(Constants.PHONE,"");
                                             editor.putString(Constants.ADDRESS,"");
                                             editor.putString(Constants.AGE,"");
                                             editor.putBoolean(Constants.IS_LOGGED,false);
                                             editor.apply();
                                             startActivity(intentExit);
                                             finish();

                                             //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                         }
                                     });

                             snackbar.setActionTextColor(Color.WHITE);
                             snackBarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                             TextView textView = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_text);
                             textView.setTextColor(Color.WHITE);
                             snackbar.show();
                             break;

                         case R.id.nav_map:
                             break;

                         case R.id.nav_user:
                             Intent intentUser = new Intent(ParkingsMapMarkers.this, UserRegistredMain.class);
                             startActivity(intentUser);
                             overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                             finish();
                             break;
                     }
                     return true;
                 }
             };




    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
        addMarkersToMap(); //Get markets from DB.
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnInfoWindowClickListener((GoogleMap.OnInfoWindowClickListener) this);


    }

    private void addMarkersToMap() {
        cd = new ConnectionDetector(this);
        if(!cd.isNetworkAvailable()) {
            String Mess = "¡VERIFIQUE LA CONEXION DE RED!";
            Toasty.error(this,Mess);
            mainL.setAlpha(1);
            progress.setVisibility(View.INVISIBLE);
        }else {
            http = new http(ParkingsMapMarkers.this);
            http.getPlaces();
        }
    }


    //Control encontrar mi ubicacion
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
            // Deshabilitar boton de localizar ubicacion
            //mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
        }
    }


    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Hello! Muthafucker", Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }


    @Override
    public void onInfoWindowClick(final Marker marker) {

                Snackbar snackbar;
                View snackBarView;
                int setid = marker.getId().indexOf("m");
                final String suffix = marker.getId().substring(setid+1);
                if(count.get(Integer.parseInt(suffix)).equals("0")){
                    snackbar = Snackbar.make(relativeLayout, "Parqueadero sin espacios disponibles!", Snackbar.LENGTH_LONG);
                    snackBarView = snackbar.getView();
                    snackBarView.setBackgroundColor(getResources().getColor(R.color.colorOrange));

                }else{
                        snackbar = Snackbar.make(relativeLayout, "Espacios disponibles: " + count.get(Integer.parseInt(suffix)), Snackbar.LENGTH_LONG);
                        snackBarView = snackbar.getView();
                        snackbar.setAction("Reservar", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                AbirDialogAlert(parkiname.get(Integer.parseInt(suffix)).toString(),
                                        idparki.get(Integer.parseInt(suffix)).toString(),cost.get(Integer.parseInt(suffix)).toString()
                                );
                            }
                        });
                        snackbar.setActionTextColor(Color.WHITE);
                        snackBarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                }
                TextView textView = snackBarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(Color.WHITE);
                snackbar.show();
        }
    @Override
    public void onPlacesListener(int identifier, Class gointo, JSONArray data) {
        switch (identifier){
            case 1:
                try {
                    LatLngBounds.Builder b = new LatLngBounds.Builder();
                    markers = new Marker[data.length()];
                    for(int i = 0; i < data.length(); i++){
                        cost.add(i,data.getJSONObject(i).get("price"));
                        fini.add(i,data.getJSONObject(i).get("fini"));
                        ffin.add(i,data.getJSONObject(i).get("ffin"));
                        count.add(i,data.getJSONObject(i).get("cantidad"));
                        idparki.add(i,data.getJSONObject(i).get("identifier"));
                        parkiname.add(i,data.getJSONObject(i).get("name"));
                        b.include(new LatLng(data.getJSONObject(i).getDouble("latitud"),data.getJSONObject(i).getDouble("longitud")));
                        markers[i] = mMap.addMarker(new MarkerOptions()
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foreground_pya))
                                        //.icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.taj)));
                                        .position(new LatLng(data.getJSONObject(i).getDouble("latitud"),data.getJSONObject(i).getDouble("longitud")))
                                        .title("Parking Ya! " +data.getJSONObject(i).getString("name"))
                                        .snippet("Horario de atención: "+data.getJSONObject(i).getString("fini")+" a "+data.getJSONObject(i).getString("ffin")
                                        )
                        );
                    }
                    LatLngBounds bounds = b.build();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250));
                    mainL.setAlpha(1);
                    progress.setVisibility(View.INVISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    Snackbar snackbar;
                    View snackBarView;
                    snackbar = Snackbar.make(relativeLayout, ""+data.getJSONObject(0).get("msg")+" - Espacio "+data.getJSONObject(0).getString("space"), Snackbar.LENGTH_LONG);
                    snackBarView = snackbar.getView();
                    snackbar.setActionTextColor(Color.WHITE);
                    snackBarView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                    TextView textView = snackBarView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.WHITE);
                    snackbar.show();
                    sendTestEmail();
                    addMarkersToMap();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 3:
                try {
                    Snackbar snackbar;
                    View snackBarView;
                    snackbar = Snackbar.make(relativeLayout, ""+data.getJSONObject(0).get("msg"), Snackbar.LENGTH_LONG);
                    snackBarView = snackbar.getView();
                    snackbar.setActionTextColor(Color.WHITE);
                    snackBarView.setBackgroundColor(getResources().getColor(R.color.colorOrange));
                    TextView textView = snackBarView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.WHITE);
                    snackbar.show();
                    addMarkersToMap();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 0:
                mainL.setAlpha(1);
                progress.setVisibility(View.INVISIBLE);
                Toasty.error(this,"NO FUE POSIBLE CONECTARSE CON EL SERVIDOR");
                break;
        }
    }

    private void AbirDialogAlert(String tvtitle,String idparkin,String cost) {

        String iduser = pref.getString(Constants.ID,"");
        popupReservation = new PopupReservation(ParkingsMapMarkers.this);
        popupReservation.alert_msg(this,tvtitle,idparkin,iduser,cost,true);
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        super.onDestroy();

        ParkingsMapMarkers.this.finish();
    }

    public void toastShare(View view) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,  getResources().getString(R.string.boton_app_compartir));
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void sendTestEmail() {

        String id_user    = pref.getString(Constants.ID,"");
        String name_user  = pref.getString(Constants.NAME, "");
        String email_user = pref.getString(Constants.EMAIL, "");

        BackgroundMail.newBuilder(this)
                .withUsername("noreplyparkingya@gmail.com")
                .withPassword(Constants.PASSWORD_EMAIL)
                .withMailto(email_user)
                .withSubject("CONFIRMACION DE RESERVA")
                .withBody("Su reserva se encuentra a nombre de:\n"
                          +name_user+ " \nNumero de reserva #"+id_user+"."+
                          " \n\n\nGracias por reservar tu parqueadero en Parking Ya!")

                .withOnSuccessCallback(new BackgroundMail.OnSuccessCallback() {
                    @Override
                    public void onSuccess() {
                        //do some magic
                    }
                })
                .withOnFailCallback(new BackgroundMail.OnFailCallback() {
                    @Override
                    public void onFail() {
                        //do some magic
                    }
                })
                .send();
    }

    /**Su funcion es algo similar a lo que se llama cuando se presiona el botón "Forzar Detención" o
     * "Administrar aplicaciones", lo cuál mata la aplicación
     * android.os.Process.killProcess(android.os.Process.myPid());
     *
     * finish(); Si solo quiere mandar la aplicación a segundo plano */
}