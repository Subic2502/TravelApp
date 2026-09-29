package com.example.travelapp;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PlanItemAdapter extends RecyclerView.Adapter<PlanItemAdapter.PlanViewHolder> {

    private static final float DONE_ALPHA = 0.5f;

    public interface OnPlanItemListener {
        void onDoneChanged(PlanItem item, boolean done);

        void onItemClick(PlanItem item, int position);
    }

    private final List<PlanItem> items = new ArrayList<>();
    private final OnPlanItemListener listener;

    public PlanItemAdapter(OnPlanItemListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PlanItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        PlanItem item = items.get(position);
        Context context = holder.itemView.getContext();

        // Naslov dana prikazujemo samo kada se dan promeni u odnosu na prethodnu stavku.
        boolean isNewDay = position == 0 || items.get(position - 1).getDay() != item.getDay();
        holder.dayHeader.setVisibility(isNewDay ? View.VISIBLE : View.GONE);
        holder.dayHeader.setText(context.getString(R.string.day_header, item.getDay()));

        holder.icon.setText(iconForType(item.getType()));
        holder.time.setText(item.getTime());
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
        bindNote(holder, item);
        bindDoneState(holder, item.isDone());

        holder.done.setOnClickListener(v -> {
            boolean done = holder.done.isChecked();
            item.setDone(done);
            bindDoneState(holder, done);
            listener.onDoneChanged(item, done);
        });
        holder.card.setOnClickListener(v ->
                listener.onItemClick(item, holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindNote(PlanViewHolder holder, PlanItem item) {
        boolean hasNote = !item.getNote().isEmpty();
        holder.note.setVisibility(hasNote ? View.VISIBLE : View.GONE);
        holder.note.setText(holder.itemView.getContext()
                .getString(R.string.note_label, item.getNote()));
    }

    // Završena stavka: precrtan naslov i prigušena kartica.
    private void bindDoneState(PlanViewHolder holder, boolean done) {
        holder.done.setChecked(done);
        int flags = holder.title.getPaintFlags();
        holder.title.setPaintFlags(done
                ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        holder.card.setAlpha(done ? DONE_ALPHA : 1f);
    }

    private int iconForType(String type) {
        switch (type) {
            case "HRANA":
                return R.string.type_icon_food;
            case "SMESTAJ":
                return R.string.type_icon_accommodation;
            case "AKTIVNOST":
                return R.string.type_icon_activity;
            default:
                return R.string.type_icon_sight;
        }
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        final TextView dayHeader;
        final MaterialCardView card;
        final TextView icon;
        final TextView time;
        final TextView title;
        final TextView description;
        final TextView note;
        final CheckBox done;

        PlanViewHolder(View itemView) {
            super(itemView);
            dayHeader = itemView.findViewById(R.id.day_header);
            card = itemView.findViewById(R.id.plan_card);
            icon = itemView.findViewById(R.id.plan_icon);
            time = itemView.findViewById(R.id.plan_time);
            title = itemView.findViewById(R.id.plan_title);
            description = itemView.findViewById(R.id.plan_description);
            note = itemView.findViewById(R.id.plan_note);
            done = itemView.findViewById(R.id.plan_done);
        }
    }
}
