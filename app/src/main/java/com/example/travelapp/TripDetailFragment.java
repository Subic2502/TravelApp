package com.example.travelapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class TripDetailFragment extends Fragment implements PlanItemAdapter.OnPlanItemListener {

    private static final String ARG_TRIP_ID = "trip_id";
    private static final String[] CALENDAR_PERMISSIONS = {
            Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};

    private TextView destinationText;
    private TextView datesText;
    private TextView summaryText;
    private View calendarButton;
    private PlanItemAdapter adapter;
    private Trip trip;

    // Traženje dozvola za kalendar u toku rada aplikacije (runtime permissions).
    private final ActivityResultLauncher<String[]> calendarPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> onCalendarPermissionResult());

    public static TripDetailFragment newInstance(long tripId) {
        Bundle args = new Bundle();
        args.putLong(ARG_TRIP_ID, tripId);
        TripDetailFragment fragment = new TripDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public long getTripId() {
        return requireArguments().getLong(ARG_TRIP_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_detail, container, false);
        destinationText = view.findViewById(R.id.detail_destination);
        datesText = view.findViewById(R.id.detail_dates);
        summaryText = view.findViewById(R.id.detail_summary);
        calendarButton = view.findViewById(R.id.calendar_button);
        calendarButton.setOnClickListener(v -> onCalendarClick());

        adapter = new PlanItemAdapter(this);
        RecyclerView recycler = view.findViewById(R.id.plan_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        loadTrip();
        return view;
    }

    private void loadTrip() {
        long tripId = getTripId();
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        AppExecutor.runInBackground(() -> {
            Trip loadedTrip = db.getTrip(tripId);
            List<PlanItem> items = db.getPlanItems(tripId);
            AppExecutor.runOnMainThread(() -> showTrip(loadedTrip, items));
        });
    }

    private void showTrip(Trip loadedTrip, List<PlanItem> items) {
        if (!isAdded()) {
            return;
        }
        if (loadedTrip == null) {
            Toast.makeText(requireContext(), R.string.trip_not_found, Toast.LENGTH_SHORT).show();
            calendarButton.setVisibility(View.GONE);
            return;
        }
        trip = loadedTrip;
        destinationText.setText(trip.getDestination());
        datesText.setText(getString(R.string.date_range,
                Trip.formatDate(trip.getStartDate()), Trip.formatDate(trip.getEndDate())));
        summaryText.setText(trip.getSummary());
        adapter.setItems(items);
    }

    @Override
    public void onDoneChanged(PlanItem item, boolean done) {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        AppExecutor.runInBackground(() -> db.setItemDone(item.getId(), done));
    }

    @Override
    public void onItemClick(PlanItem item, int position) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_note, null);
        EditText noteInput = dialogView.findViewById(R.id.note_input);
        noteInput.setText(item.getNote());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.note_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) ->
                        saveNote(item, position, noteInput.getText().toString().trim()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveNote(PlanItem item, int position, String note) {
        item.setNote(note);
        adapter.notifyItemChanged(position);
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        AppExecutor.runInBackground(() -> db.updateItemNote(item.getId(), note));
    }

    private void onCalendarClick() {
        if (trip == null) {
            return;
        }
        if (hasCalendarPermission()) {
            addToCalendar();
        } else {
            calendarPermissionLauncher.launch(CALENDAR_PERMISSIONS);
        }
    }

    private void onCalendarPermissionResult() {
        if (hasCalendarPermission()) {
            addToCalendar();
        } else {
            Toast.makeText(requireContext(), R.string.calendar_permission_denied,
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasCalendarPermission() {
        for (String permission : CALENDAR_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Upis u kalendar ide preko Content Provider-a, pa ga radimo na pozadinskoj niti.
    private void addToCalendar() {
        Context appContext = requireContext().getApplicationContext();
        Trip tripToAdd = trip;
        AppExecutor.runInBackground(() -> {
            boolean added = CalendarHelper.addTripToCalendar(appContext, tripToAdd);
            AppExecutor.runOnMainThread(() -> Toast.makeText(appContext,
                    added ? R.string.calendar_added : R.string.calendar_not_found,
                    Toast.LENGTH_LONG).show());
        });
    }
}
