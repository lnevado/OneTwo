package com.nicue.onetwo.data.dice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DiceRecordPersistenceTest {
    private static final String PREF_FILE = "SHARED_PREFS_FILE";
    private static final String KEY_DICES = "DICES";

    private SharedPreferences preferences;
    private DiceRepository diceRepository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        diceRepository = new DiceRepository(new DicePrefsDataSource(context));
    }

    @Test
    public void readDice_upgradesTheLegacyFlatFaceArray() {
        preferences.edit().putString(KEY_DICES, "[6,20,4]").commit();

        List<DieRecord> dice = diceRepository.readDice();

        assertEquals(3, dice.size());
        assertEquals(6, dice.get(0).getFaces());
        assertEquals(20, dice.get(1).getFaces());
        assertEquals(4, dice.get(2).getFaces());
        // Migrated dice take the colour their creation order would have given them.
        assertEquals(0, dice.get(0).getColorIndex());
        assertEquals(1, dice.get(1).getColorIndex());
        assertEquals(2, dice.get(2).getColorIndex());
        for (DieRecord die : dice) {
            assertTrue("migrated dice carry no label", die.getLabel().isEmpty());
        }
    }

    @Test
    public void writeThenRead_roundTripsColourAndLabel() {
        List<DieRecord> dice =
                Arrays.asList(
                        new DieRecord(20, 3, "dmg"),
                        new DieRecord(6, 7, ""),
                        new DieRecord(8, 1, "Ana's"));

        diceRepository.writeDice(dice);
        List<DieRecord> restored = diceRepository.readDice();

        assertEquals(3, restored.size());
        assertEquals(20, restored.get(0).getFaces());
        assertEquals(3, restored.get(0).getColorIndex());
        assertEquals("dmg", restored.get(0).getLabel());
        assertEquals("", restored.get(1).getLabel());
        assertEquals("Ana's", restored.get(2).getLabel());
        assertEquals(1, restored.get(2).getColorIndex());
    }

    @Test
    public void readDice_clampsAColourIndexItDoesNotRecognise() {
        preferences
                .edit()
                .putString(KEY_DICES, "[{\"faces\":6,\"color\":14,\"label\":\"\"}]")
                .commit();

        assertEquals(4, diceRepository.readDice().get(0).getColorIndex());
    }

    @Test
    public void readDice_survivesRecordsMissingFields() {
        preferences.edit().putString(KEY_DICES, "[{\"faces\":12}]").commit();

        List<DieRecord> dice = diceRepository.readDice();

        assertEquals(1, dice.size());
        assertEquals(12, dice.get(0).getFaces());
        assertEquals("", dice.get(0).getLabel());
    }

    @Test
    public void readDice_skipsOnlyTheUnreadableRecord() {
        preferences
                .edit()
                .putString(
                        KEY_DICES,
                        "[{\"faces\":6,\"color\":1,\"label\":\"a\"},"
                                + "{\"color\":2},"
                                + "{\"faces\":20,\"color\":3,\"label\":\"b\"}]")
                .commit();

        List<DieRecord> dice = diceRepository.readDice();

        assertEquals("one bad record must not discard the others", 2, dice.size());
        assertEquals(6, dice.get(0).getFaces());
        assertEquals(20, dice.get(1).getFaces());
        assertEquals("b", dice.get(1).getLabel());
    }

    @Test
    public void readDice_returnsNothingForMalformedData() {
        preferences.edit().putString(KEY_DICES, "not json at all").commit();

        assertTrue(diceRepository.readDice().isEmpty());
    }
}
