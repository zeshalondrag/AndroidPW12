package com.example.geoposition;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private MapView mapView;
    private TextView locationText;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private MapObjectCollection mapObjects;
    private PlacemarkMapObject currentLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            MapKitFactory.setApiKey("7984a9f9-430b-45ed-82d4-1a542afe027c");
            MapKitFactory.initialize(this);
            Log.d("MapKit", "MapKit успешно инициализирован");
        } catch (Exception e) {
            Log.e("MapKit", "Ошибка инициализации MapKit: " + e.getMessage());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapview);
        locationText = findViewById(R.id.location_text);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapObjects = mapView.getMap().getMapObjects().addCollection();

        if (!isNetworkAvailable()) {
            locationText.setText("Нет интернет-соединения. Карта не может быть загружена.");
            Log.w("Network", "Нет интернет-соединения");
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult.getLastLocation() != null) {
                    double latitude = locationResult.getLastLocation().getLatitude();
                    double longitude = locationResult.getLastLocation().getLongitude();

                    String locationStr = String.format("Широта: %.6f\nДолгота: %.6f", latitude, longitude);
                    locationText.setText(locationStr);
                    Log.d("Location", "Получено местоположение через LocationCallback: " + locationStr);

                    updateMapLocation(latitude, longitude);
                } else {
                    locationText.setText("Местоположение не определено (LocationCallback)");
                    Log.w("Location", "Местоположение не определено в LocationCallback");
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            getLastKnownLocation();
            startLocationUpdates();
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        String locationStr = String.format("Широта: %.6f\nДолгота: %.6f", latitude, longitude);
                        locationText.setText(locationStr);
                        Log.d("Location", "Получено последнее известное местоположение: " + locationStr);

                        updateMapLocation(latitude, longitude);
                    } else {
                        locationText.setText("Последнее местоположение недоступно");
                        Log.w("Location", "Последнее известное местоположение недоступно");
                    }
                })
                .addOnFailureListener(this, e -> {
                    locationText.setText("Ошибка получения местоположения: " + e.getMessage());
                    Log.e("Location", "Ошибка получения последнего местоположения: " + e.getMessage());
                });
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // Обновление каждые 5 секунд
        locationRequest.setFastestInterval(2000); // Самый быстрый интервал

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            Log.d("Location", "Запрос обновлений местоположения начат");
        } else {
            Log.w("Location", "Нет разрешения на геолокацию");
        }
    }

    private void updateMapLocation(double latitude, double longitude) {
        Point currentPoint = new Point(latitude, longitude);
        mapView.getMap().move(
                new CameraPosition(currentPoint, 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1),
                null
        );

        if (currentLocationMarker == null) {
            currentLocationMarker = mapObjects.addPlacemark(currentPoint);
            currentLocationMarker.setOpacity(0.8f);
        } else {
            currentLocationMarker.setGeometry(currentPoint);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastKnownLocation();
                startLocationUpdates();
                Log.d("Permissions", "Разрешение на геолокацию предоставлено");
            } else {
                locationText.setText("Разрешение на геолокацию не предоставлено");
                Log.w("Permissions", "Разрешение на геолокацию отклонено");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        MapKitFactory.getInstance().onStart();
        Log.d("MapKit", "onStart вызван");
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d("MapKit", "onStop вызван");
        super.onStop();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}