package com.example.travelapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// Lokalna SQLite baza: kreiranje tabela i sve CRUD operacije.
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "travel_planner.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_TRIPS = "trips";
    private static final String TABLE_PLAN_ITEMS = "plan_items";

    private static final String CREATE_TRIPS = "CREATE TABLE " + TABLE_TRIPS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "destination TEXT, "
            + "days INTEGER, "
            + "start_date INTEGER, "
            + "travel_style TEXT, "
            + "budget TEXT, "
            + "summary TEXT, "
            + "created_at INTEGER)";

    private static final String CREATE_PLAN_ITEMS = "CREATE TABLE " + TABLE_PLAN_ITEMS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "trip_id INTEGER REFERENCES " + TABLE_TRIPS + "(id) ON DELETE CASCADE, "
            + "day INTEGER, "
            + "time TEXT, "
            + "type TEXT, "
            + "title TEXT, "
            + "description TEXT, "
            + "done INTEGER DEFAULT 0, "
            + "note TEXT)";

    private static DatabaseHelper instance;

    // Jedna instanca za celu aplikaciju, da svi ekrani dele istu konekciju ka bazi.
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // SQLite podrazumevano ne proverava strane ključeve, pa ih ovde uključujemo (zbog CASCADE).
    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TRIPS);
        db.execSQL(CREATE_PLAN_ITEMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAN_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        onCreate(db);
    }

    // Putovanje i sve stavke plana upisujemo u jednoj transakciji: ili sve uspe, ili ništa.
    public long insertTripWithItems(Trip trip, List<PlanItem> items) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long tripId = db.insert(TABLE_TRIPS, null, toValues(trip));
            for (PlanItem item : items) {
                db.insert(TABLE_PLAN_ITEMS, null, toValues(tripId, item));
            }
            db.setTransactionSuccessful();
            return tripId;
        } finally {
            db.endTransaction();
        }
    }

    public List<Trip> getAllTrips() {
        List<Trip> trips = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE_TRIPS, null, null, null,
                null, null, "created_at DESC")) {
            while (cursor.moveToNext()) {
                trips.add(readTrip(cursor));
            }
        }
        return trips;
    }

    public Trip getTrip(long tripId) {
        try (Cursor cursor = getReadableDatabase().query(TABLE_TRIPS, null, "id = ?",
                new String[]{String.valueOf(tripId)}, null, null, null)) {
            return cursor.moveToFirst() ? readTrip(cursor) : null;
        }
    }

    public List<PlanItem> getPlanItems(long tripId) {
        List<PlanItem> items = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE_PLAN_ITEMS, null, "trip_id = ?",
                new String[]{String.valueOf(tripId)}, null, null, "day, time")) {
            while (cursor.moveToNext()) {
                items.add(readPlanItem(cursor));
            }
        }
        return items;
    }

    public void setItemDone(long itemId, boolean done) {
        ContentValues values = new ContentValues();
        values.put("done", done ? 1 : 0);
        updatePlanItem(itemId, values);
    }

    public void updateItemNote(long itemId, String note) {
        ContentValues values = new ContentValues();
        values.put("note", note);
        updatePlanItem(itemId, values);
    }

    // Stavke plana se brišu automatski zahvaljujući ON DELETE CASCADE.
    public void deleteTrip(long tripId) {
        getWritableDatabase().delete(TABLE_TRIPS, "id = ?", new String[]{String.valueOf(tripId)});
    }

    private void updatePlanItem(long itemId, ContentValues values) {
        getWritableDatabase().update(TABLE_PLAN_ITEMS, values, "id = ?",
                new String[]{String.valueOf(itemId)});
    }

    private ContentValues toValues(Trip trip) {
        ContentValues values = new ContentValues();
        values.put("destination", trip.getDestination());
        values.put("days", trip.getDays());
        values.put("start_date", trip.getStartDate());
        values.put("travel_style", trip.getTravelStyle());
        values.put("budget", trip.getBudget());
        values.put("summary", trip.getSummary());
        values.put("created_at", trip.getCreatedAt());
        return values;
    }

    private ContentValues toValues(long tripId, PlanItem item) {
        ContentValues values = new ContentValues();
        values.put("trip_id", tripId);
        values.put("day", item.getDay());
        values.put("time", item.getTime());
        values.put("type", item.getType());
        values.put("title", item.getTitle());
        values.put("description", item.getDescription());
        values.put("done", item.isDone() ? 1 : 0);
        values.put("note", item.getNote());
        return values;
    }

    private Trip readTrip(Cursor cursor) {
        return new Trip(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("destination")),
                cursor.getInt(cursor.getColumnIndexOrThrow("days")),
                cursor.getLong(cursor.getColumnIndexOrThrow("start_date")),
                cursor.getString(cursor.getColumnIndexOrThrow("travel_style")),
                cursor.getString(cursor.getColumnIndexOrThrow("budget")),
                cursor.getString(cursor.getColumnIndexOrThrow("summary")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
    }

    private PlanItem readPlanItem(Cursor cursor) {
        return new PlanItem(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("day")),
                cursor.getString(cursor.getColumnIndexOrThrow("time")),
                cursor.getString(cursor.getColumnIndexOrThrow("type")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getInt(cursor.getColumnIndexOrThrow("done")) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow("note")));
    }
}
