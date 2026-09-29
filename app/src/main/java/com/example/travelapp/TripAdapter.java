package com.example.travelapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    public interface OnTripClickListener {
        void onTripClick(Trip trip);

        void onTripLongClick(Trip trip);
    }

    private final List<Trip> trips = new ArrayList<>();
    private final OnTripClickListener listener;

    public TripAdapter(OnTripClickListener listener) {
        this.listener = listener;
    }

    public void setTrips(List<Trip> newTrips) {
        trips.clear();
        trips.addAll(newTrips);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        Context context = holder.itemView.getContext();

        holder.destination.setText(trip.getDestination());
        holder.dates.setText(context.getString(R.string.date_range,
                Trip.formatDate(trip.getStartDate()), Trip.formatDate(trip.getEndDate())));
        String days = context.getResources()
                .getQuantityString(R.plurals.days_count, trip.getDays(), trip.getDays());
        holder.info.setText(context.getString(R.string.trip_info,
                days, trip.getTravelStyle(), trip.getBudget()));

        holder.itemView.setOnClickListener(v -> listener.onTripClick(trip));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onTripLongClick(trip);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        final TextView destination;
        final TextView dates;
        final TextView info;

        TripViewHolder(View itemView) {
            super(itemView);
            destination = itemView.findViewById(R.id.trip_destination);
            dates = itemView.findViewById(R.id.trip_dates);
            info = itemView.findViewById(R.id.trip_info);
        }
    }
}
