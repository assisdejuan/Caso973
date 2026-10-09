package com.example.sinalperdido;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sinalperdido.databinding.ActivityCaseFileBinding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Screen 2 — the case file. Holds the evidence, the tape player and the
 * final report form.
 */
public class CaseFileActivity extends AppCompatActivity {

    /** States shown on the recorder display. */
    private enum TapeState {
        WAITING(R.string.state_waiting),
        PLAYING(R.string.state_playing),
        PAUSED(R.string.state_paused),
        STOPPED(R.string.state_stopped),
        COMPLETED(R.string.state_completed);

        @StringRes final int label;

        TapeState(@StringRes int label) {
            this.label = label;
        }
    }

    // Accepted answers (already lower-case). Compared after trim() + toLowerCase().
    private static final Set<String> ACCEPTED_ANSWERS =
            new HashSet<>(Arrays.asList("caio ventura", "caio", "faroleiro", "o faroleiro"));
    private static final int REASONING_MIN_CHARS = 20;
    private static final int SKIP_MS = 10_000;
    private static final long CLOCK_TICK_MS = 300;

    private ActivityCaseFileBinding ui;
    private MediaPlayer tape;
    private TapeState state = TapeState.WAITING;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            refreshClock();
            if (tape != null && tape.isPlaying()) {
                clockHandler.postDelayed(this, CLOCK_TICK_MS);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = ActivityCaseFileBinding.inflate(getLayoutInflater());
        setContentView(ui.getRoot());

        // The player is created once here and reused by every control.
        tape = MediaPlayer.create(this, R.raw.transmissao_interceptada);
        tape.setOnCompletionListener(player -> {
            setState(TapeState.COMPLETED);
            refreshClock();
        });
        setState(TapeState.WAITING);
        refreshClock();

        ui.btnPlay.setOnClickListener(v -> onPlayPressed());
        ui.btnPause.setOnClickListener(v -> onPausePressed());
        ui.btnRestart.setOnClickListener(v -> onRestartPressed());
        ui.btnRewind.setOnClickListener(v -> skip(-SKIP_MS));
        ui.btnForward.setOnClickListener(v -> skip(SKIP_MS));

        ui.btnVerify.setOnClickListener(v -> checkReport());
        ui.btnBack.setOnClickListener(v -> finish());
    }

    // ----------------------------------------------------------------- tape

    private void onPlayPressed() {
        if (tape.isPlaying()) return;
        tape.start();
        setState(TapeState.PLAYING);
        clockHandler.post(clockTick);
    }

    private void onPausePressed() {
        if (!tape.isPlaying()) return;
        tape.pause();
        setState(TapeState.PAUSED);
        refreshClock();
    }

    /** Back to the start without stop(): pause, then seek to zero. */
    private void onRestartPressed() {
        if (tape.isPlaying()) {
            tape.pause();
        }
        tape.seekTo(0);
        setState(TapeState.STOPPED);
        refreshClock();
    }

    /** Moves the position by {@code delta}, clamped to [0, duration]. */
    private void skip(int delta) {
        int target = tape.getCurrentPosition() + delta;
        if (target < 0) target = 0;
        if (target > tape.getDuration()) target = tape.getDuration();
        tape.seekTo(target);
        showClock(target);
    }

    private void setState(TapeState newState) {
        state = newState;
        ui.tvPlayerState.setText(newState.label);
    }

    private void refreshClock() {
        showClock(tape.getCurrentPosition());
    }

    private void showClock(int positionMs) {
        ui.tvTapeTime.setText(getString(R.string.tape_time,
                mmss(positionMs), mmss(tape.getDuration())));
    }

    private static String mmss(int ms) {
        int seconds = ms / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    // --------------------------------------------------------------- report

    private void checkReport() {
        String culprit = read(ui.etCulprit);
        String reasoning = read(ui.etReasoning);

        ui.tilCulprit.setError(culprit.isEmpty() ? getString(R.string.err_culprit) : null);
        ui.tilReasoning.setError(reasoning.length() < REASONING_MIN_CHARS
                ? getString(R.string.err_reasoning, REASONING_MIN_CHARS) : null);

        if (ui.tilCulprit.getError() != null) {
            ui.etCulprit.requestFocus();
        } else if (ui.tilReasoning.getError() != null) {
            ui.etReasoning.requestFocus();
        }
        if (ui.tilCulprit.getError() != null || ui.tilReasoning.getError() != null) {
            ui.tvVerdict.setVisibility(View.GONE);
            return;
        }

        boolean solved = ACCEPTED_ANSWERS.contains(culprit.toLowerCase(Locale.ROOT));
        showVerdict(solved);
    }

    private void showVerdict(boolean solved) {
        ui.tvVerdict.setVisibility(View.VISIBLE);
        if (solved) {
            ui.tvVerdict.setText(getString(R.string.verdict_solved) + "\n\n"
                    + getString(R.string.verdict_epilogue));
            ui.tvVerdict.setTextColor(ContextCompat.getColor(this, R.color.case_closed_green));
            ui.tvVerdict.setBackgroundResource(R.drawable.bg_verdict_solved);
        } else {
            // Never reveal the answer — the investigator can try again.
            ui.tvVerdict.setText(R.string.verdict_rejected);
            ui.tvVerdict.setTextColor(ContextCompat.getColor(this, R.color.stamp_red));
            ui.tvVerdict.setBackgroundResource(R.drawable.bg_verdict_rejected);
        }
    }

    private static String read(EditText field) {
        Editable text = field.getText();
        return text == null ? "" : text.toString().trim();
    }

    // ------------------------------------------------------------ lifecycle

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
        if (tape != null && tape.isPlaying()) {
            tape.pause();
            setState(TapeState.PAUSED);
            refreshClock();
        }
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockTick);
        if (tape != null) {
            tape.release();
            tape = null;
        }
        super.onDestroy();
    }
}
