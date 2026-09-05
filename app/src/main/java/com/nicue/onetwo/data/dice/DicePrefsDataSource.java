package com.nicue.onetwo.data.dice;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DicePrefsDataSource {
    private static final String PREF_FILE = "SHARED_PREFS_FILE";
    private static final String KEY_DICES = "DICES";
    private static final String FIELD_FACES = "faces";
    private static final String FIELD_COLOR = "color";
    private static final String FIELD_LABEL = "label";

    /** Kept in step with the palette in {@code DiceAdapter}. */
    public static final int COLOR_COUNT = 10;

    private final SharedPreferences sharedPreferences;

    public DicePrefsDataSource(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Reads the stored dice, upgrading the older format in place. That format was a flat array of
     * face counts, so migrated dice take the colour their creation order would have given them and
     * carry no label.
     */
    public List<DieRecord> readDice() {
        String rawValue = sharedPreferences.getString(KEY_DICES, "");
        ArrayList<DieRecord> dice = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(rawValue);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject record = jsonArray.optJSONObject(i);
                if (record == null) {
                    // Legacy entries are bare face counts.
                    int faces = jsonArray.optInt(i, 0);
                    if (faces > 0) {
                        dice.add(new DieRecord(faces, colorIndexFor(i), ""));
                    }
                    continue;
                }
                // Skip only the unreadable record. Losing one die beats losing the whole set.
                int faces = record.optInt(FIELD_FACES, 0);
                if (faces <= 0) {
                    continue;
                }
                dice.add(
                        new DieRecord(
                                faces,
                                colorIndexFor(record.optInt(FIELD_COLOR, i)),
                                record.optString(FIELD_LABEL, "")));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return dice;
    }

    public void writeDice(List<DieRecord> dice) {
        JSONArray jsonArray = new JSONArray();
        for (DieRecord die : dice) {
            JSONObject record = new JSONObject();
            try {
                record.put(FIELD_FACES, die.getFaces());
                record.put(FIELD_COLOR, die.getColorIndex());
                record.put(FIELD_LABEL, die.getLabel());
            } catch (JSONException ignored) {
                continue;
            }
            jsonArray.put(record);
        }
        sharedPreferences.edit().putString(KEY_DICES, jsonArray.toString()).apply();
    }

    /** Wraps rather than rejects, so a value written by another build still yields a colour. */
    private static int colorIndexFor(int storedIndex) {
        int wrapped = storedIndex % COLOR_COUNT;
        return wrapped < 0 ? wrapped + COLOR_COUNT : wrapped;
    }
}
