package com.example.travelapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.concurrent.TimeUnit;

// Content Provider: upis putovanja u kalendar uređaja preko ContentResolver-a.
public final class CalendarHelper {

    private static final long NO_CALENDAR = -1;

    private CalendarHelper() {
    }

    // Vraća false ako na uređaju ne postoji nijedan kalendar.
    public static boolean addTripToCalendar(Context context, Trip trip) {
        ContentResolver resolver = context.getContentResolver();
        long calendarId = findFirstCalendarId(resolver);
        if (calendarId == NO_CALENDAR) {
            return false;
        }

        // Celodnevni događaj: DTEND je prvi dan POSLE putovanja (kraj nije uključen).
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.TITLE,
                context.getString(R.string.calendar_event_title, trip.getDestination()));
        values.put(CalendarContract.Events.DESCRIPTION, trip.getSummary());
        values.put(CalendarContract.Events.DTSTART, trip.getStartDate());
        values.put(CalendarContract.Events.DTEND,
                trip.getStartDate() + TimeUnit.DAYS.toMillis(trip.getDays()));
        values.put(CalendarContract.Events.ALL_DAY, 1);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "UTC");

        Uri eventUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values);
        return eventUri != null;
    }

    private static long findFirstCalendarId(ContentResolver resolver) {
        String[] projection = {CalendarContract.Calendars._ID};
        try (Cursor cursor = resolver.query(CalendarContract.Calendars.CONTENT_URI,
                projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return NO_CALENDAR;
    }
}
