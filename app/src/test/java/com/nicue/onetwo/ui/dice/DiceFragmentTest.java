package com.nicue.onetwo.ui.dice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DiceFragmentTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        OneTwoApplication app = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void launchFragment_showsEmptyStateInitially() {
        FragmentScenario<DiceFragment> scenario =
                FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    View emptyText = fragment.getView().findViewById(R.id.tv_dice_empty);
                    assertEquals(View.VISIBLE, emptyText.getVisibility());

                    RecyclerView recyclerView =
                            fragment.getView().findViewById(R.id.recyclerview_dice);
                    assertEquals(0, recyclerView.getAdapter().getItemCount());

                    TextView totalText = fragment.getView().findViewById(R.id.tv_dice_total);
                    assertEquals("0", totalText.getText().toString());
                });
    }

    @Test
    public void addDieViaViewModel_updatesUI() {
        FragmentScenario<DiceFragment> scenario =
                FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    DiceViewModel viewModel =
                            new androidx.lifecycle.ViewModelProvider(fragment)
                                    .get(DiceViewModel.class);
                    // Clear first just in case
                    while (!viewModel.getUiState().getValue().getDice().isEmpty()) {
                        viewModel.removeDie(0);
                    }
                    viewModel.addDie(20);
                });

        scenario.onFragment(
                fragment -> {
                    RecyclerView recyclerView =
                            fragment.getView().findViewById(R.id.recyclerview_dice);
                    assertEquals(1, recyclerView.getAdapter().getItemCount());

                    View emptyText = fragment.getView().findViewById(R.id.tv_dice_empty);
                    assertEquals(View.GONE, emptyText.getVisibility());
                });
    }

    @Test
    public void lockButton_togglesLockStateAndUpdatesTile() {
        FragmentScenario<DiceFragment> scenario =
                FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    DiceViewModel viewModel =
                            new androidx.lifecycle.ViewModelProvider(fragment)
                                    .get(DiceViewModel.class);
                    while (!viewModel.getUiState().getValue().getDice().isEmpty()) {
                        viewModel.removeDie(0);
                    }
                    viewModel.addDie(6);

                    RecyclerView recyclerView =
                            fragment.getView().findViewById(R.id.recyclerview_dice);
                    layOut(recyclerView);

                    View tile = recyclerView.getChildAt(0);
                    assertNotNull(tile);
                    ImageButton lockButton = tile.findViewById(R.id.btn_lock);
                    assertEquals(
                            fragment.getString(R.string.content_desc_lock_die),
                            lockButton.getContentDescription().toString());

                    lockButton.performClick();

                    DiceUiState locked = viewModel.getUiState().getValue();
                    assertTrue(locked.getDice().get(0).isLocked());
                    assertTrue(locked.hasLockedDice());
                    assertFalse(locked.hasRollableDice());

                    layOut(recyclerView);
                    ImageButton lockedButton =
                            recyclerView.getChildAt(0).findViewById(R.id.btn_lock);
                    assertEquals(
                            fragment.getString(R.string.content_desc_unlock_die),
                            lockedButton.getContentDescription().toString());

                    lockedButton.performClick();

                    assertFalse(viewModel.getUiState().getValue().getDice().get(0).isLocked());
                });
    }

    private static void layOut(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 1080, 1920);
    }
}
