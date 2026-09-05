package com.nicue.onetwo.data.dice;

import static org.junit.Assert.assertEquals;

import android.content.Context;
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
public class DiceRepositoryTest {
    private DiceRepository diceRepository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("SHARED_PREFS_FILE", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        diceRepository = new DiceRepository(new DicePrefsDataSource(context));
    }

    @Test
    public void writeAndReadDiceDefinitions_roundTripInPreferences() {
        List<DieRecord> dice =
                Arrays.asList(
                        new DieRecord(6, 0, ""),
                        new DieRecord(10, 1, ""),
                        new DieRecord(20, 2, ""));
        diceRepository.writeDice(dice);

        List<DieRecord> restored = diceRepository.readDice();
        assertEquals(3, restored.size());
        assertEquals(6, restored.get(0).getFaces());
        assertEquals(10, restored.get(1).getFaces());
        assertEquals(20, restored.get(2).getFaces());
    }
}
