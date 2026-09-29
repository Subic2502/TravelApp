package com.example.travelapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

// Koristi se samo na telefonu; na tabletu se TripDetailFragment prikazuje u MainActivity.
public class TripDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Fragment dodajemo samo prvi put; posle rotacije ga sistem sam vraća.
        if (savedInstanceState == null) {
            long tripId = getIntent().getLongExtra(MainActivity.EXTRA_TRIP_ID, 0);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, TripDetailFragment.newInstance(tripId))
                    .commit();
        }
    }
}
