package com.example.sinalperdido;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sinalperdido.databinding.ActivityCentralBinding;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Screen 1 — investigator check-in. Opens the case file only when
 * codename, access code and readiness level are all valid.
 */
public class CentralActivity extends AppCompatActivity {

    private static final String REQUIRED_CODE = "042";
    private static final int CODENAME_MIN_CHARS = 3;
    private static final int READINESS_THRESHOLD = 50;

    private ActivityCentralBinding ui;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = ActivityCentralBinding.inflate(getLayoutInflater());
        setContentView(ui.getRoot());

        renderReadiness(ui.sbReadiness.getProgress());
        ui.sbReadiness.setOnSeekBarChangeListener(readinessWatcher);

        resetErrorOnInput(ui.etCodename, ui.tilCodename);
        resetErrorOnInput(ui.etAccessCode, ui.tilAccessCode);

        ui.btnClear.setOnClickListener(v -> resetForm());
        ui.btnAccess.setOnClickListener(v -> tryToOpenCaseFile());
    }

    private final SeekBar.OnSeekBarChangeListener readinessWatcher =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    renderReadiness(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar bar) { /* not used */ }

                @Override
                public void onStopTrackingTouch(SeekBar bar) { /* not used */ }
            };

    private void renderReadiness(int percent) {
        ui.tvReadinessValue.setText(getString(R.string.readiness_percent, percent));
        if (percent >= READINESS_THRESHOLD) {
            ui.tvReadinessError.setVisibility(View.GONE);
        }
    }

    private void tryToOpenCaseFile() {
        // All three checks always run, so every invalid field shows its own error.
        boolean codenameOk = checkCodename();
        boolean codeOk = checkAccessCode();
        boolean readinessOk = checkReadiness();

        if (!codenameOk) {
            ui.etCodename.requestFocus();
        } else if (!codeOk) {
            ui.etAccessCode.requestFocus();
        } else if (!readinessOk) {
            ui.sbReadiness.requestFocus();
        }

        if (codenameOk && codeOk && readinessOk) {
            startActivity(new Intent(this, CaseFileActivity.class));
        }
    }

    private boolean checkCodename() {
        boolean valid = read(ui.etCodename).length() >= CODENAME_MIN_CHARS;
        ui.tilCodename.setError(valid ? null : getString(R.string.err_codename, CODENAME_MIN_CHARS));
        return valid;
    }

    private boolean checkAccessCode() {
        boolean valid = REQUIRED_CODE.equals(read(ui.etAccessCode));
        ui.tilAccessCode.setError(valid ? null : getString(R.string.err_access_code));
        return valid;
    }

    private boolean checkReadiness() {
        boolean valid = ui.sbReadiness.getProgress() >= READINESS_THRESHOLD;
        if (!valid) {
            ui.tvReadinessError.setText(getString(R.string.err_readiness, READINESS_THRESHOLD));
        }
        ui.tvReadinessError.setVisibility(valid ? View.GONE : View.VISIBLE);
        return valid;
    }

    private void resetForm() {
        ui.etCodename.getText().clear();
        ui.etAccessCode.getText().clear();
        ui.sbReadiness.setProgress(0);
        ui.tilCodename.setError(null);
        ui.tilAccessCode.setError(null);
        ui.tvReadinessError.setVisibility(View.GONE);
        ui.etCodename.requestFocus();
    }

    private static void resetErrorOnInput(EditText field, TextInputLayout container) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                container.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private static String read(EditText field) {
        Editable text = field.getText();
        return text == null ? "" : text.toString().trim();
    }
}
