package com.example.travelapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class TripListFragment extends Fragment implements TripAdapter.OnTripClickListener {

    private TripAdapter adapter;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_list, container, false);
        emptyText = view.findViewById(R.id.empty_text);

        adapter = new TripAdapter(this);
        RecyclerView recycler = view.findViewById(R.id.trips_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        return view;
    }

    // Listu osvežavamo svaki put kada se ekran vrati (npr. posle kreiranja novog putovanja).
    @Override
    public void onResume() {
        super.onResume();
        loadTrips();
    }

    private void loadTrips() {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        AppExecutor.runInBackground(() -> {
            List<Trip> trips = db.getAllTrips();
            AppExecutor.runOnMainThread(() -> showTrips(trips));
        });
    }

    private void showTrips(List<Trip> trips) {
        if (!isAdded()) {
            return;
        }
        adapter.setTrips(trips);
        emptyText.setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTripClick(Trip trip) {
        ((MainActivity) requireActivity()).openTrip(trip.getId());
    }

    @Override
    public void onTripLongClick(Trip trip) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_trip_title)
                .setMessage(getString(R.string.delete_trip_message, trip.getDestination()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTrip(trip.getId()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteTrip(long tripId) {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        AppExecutor.runInBackground(() -> {
            db.deleteTrip(tripId);
            AppExecutor.runOnMainThread(() -> onTripDeleted(tripId));
        });
    }

    private void onTripDeleted(long tripId) {
        if (!isAdded()) {
            return;
        }
        ((MainActivity) requireActivity()).closeTripIfShown(tripId);
        loadTrips();
    }
}
