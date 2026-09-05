package com.nicue.onetwo.ui.dice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import androidx.test.core.app.ApplicationProvider;
import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.data.dice.DicePrefsDataSource;
import com.nicue.onetwo.data.dice.DiceRepository;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DiceAppearanceViewModelTest {
    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private DiceRepository repository;
    private DiceViewModel viewModel;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("SHARED_PREFS_FILE", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        repository = new DiceRepository(new DicePrefsDataSource(context));
        viewModel = new DiceViewModel(repository, new SavedStateHandle());
    }

    @Test
    public void addDie_assignsPaletteColoursInCreationOrder() throws Exception {
        for (int i = 0; i < 12; i++) {
            viewModel.addDie(6);
        }

        List<DieUiModel> dice = state().getDice();
        for (int i = 0; i < 10; i++) {
            assertEquals("die " + i, i, dice.get(i).getColorIndex());
        }
        assertEquals("palette wraps after ten", 0, dice.get(10).getColorIndex());
        assertEquals(1, dice.get(11).getColorIndex());
    }

    @Test
    public void setDieAppearance_appliesColourAndLabel() throws Exception {
        viewModel.addDie(20);

        viewModel.setDieAppearance(0, 5, "dmg");

        assertEquals(5, dieAt(0).getColorIndex());
        assertEquals("dmg", dieAt(0).getLabel());
    }

    @Test
    public void setDieAppearance_capsAndTrimsTheLabel() throws Exception {
        viewModel.addDie(6);

        viewModel.setDieAppearance(0, 0, "   a very long label indeed   ");

        assertTrue(
                "label should be capped: " + dieAt(0).getLabel(),
                dieAt(0).getLabel().length() <= DiceViewModel.MAX_LABEL_LENGTH);
        assertEquals("a very long", dieAt(0).getLabel());
        assertEquals(
                "truncation must not leave a trailing space",
                dieAt(0).getLabel().trim(),
                dieAt(0).getLabel());
    }

    @Test
    public void setDieAppearance_withBlankLabelClearsIt() throws Exception {
        viewModel.addDie(6);
        viewModel.setDieAppearance(0, 2, "dmg");

        viewModel.setDieAppearance(0, 2, "   ");

        assertEquals("", dieAt(0).getLabel());
        assertEquals("clearing the label leaves the colour alone", 2, dieAt(0).getColorIndex());
    }

    @Test
    public void appearanceFollowsItsDieWhenAnEarlierDieIsRemoved() throws Exception {
        viewModel.addDie(4);
        viewModel.addDie(6);
        viewModel.addDie(20);
        viewModel.setDieAppearance(2, 7, "keep");

        viewModel.removeDie(0);

        assertEquals(2, state().getDice().size());
        assertEquals(20, dieAt(1).getFaces());
        assertEquals(7, dieAt(1).getColorIndex());
        assertEquals("keep", dieAt(1).getLabel());
        assertEquals("", dieAt(0).getLabel());
    }

    @Test
    public void appearanceIsPersistedForTheNextSession() throws Exception {
        viewModel.addDie(6);
        viewModel.addDie(20);
        viewModel.setDieAppearance(1, 8, "boom");

        DiceViewModel restored = new DiceViewModel(repository, new SavedStateHandle());

        List<DieUiModel> dice = LiveDataTestUtil.getValue(restored.getUiState()).getDice();
        assertEquals(2, dice.size());
        assertEquals(20, dice.get(1).getFaces());
        assertEquals(8, dice.get(1).getColorIndex());
        assertEquals("boom", dice.get(1).getLabel());
    }

    @Test
    public void appearanceSurvivesASavedStateHandleRoundTrip() throws Exception {
        SavedStateHandle handle = new SavedStateHandle();
        DiceViewModel first = new DiceViewModel(repository, handle);
        first.addDie(6);
        first.setDieAppearance(0, 4, "rot");

        DiceViewModel restored = new DiceViewModel(repository, handle);

        DieUiModel die = LiveDataTestUtil.getValue(restored.getUiState()).getDice().get(0);
        assertEquals(4, die.getColorIndex());
        assertEquals("rot", die.getLabel());
    }

    private DiceUiState state() throws Exception {
        return LiveDataTestUtil.getValue(viewModel.getUiState());
    }

    private DieUiModel dieAt(int position) throws Exception {
        return state().getDice().get(position);
    }
}
