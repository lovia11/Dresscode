package com.example.dresscode;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dresscode.data.prefs.AuthRepository;
import com.example.dresscode.databinding.ActivityLoginBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthRepository auth;
    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = new AuthRepository(this);

        if (!auth.canLogin()) {
            isRegisterMode = true;
        }
        updateModeUi();

        binding.btnPrimary.setOnClickListener(v -> {
            String username = binding.editUsername.getText() == null ? "" : binding.editUsername.getText().toString();
            String password = binding.editPassword.getText() == null ? "" : binding.editPassword.getText().toString();
            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                showMessage(getString(R.string.error_login_empty));
                return;
            }

            if (isRegisterMode) {
                if (auth.hasUser(username)) {
                    showMessage(getString(R.string.error_register_exists));
                    return;
                }
                String nickname = binding.editNickname.getText() == null ? "" : binding.editNickname.getText().toString();
                auth.register(username, password, nickname);
                goMain();
                return;
            }

            boolean ok = auth.login(username, password);
            if (ok) {
                goMain();
            } else {
                showMessage(getString(R.string.error_login_failed));
            }
        });

        binding.btnSwitchMode.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            updateModeUi();
        });

        binding.btnPickUser.setOnClickListener(v -> {
            List<String> users = auth.getAllUsernames();
            if (users.isEmpty()) {
                return;
            }
            String[] items = users.toArray(new String[0]);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.action_pick_registered_user)
                    .setItems(items, (d, which) -> binding.editUsername.setText(items[which]))
                    .show();
        });
    }

    private void updateModeUi() {
        binding.textTitle.setText(isRegisterMode ? R.string.title_register : R.string.title_login);
        binding.btnPrimary.setText(isRegisterMode ? R.string.action_register : R.string.action_login);
        binding.layoutNickname.setVisibility(isRegisterMode ? View.VISIBLE : View.GONE);
        binding.btnPickUser.setVisibility(!isRegisterMode && auth.canLogin() ? View.VISIBLE : View.GONE);

        boolean canSwitch = auth.canLogin();
        binding.btnSwitchMode.setVisibility(canSwitch ? View.VISIBLE : View.GONE);
        if (canSwitch) {
            binding.btnSwitchMode.setText(isRegisterMode ? R.string.action_switch_to_login : R.string.action_switch_to_register);
        }
    }

    private void showMessage(String message) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
