package com.example.travelapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Upitnik: jedna aktivnost i jedan layout; sadržaj se menja za svako pitanje.
public class QuestionnaireActivity extends AppCompatActivity {

    private static final String KEY_INDEX = "current_index";
    private static final String KEY_ANSWERS = "answers";
    private static final String KEY_START_DATE = "start_date";
    private static final String DATE_PICKER_TAG = "date_picker";
    private static final long ANIMATION_DURATION_MS = 250;
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 14;

    // Redni brojevi pitanja čiji se odgovori čuvaju i u bazi.
    private static final int Q_DESTINATION = 0;
    private static final int Q_DAYS = 1;
    private static final int Q_STYLE = 3;
    private static final int Q_BUDGET = 4;

    private List<Question> questions;
    private String[] answers;
    private int currentIndex;
    private long startDateMillis;

    private View contentLayout;
    private View loadingLayout;
    private View questionContainer;
    private TextView progressText;
    private LinearProgressIndicator progressIndicator;
    private TextView titleText;
    private TextView descriptionText;
    private View answerLayout;
    private EditText answerInput;
    private MaterialButton dateButton;
    private LinearLayout choicesLayout;
    private MaterialButton backButton;
    private MaterialButton nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);
        bindViews();

        questions = createQuestions();
        answers = new String[questions.size()];
        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(KEY_INDEX);
            answers = savedInstanceState.getStringArray(KEY_ANSWERS);
            startDateMillis = savedInstanceState.getLong(KEY_START_DATE);
        }

        progressIndicator.setMax(questions.size());
        setupListeners();
        showQuestion();
    }

    // Čuvamo napredak da se ne izgubi pri rotaciji ekrana.
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_INDEX, currentIndex);
        outState.putStringArray(KEY_ANSWERS, answers);
        outState.putLong(KEY_START_DATE, startDateMillis);
    }

    private void bindViews() {
        contentLayout = findViewById(R.id.questionnaire_content);
        loadingLayout = findViewById(R.id.loading_layout);
        questionContainer = findViewById(R.id.question_container);
        progressText = findViewById(R.id.progress_text);
        progressIndicator = findViewById(R.id.progress_indicator);
        titleText = findViewById(R.id.question_title);
        descriptionText = findViewById(R.id.question_description);
        answerLayout = findViewById(R.id.answer_layout);
        answerInput = findViewById(R.id.answer_input);
        dateButton = findViewById(R.id.date_button);
        choicesLayout = findViewById(R.id.choices_layout);
        backButton = findViewById(R.id.back_button);
        nextButton = findViewById(R.id.next_button);
    }

    private List<Question> createQuestions() {
        List<Question> list = new ArrayList<>();
        list.add(new Question(Question.Type.TEXT, getString(R.string.q_destination_title),
                getString(R.string.q_destination_description)));
        list.add(new Question(Question.Type.NUMBER, getString(R.string.q_days_title),
                getString(R.string.q_days_description)));
        list.add(new Question(Question.Type.DATE, getString(R.string.q_date_title),
                getString(R.string.q_date_description)));
        list.add(createChoiceQuestion(R.string.q_style_title, R.string.q_style_description,
                R.array.q_style_options));
        list.add(createChoiceQuestion(R.string.q_budget_title, R.string.q_budget_description,
                R.array.q_budget_options));
        list.add(createChoiceQuestion(R.string.q_food_title, R.string.q_food_description,
                R.array.q_food_options));
        list.add(createChoiceQuestion(R.string.q_accommodation_title,
                R.string.q_accommodation_description, R.array.q_accommodation_options));
        list.add(createChoiceQuestion(R.string.q_company_title, R.string.q_company_description,
                R.array.q_company_options));
        return list;
    }

    private Question createChoiceQuestion(int titleRes, int descriptionRes, int optionsRes) {
        return new Question(Question.Type.CHOICE, getString(titleRes), getString(descriptionRes),
                getResources().getStringArray(optionsRes));
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> goToQuestion(currentIndex - 1));
        nextButton.setOnClickListener(v -> onNextClick());
        dateButton.setOnClickListener(v -> showDatePicker());
        answerInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                answers[currentIndex] = s.toString().trim();
                updateNextButton();
            }
        });
    }

    private void onNextClick() {
        if (currentIndex == questions.size() - 1) {
            startGeneration();
        } else {
            goToQuestion(currentIndex + 1);
        }
    }

    private void goToQuestion(int newIndex) {
        boolean forward = newIndex > currentIndex;
        currentIndex = newIndex;
        hideKeyboard();
        showQuestion();
        animateQuestion(forward);
    }

    // Prikazuje trenutno pitanje: vidljiv je samo ulaz koji odgovara tipu pitanja.
    private void showQuestion() {
        Question question = questions.get(currentIndex);
        progressText.setText(getString(R.string.question_progress,
                currentIndex + 1, questions.size()));
        progressIndicator.setProgressCompat(currentIndex + 1, true);
        titleText.setText(question.getTitle());
        descriptionText.setText(question.getDescription());

        Question.Type type = question.getType();
        boolean isTextInput = type == Question.Type.TEXT || type == Question.Type.NUMBER;
        answerLayout.setVisibility(isTextInput ? View.VISIBLE : View.GONE);
        dateButton.setVisibility(type == Question.Type.DATE ? View.VISIBLE : View.GONE);
        choicesLayout.setVisibility(type == Question.Type.CHOICE ? View.VISIBLE : View.GONE);

        if (isTextInput) {
            showTextInput(type);
        } else if (type == Question.Type.DATE) {
            showDateAnswer();
        } else {
            showChoices(question);
        }

        backButton.setEnabled(currentIndex > 0);
        boolean isLast = currentIndex == questions.size() - 1;
        nextButton.setText(isLast ? R.string.generate_plan : R.string.next);
        updateNextButton();
    }

    private void showTextInput(Question.Type type) {
        answerInput.setInputType(type == Question.Type.NUMBER
                ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        String answer = answers[currentIndex];
        answerInput.setText(answer == null ? "" : answer);
        answerInput.setSelection(answerInput.length());
    }

    private void showDateAnswer() {
        String answer = answers[currentIndex];
        dateButton.setText(answer == null ? getString(R.string.choose_date) : answer);
    }

    // Svaka opcija je velika kartica; izabrana kartica je označena (checked).
    private void showChoices(Question question) {
        choicesLayout.removeAllViews();
        for (String option : question.getOptions()) {
            MaterialCardView card = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.item_choice, choicesLayout, false);
            TextView optionText = card.findViewById(R.id.choice_text);
            optionText.setText(option);
            card.setChecked(option.equals(answers[currentIndex]));
            card.setOnClickListener(v -> selectChoice(question, option));
            choicesLayout.addView(card);
        }
    }

    private void selectChoice(Question question, String option) {
        answers[currentIndex] = option;
        showChoices(question);
        updateNextButton();
    }

    // MaterialDatePicker radi u UTC; DateValidatorPointForward ne dozvoljava prošle datume.
    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.q_date_title)
                .setCalendarConstraints(constraints)
                .setSelection(startDateMillis > 0
                        ? startDateMillis : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(this::onDateSelected);
        picker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
    }

    private void onDateSelected(Long selection) {
        startDateMillis = selection;
        answers[currentIndex] = Trip.formatDate(selection);
        showDateAnswer();
        updateNextButton();
    }

    private void updateNextButton() {
        nextButton.setEnabled(isAnswered(currentIndex));
    }

    private boolean isAnswered(int index) {
        String answer = answers[index];
        if (answer == null || answer.isEmpty()) {
            return false;
        }
        if (questions.get(index).getType() == Question.Type.NUMBER) {
            int days = parseDays(answer);
            return days >= MIN_DAYS && days <= MAX_DAYS;
        }
        return true;
    }

    private int parseDays(String answer) {
        try {
            return Integer.parseInt(answer);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Kratka animacija: novo pitanje klizi sa strane i postepeno se pojavljuje.
    private void animateQuestion(boolean forward) {
        float offset = getResources().getDimension(R.dimen.question_slide_offset);
        questionContainer.setAlpha(0f);
        questionContainer.setTranslationX(forward ? offset : -offset);
        questionContainer.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(ANIMATION_DURATION_MS)
                .start();
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        imm.hideSoftInputFromWindow(answerInput.getWindowToken(), 0);
    }

    // Pokreće pozadinski zadatak. Glavna nit ostaje slobodna i prikazuje učitavanje.
    private void startGeneration() {
        showLoading(true);
        Context appContext = getApplicationContext();
        String[] answersCopy = answers.clone();
        long startDate = startDateMillis;

        AppExecutor.runInBackground(() -> {
            try {
                long tripId = generateAndSavePlan(appContext, answersCopy, startDate);
                AppExecutor.runOnMainThread(() -> onPlanReady(tripId));
            } catch (IOException e) {
                AppExecutor.runOnMainThread(() -> showError(R.string.error_network));
            } catch (JSONException e) {
                AppExecutor.runOnMainThread(() -> showError(R.string.error_response));
            }
        });
    }

    // Izvršava se na pozadinskoj niti: poziv Gemini API-ja, upis u bazu i notifikacija.
    // Zato je plan sačuvan čak i ako korisnik u međuvremenu napusti ekran.
    private long generateAndSavePlan(Context context, String[] answers, long startDate)
            throws IOException, JSONException {
        GeminiService.PlanResult plan = new GeminiService().generatePlan(questions, answers);
        Trip trip = new Trip(0, answers[Q_DESTINATION], Integer.parseInt(answers[Q_DAYS]),
                startDate, answers[Q_STYLE], answers[Q_BUDGET], plan.getSummary(),
                System.currentTimeMillis());
        long tripId = DatabaseHelper.getInstance(context).insertTripWithItems(trip, plan.getItems());
        NotificationHelper.showPlanReady(context, tripId, trip.getDestination());
        return tripId;
    }

    private void onPlanReady(long tripId) {
        if (isDestroyed()) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TRIP_ID, tripId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showError(int messageRes) {
        if (isDestroyed()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error_title)
                .setMessage(messageRes)
                .setCancelable(false)
                .setPositiveButton(R.string.try_again, (dialog, which) -> startGeneration())
                .setNegativeButton(R.string.cancel, (dialog, which) -> showLoading(false))
                .show();
    }

    // Dok traje generisanje zaključavamo orijentaciju, da se aktivnost ne bi ponovo kreirala.
    private void showLoading(boolean loading) {
        contentLayout.setVisibility(loading ? View.GONE : View.VISIBLE);
        loadingLayout.setVisibility(loading ? View.VISIBLE : View.GONE);
        setRequestedOrientation(loading
                ? ActivityInfo.SCREEN_ORIENTATION_LOCKED
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
