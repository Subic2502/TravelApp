package com.example.travelapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Internet servis: pravi prompt, šalje POST zahtev Gemini API-ju i parsira JSON odgovor.
// Metoda generatePlan blokira dok ne stigne odgovor, zato se poziva samo sa pozadinske niti.
public class GeminiService {

    // Naziv modela; može se promeniti ako Google preimenuje besplatne modele.
    private static final String MODEL = "gemini-3.5-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 90000;
    private static final String CODE_FENCE = "```";

    // Uputstvo za model (nije tekst korisničkog interfejsa, zato nije u strings.xml).
    private static final String PROMPT_INTRO =
            "Ti si iskusan turistički vodič. Napravi plan putovanja po danima na osnovu odgovora korisnika.\n\n"
            + "Odgovori korisnika:\n";
    private static final String PROMPT_RULES = "\nPravila:\n"
            + "- Piši na srpskom jeziku, latinicom.\n"
            + "- Za svaki dan predloži 4 do 6 stavki.\n"
            + "- Prvi dan mora imati stavku tipa SMESTAJ sa preporukom smeštaja.\n"
            + "- Svaki dan mora imati bar jednu stavku tipa HRANA.\n"
            + "- Polje type mora biti tačno jedno od: ZNAMENITOST, HRANA, SMESTAJ, AKTIVNOST.\n"
            + "- Vreme piši u formatu HH:mm.\n"
            + "- Vrati ISKLJUČIVO JSON, bez ikakvog dodatnog teksta, tačno u ovom obliku:\n"
            + "{\"summary\": \"kratak opis putovanja u 2-3 rečenice\", \"items\": ["
            + "{\"day\": 1, \"time\": \"09:00\", \"type\": \"ZNAMENITOST\", "
            + "\"title\": \"...\", \"description\": \"...\"}]}";

    // Rezultat poziva: kratak opis i lista stavki plana.
    public static class PlanResult {
        private final String summary;
        private final List<PlanItem> items;

        PlanResult(String summary, List<PlanItem> items) {
            this.summary = summary;
            this.items = items;
        }

        public String getSummary() {
            return summary;
        }

        public List<PlanItem> getItems() {
            return items;
        }
    }

    public PlanResult generatePlan(String[] questionTitles, String[] answers)
            throws IOException, JSONException {
        String requestBody = buildRequestBody(buildPrompt(questionTitles, answers));
        String response = sendRequest(requestBody);
        return parseResponse(response);
    }

    private String buildPrompt(String[] questionTitles, String[] answers) {
        StringBuilder prompt = new StringBuilder(PROMPT_INTRO);
        for (int i = 0; i < questionTitles.length; i++) {
            prompt.append("- ").append(questionTitles[i])
                    .append(": ").append(answers[i]).append('\n');
        }
        return prompt.append(PROMPT_RULES).toString();
    }

    // Telo zahteva: {"contents":[{"parts":[{"text":"..."}]}], "generationConfig":{...}}
    private String buildRequestBody(String prompt) throws JSONException {
        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject config = new JSONObject().put("responseMimeType", "application/json");
        return new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", config)
                .toString();
    }

    private String sendRequest(String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);
            connection.setDoOutput(true);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode);
            }
            return readStream(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    // Putanja do teksta u odgovoru: candidates[0].content.parts[0].text
    private PlanResult parseResponse(String response) throws JSONException {
        String text = new JSONObject(response)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0)
                .getString("text");

        JSONObject plan = new JSONObject(stripCodeFences(text));
        JSONArray itemsJson = plan.getJSONArray("items");
        List<PlanItem> items = new ArrayList<>();
        for (int i = 0; i < itemsJson.length(); i++) {
            items.add(parseItem(itemsJson.getJSONObject(i)));
        }
        return new PlanResult(plan.getString("summary"), items);
    }

    private PlanItem parseItem(JSONObject json) throws JSONException {
        return new PlanItem(0, json.getInt("day"), json.getString("time"),
                json.getString("type"), json.getString("title"),
                json.getString("description"), false, "");
    }

    // Model ponekad obavije JSON u ```json ... ```, pa te oznake uklanjamo.
    private String stripCodeFences(String text) {
        String result = text.trim();
        if (result.startsWith(CODE_FENCE)) {
            result = result.substring(result.indexOf('\n') + 1);
        }
        if (result.endsWith(CODE_FENCE)) {
            result = result.substring(0, result.length() - CODE_FENCE.length());
        }
        return result.trim();
    }
}
