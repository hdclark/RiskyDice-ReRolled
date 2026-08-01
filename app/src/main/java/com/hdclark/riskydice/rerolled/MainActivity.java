package com.hdclark.riskydice.rerolled;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends Activity {
    private static final int[][] MATCHUPS = {
            {3, 2}, {3, 1}, {2, 2}, {2, 1}, {1, 2}, {1, 1}
    };

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicInteger workGeneration = new AtomicInteger();
    private final RiskSimulator simulator = new RiskSimulator();

    private EditText battleInput;
    private Button rollButton;
    private Button estimateRoundButton;
    private Button battleButton;
    private Button clearButton;
    private Button helpButton;
    private ProgressBar progress;
    private TextView statusText;
    private TextView bannerText;
    private GridLayout resultsGrid;
    private TextView[] resultViews;
    private volatile boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        battleInput = findViewById(R.id.battleInput);
        rollButton = findViewById(R.id.rollButton);
        estimateRoundButton = findViewById(R.id.estimateRoundButton);
        battleButton = findViewById(R.id.battleButton);
        clearButton = findViewById(R.id.clearButton);
        helpButton = findViewById(R.id.helpButton);
        progress = findViewById(R.id.progress);
        statusText = findViewById(R.id.statusText);
        bannerText = findViewById(R.id.bannerText);
        resultsGrid = findViewById(R.id.resultsGrid);
        resultViews = new TextView[] {
                findViewById(R.id.result0),
                findViewById(R.id.result1),
                findViewById(R.id.result2),
                findViewById(R.id.result3),
                findViewById(R.id.result4),
                findViewById(R.id.result5)
        };

        rollButton.setOnClickListener(view -> rollAllMatchups());
        estimateRoundButton.setOnClickListener(view -> estimateAllRoundLosses());
        battleButton.setOnClickListener(view -> simulateBattle());
        clearButton.setOnClickListener(view -> clearAll());
        helpButton.setOnClickListener(view -> showUsage());

        battleInput.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterKey = event != null
                    && event.getAction() == KeyEvent.ACTION_UP
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_DONE || enterKey) {
                simulateBattle();
                return true;
            }
            return false;
        });
        battleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                if (!busy && sequence.length() > 0) {
                    bannerText.setText(sequence);
                    bannerText.setVisibility(View.VISIBLE);
                    resultsGrid.setVisibility(View.GONE);
                    statusText.setText("Press Enter or Simulate battle.");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        showUsage();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        int unicode = event.getUnicodeChar();
        char key = unicode == 0 ? '\0' : Character.toLowerCase((char) unicode);
        boolean handledKey = code == KeyEvent.KEYCODE_ESCAPE
                || code == KeyEvent.KEYCODE_SPACE
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || key == 'q'
                || key == 's'
                || key == 'c'
                || key == 'v'
                || (key >= '0' && key <= '9');

        // Consume both halves of physical-key events so EditText does not insert a second copy.
        if (handledKey && event.getAction() == KeyEvent.ACTION_DOWN) {
            return true;
        }
        if (event.getAction() != KeyEvent.ACTION_UP || !handledKey) {
            return super.dispatchKeyEvent(event);
        }

        if (code == KeyEvent.KEYCODE_ESCAPE || key == 'q') {
            finish();
            return true;
        }
        if (busy) {
            return true;
        }
        if (code == KeyEvent.KEYCODE_SPACE) {
            rollAllMatchups();
            return true;
        }
        if (key == 's') {
            estimateAllRoundLosses();
            return true;
        }
        if (key == 'c') {
            clearAll();
            return true;
        }
        if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            simulateBattle();
            return true;
        }
        if ((key >= '0' && key <= '9') || key == 'v') {
            if (battleInput.length() < 9) {
                battleInput.append(String.valueOf(key));
                battleInput.requestFocus();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        workGeneration.incrementAndGet();
        worker.shutdownNow();
        super.onDestroy();
    }

    private void rollAllMatchups() {
        submitWork("Rolling all six matchups…", () -> {
            List<String> cards = new ArrayList<>(MATCHUPS.length);
            for (int[] matchup : MATCHUPS) {
                cards.add(RiskFormatter.roundResult(simulator.rollRound(matchup[0], matchup[1])));
            }
            return cards;
        });
    }

    private void estimateAllRoundLosses() {
        submitWork("Running 10,000 samples for each matchup…", () -> {
            List<String> cards = new ArrayList<>(MATCHUPS.length);
            for (int[] matchup : MATCHUPS) {
                cards.add(RiskFormatter.roundEstimate(
                        simulator.estimateRoundLosses(matchup[0], matchup[1])));
            }
            return cards;
        });
    }

    private void simulateBattle() {
        if (busy) {
            return;
        }

        final RiskSimulator.BattleInput input;
        try {
            input = RiskSimulator.BattleInput.parse(battleInput.getText().toString());
        } catch (IllegalArgumentException error) {
            battleInput.setError(error.getMessage());
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        submitWork(
                "Sampling " + input.attackers + "v" + input.defenders + "…",
                () -> {
                    List<String> cards = new ArrayList<>(2);
                    cards.add(RiskFormatter.battleResult(
                            simulator.simulateBattle(input.attackers, input.defenders)));
                    cards.add(RiskFormatter.battleEstimate(
                            simulator.estimateBattleLosses(input.attackers, input.defenders)));
                    return cards;
                });
        // The reference clears its free-form input after a successful launch.
        battleInput.setText("");
    }

    private void submitWork(String message, Work work) {
        if (busy) {
            return;
        }
        int generation = workGeneration.incrementAndGet();
        setBusy(true, message);
        worker.execute(() -> {
            try {
                List<String> cards = work.run();
                runOnUiThread(() -> {
                    if (generation != workGeneration.get() || isFinishing() || isDestroyed()) {
                        return;
                    }
                    showCards(cards);
                    setBusy(false, "Ready.");
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (generation != workGeneration.get() || isFinishing() || isDestroyed()) {
                        return;
                    }
                    setBusy(false, "Could not complete simulation.");
                    String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                    Toast.makeText(this, detail, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showCards(List<String> cards) {
        bannerText.setVisibility(View.GONE);
        resultsGrid.setVisibility(View.VISIBLE);
        for (int i = 0; i < resultViews.length; i++) {
            if (i < cards.size()) {
                resultViews[i].setText(cards.get(i));
                resultViews[i].setVisibility(View.VISIBLE);
            } else {
                resultViews[i].setText("");
                resultViews[i].setVisibility(View.GONE);
            }
        }
    }

    private void clearAll() {
        battleInput.setText("");
        battleInput.setError(null);
        showUsage();
    }

    private void showUsage() {
        if (busy) {
            return;
        }
        bannerText.setText(RiskFormatter.usage());
        bannerText.setVisibility(View.VISIBLE);
        resultsGrid.setVisibility(View.GONE);
        statusText.setText("Ready.");
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        statusText.setText(message);
        rollButton.setEnabled(!value);
        estimateRoundButton.setEnabled(!value);
        battleButton.setEnabled(!value);
        clearButton.setEnabled(!value);
        helpButton.setEnabled(!value);
        battleInput.setEnabled(!value);
    }

    @FunctionalInterface
    private interface Work {
        List<String> run();
    }
}
