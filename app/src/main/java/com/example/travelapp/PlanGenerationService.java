package com.example.travelapp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.io.IOException;

// Foreground servis: generisanje plana nastavlja da radi i kada korisnik napusti aplikaciju.
// Android pozadinskim aplikacijama blokira internet i zamrzava ih, ali ne i aplikaciji
// koja ima foreground servis (zato on mora da prikazuje notifikaciju dok radi).
public class PlanGenerationService extends Service {

    public interface Listener {
        void onPlanReady(long tripId);

        void onPlanFailed(int messageRes);
    }

    private static final String EXTRA_QUESTION_TITLES = "question_titles";
    private static final String EXTRA_ANSWERS = "answers";
    private static final String EXTRA_START_DATE = "start_date";
    private static final long NO_TRIP = -1;
    private static final int NO_ERROR = 0;

    // Redni brojevi pitanja čiji se odgovori čuvaju u bazi.
    private static final int Q_DESTINATION = 0;
    private static final int Q_DAYS = 1;
    private static final int Q_STYLE = 3;
    private static final int Q_BUDGET = 4;

    // Statička polja menjamo samo na glavnoj niti, pa nema istovremenog pristupa.
    // Rezultat se čuva dok ga aktivnost ne preuzme (npr. ako se korisnik vrati kasnije).
    private static Listener listener;
    private static long pendingTripId = NO_TRIP;
    private static int pendingErrorRes = NO_ERROR;

    public static void start(Context context, String[] questionTitles, String[] answers,
                             long startDate) {
        pendingTripId = NO_TRIP;
        pendingErrorRes = NO_ERROR;
        Intent intent = new Intent(context, PlanGenerationService.class)
                .putExtra(EXTRA_QUESTION_TITLES, questionTitles)
                .putExtra(EXTRA_ANSWERS, answers)
                .putExtra(EXTRA_START_DATE, startDate);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void setListener(Listener newListener) {
        listener = newListener;
        if (listener != null) {
            deliverPendingResult();
        }
    }

    // ServiceCompat na Androidu starijem od 10 sam ignoriše tip servisa, pa je konstanta bezbedna.
    @SuppressLint("InlinedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ServiceCompat.startForeground(this, NotificationHelper.PROGRESS_NOTIFICATION_ID,
                NotificationHelper.createProgressNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        String[] questionTitles = intent.getStringArrayExtra(EXTRA_QUESTION_TITLES);
        String[] answers = intent.getStringArrayExtra(EXTRA_ANSWERS);
        long startDate = intent.getLongExtra(EXTRA_START_DATE, 0);
        AppExecutor.runInBackground(() -> generatePlan(questionTitles, answers, startDate));
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Izvršava se na pozadinskoj niti: poziv Gemini API-ja, upis u bazu i notifikacija.
    private void generatePlan(String[] questionTitles, String[] answers, long startDate) {
        try {
            long tripId = generateAndSavePlan(questionTitles, answers, startDate);
            AppExecutor.runOnMainThread(() -> {
                pendingTripId = tripId;
                deliverPendingResult();
            });
        } catch (IOException e) {
            postError(R.string.error_network);
        } catch (JSONException e) {
            postError(R.string.error_response);
        } finally {
            stopSelf();
        }
    }

    private long generateAndSavePlan(String[] questionTitles, String[] answers, long startDate)
            throws IOException, JSONException {
        GeminiService.PlanResult plan = new GeminiService().generatePlan(questionTitles, answers);
        Trip trip = new Trip(0, answers[Q_DESTINATION], Integer.parseInt(answers[Q_DAYS]),
                startDate, answers[Q_STYLE], answers[Q_BUDGET], plan.getSummary(),
                System.currentTimeMillis());
        long tripId = DatabaseHelper.getInstance(this).insertTripWithItems(trip, plan.getItems());
        NotificationHelper.showPlanReady(this, tripId, trip.getDestination());
        return tripId;
    }

    private void postError(int messageRes) {
        AppExecutor.runOnMainThread(() -> {
            pendingErrorRes = messageRes;
            deliverPendingResult();
        });
    }

    // Ako aktivnost trenutno ne sluša, rezultat ostaje sačuvan do sledećeg setListener poziva.
    private static void deliverPendingResult() {
        if (listener == null) {
            return;
        }
        if (pendingTripId != NO_TRIP) {
            long tripId = pendingTripId;
            pendingTripId = NO_TRIP;
            listener.onPlanReady(tripId);
        } else if (pendingErrorRes != NO_ERROR) {
            int messageRes = pendingErrorRes;
            pendingErrorRes = NO_ERROR;
            listener.onPlanFailed(messageRes);
        }
    }
}
