package com.nicue.onetwo.ui.dice;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import com.nicue.onetwo.databinding.DiceAlertDialogBinding;
import com.nicue.onetwo.databinding.DiceAppearanceDialogBinding;
import com.nicue.onetwo.databinding.DiceLayoutBinding;
import java.util.List;

public class DiceFragment extends Fragment implements DiceAdapter.Listener, MenuProvider {
    private DiceLayoutBinding binding;
    private DiceAdapter adapter;
    private DiceViewModel viewModel;
    private boolean hasLockedDice;
    private AlertDialog appearanceDialog;
    private boolean hasRollableDice;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DiceLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new DiceAdapter(this);
        binding.recyclerviewDice.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerviewDice.setAdapter(adapter);

        binding.fabDice.setScaleX(0f);
        binding.fabDice.setScaleY(0f);
        // Posted on the view so it is dropped automatically if the view goes away, and the end
        // action pins the final scale: nothing else in the app ever resets it, so an entrance
        // animation that does not finish would leave the button invisible for good.
        binding.fabDice.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (binding == null) {
                            return;
                        }
                        binding.fabDice
                                .animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setInterpolator(new DecelerateInterpolator(2))
                                .withEndAction(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (binding != null) {
                                                    binding.fabDice.setScaleX(1f);
                                                    binding.fabDice.setScaleY(1f);
                                                }
                                            }
                                        })
                                .start();
                    }
                },
                300);
        binding.fabDice.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddDieDialog();
                    }
                });

        DiceViewModelFactory factory =
                new DiceViewModelFactory(
                        ((OneTwoApplication) requireActivity().getApplication())
                                .getAppContainer()
                                .getDiceRepository());
        viewModel = new ViewModelProvider(this, factory).get(DiceViewModel.class);
        viewModel
                .getUiState()
                .observe(
                        getViewLifecycleOwner(),
                        new androidx.lifecycle.Observer<DiceUiState>() {
                            @Override
                            public void onChanged(DiceUiState state) {
                                adapter.submitList(state.getDice());
                                renderResultSummary(state);
                                updateLockActionState(state);
                            }
                        });

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onDestroyView() {
        // Rotating with this open would otherwise leak the window along with the activity.
        if (appearanceDialog != null) {
            appearanceDialog.dismiss();
            appearanceDialog = null;
        }
        if (binding != null) {
            binding.fabDice.animate().cancel();
            binding.diceSummaryCard.animate().cancel();
            binding.recyclerviewDice.setAdapter(null);
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.dice_actions, menu);
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem unlockAll = menu.findItem(R.id.action_unlock_all);
        if (unlockAll != null) {
            unlockAll.setVisible(hasLockedDice);
        }
        MenuItem rollAll = menu.findItem(R.id.action_roll_all);
        if (rollAll != null) {
            rollAll.setEnabled(hasRollableDice);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_unlock_all) {
            viewModel.unlockAllDice();
            return true;
        }
        if (menuItem.getItemId() == R.id.action_roll_all) {
            if (!hasRollableDice) {
                return true;
            }
            vibrate(new long[] {0, 15, 10, 15, 10, 15, 10, 15});
            animateSummaryCard();
            adapter.animateAllVisibleItems(
                    binding.recyclerviewDice,
                    new Runnable() {
                        @Override
                        public void run() {
                            if (binding != null) {
                                viewModel.rollAllDice();
                            }
                        }
                    });
            return true;
        }
        return false;
    }

    private void animateSummaryCard() {
        binding.diceSummaryCard
                .animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .translationZ(8f)
                .setDuration(150)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (binding == null) {
                                    return;
                                }
                                binding.diceSummaryCard
                                        .animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .translationZ(0f)
                                        .setDuration(150)
                                        .start();
                            }
                        })
                .start();
    }

    @Override
    public void onRollDie(int position) {
        if (binding == null) {
            return;
        }
        vibrate(new long[] {0, 15, 10, 15, 10, 15});
        viewModel.rollDie(position);
    }

    @Override
    public void onRemoveDie(int position) {
        viewModel.removeDie(position);
    }

    @Override
    public void onCustomiseDie(int position) {
        if (binding == null) {
            return;
        }
        DiceUiState state = viewModel.getUiState().getValue();
        if (state == null || position < 0 || position >= state.getDice().size()) {
            return;
        }
        showAppearanceDialog(position, state.getDice().get(position));
    }

    private void showAppearanceDialog(final int position, DieUiModel die) {
        final DiceAppearanceDialogBinding dialogBinding =
                DiceAppearanceDialogBinding.inflate(getLayoutInflater());
        final int[] selectedColor = {die.getColorIndex()};
        dialogBinding.etDieLabel.setText(die.getLabel());
        dialogBinding.etDieLabel.setSelection(die.getLabel().length());

        final View[] swatches = new View[DiceAdapter.DICE_COLORS.length];
        for (int i = 0; i < DiceAdapter.DICE_COLORS.length; i++) {
            final int colorIndex = i;
            View swatch = buildColorSwatch(colorIndex);
            swatch.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedColor[0] = colorIndex;
                            for (int j = 0; j < swatches.length; j++) {
                                markSwatchSelected(swatches[j], j == colorIndex);
                            }
                        }
                    });
            swatches[i] = swatch;
            markSwatchSelected(swatch, i == selectedColor[0]);
            dialogBinding.gridDiceColors.addView(swatch);
        }

        appearanceDialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dice_appearance_title)
                        .setView(dialogBinding.getRoot())
                        .setPositiveButton(
                                R.string.save,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Editable label = dialogBinding.etDieLabel.getText();
                                        viewModel.setDieAppearance(
                                                position,
                                                selectedColor[0],
                                                label == null ? "" : label.toString());
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
    }

    private View buildColorSwatch(int colorIndex) {
        int size = Math.round(44 * getResources().getDisplayMetrics().density);
        int margin = Math.round(4 * getResources().getDisplayMetrics().density);

        View swatch = new View(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(margin, margin, margin, margin);
        swatch.setLayoutParams(params);
        swatch.setContentDescription(
                getString(R.string.content_desc_dice_color_swatch, colorIndex + 1));
        swatch.setTag(getResources().getColor(DiceAdapter.DICE_COLORS[colorIndex]));
        return swatch;
    }

    private void markSwatchSelected(View swatch, boolean selected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor((Integer) swatch.getTag());
        if (selected) {
            int strokeWidth = Math.round(3 * getResources().getDisplayMetrics().density);
            shape.setStroke(strokeWidth, MaterialColors.getColor(swatch, R.attr.colorOnSurface));
        }
        swatch.setBackground(shape);
        // Without this the stroke is the only cue, which says nothing to a screen reader.
        swatch.setSelected(selected);
    }

    @Override
    public void onToggleLock(int position) {
        if (binding == null) {
            return;
        }
        vibrate(30L);
        viewModel.toggleLock(position);
    }

    private void updateLockActionState(DiceUiState state) {
        boolean lockedChanged = hasLockedDice != state.hasLockedDice();
        boolean rollableChanged = hasRollableDice != state.hasRollableDice();
        hasLockedDice = state.hasLockedDice();
        hasRollableDice = state.hasRollableDice();
        if (lockedChanged || rollableChanged) {
            requireActivity().invalidateMenu();
        }
    }

    public static String normalizeFacesInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "6";
        }
        int faces;
        try {
            faces = Integer.parseInt(input.trim());
        } catch (NumberFormatException exception) {
            return "6";
        }
        if (faces < 2) {
            return "2";
        }
        return String.valueOf(faces);
    }

    private void showAddDieDialog() {
        DiceAlertDialogBinding dialogBinding = DiceAlertDialogBinding.inflate(getLayoutInflater());
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogBinding.getRoot())
                        .setTitle(getString(R.string.dice_title))
                        .setPositiveButton(
                                "Add",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        String facesText =
                                                normalizeFacesInput(
                                                        dialogBinding.etDice.getText().toString());
                                        viewModel.addDie(Integer.parseInt(facesText));
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .create();

        dialogBinding.btnD4.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        viewModel.addDie(4);
                        dialog.dismiss();
                    }
                });
        dialogBinding.btnD6.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        viewModel.addDie(6);
                        dialog.dismiss();
                    }
                });
        dialogBinding.btnD10.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        viewModel.addDie(10);
                        dialog.dismiss();
                    }
                });
        dialogBinding.btnD20.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        viewModel.addDie(20);
                        dialog.dismiss();
                    }
                });

        if (dialog.getWindow() != null) {
            dialog.getWindow()
                    .setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
    }

    private void renderResultSummary(DiceUiState state) {
        List<DieUiModel> dice = state.getDice();
        binding.tvDiceTotal.setText(String.valueOf(state.getTotal()));
        binding.tvDiceEmpty.setVisibility(dice.isEmpty() ? View.VISIBLE : View.GONE);
        binding.chipGroupDiceResults.removeAllViews();
        for (DieUiModel die : dice) {
            TextView chip =
                    (TextView)
                            getLayoutInflater()
                                    .inflate(
                                            R.layout.dice_result_chip,
                                            binding.chipGroupDiceResults,
                                            false);
            chip.setText(
                    die.hasLabel()
                            ? getString(
                                    R.string.dice_result_chip_labelled,
                                    die.getLabel(),
                                    die.getValue())
                            : getString(R.string.dice_result_chip, die.getFaces(), die.getValue()));
            binding.chipGroupDiceResults.addView(chip);
        }
    }

    private void vibrate(long milliseconds) {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    private void vibrate(long[] pattern) {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(pattern, -1);
        }
    }
}
