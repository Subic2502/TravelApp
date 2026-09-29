package com.example.travelapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "trip_id";

    private static final long NO_TRIP = -1;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Android sam bira layout/activity_main.xml ili layout-sw600dp/activity_main.xml.
        setContentView(R.layout.activity_main);

        findViewById(R.id.fab_new_trip).setOnClickListener(v ->
                startActivity(new Intent(this, QuestionnaireActivity.class)));

        requestNotificationPermission();
        if (savedInstanceState == null) {
            openTripFromIntent(getIntent());
        }
    }

    // Poziva se kada se već otvorena MainActivity pokrene ponovo (notifikacija, novi plan).
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        openTripFromIntent(intent);
    }

    // Detalji postoje samo u tablet layout-u, pa tako znamo na kom uređaju radimo.
    private boolean isTablet() {
        return findViewById(R.id.detail_container) != null;
    }

    public void openTrip(long tripId) {
        if (isTablet()) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, TripDetailFragment.newInstance(tripId))
                    .commit();
        } else {
            Intent intent = new Intent(this, TripDetailActivity.class);
            intent.putExtra(EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        }
    }

    // Na tabletu uklanjamo detalje ako je obrisano putovanje koje je trenutno prikazano.
    public void closeTripIfShown(long tripId) {
        Fragment detail = getSupportFragmentManager().findFragmentById(R.id.detail_container);
        if (detail instanceof TripDetailFragment
                && ((TripDetailFragment) detail).getTripId() == tripId) {
            getSupportFragmentManager().beginTransaction().remove(detail).commit();
        }
    }

    private void openTripFromIntent(Intent intent) {
        long tripId = intent.getLongExtra(EXTRA_TRIP_ID, NO_TRIP);
        if (tripId != NO_TRIP) {
            openTrip(tripId);
        }
    }

    // Od Androida 13 dozvola za notifikacije se traži u toku rada aplikacije.
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }
}
