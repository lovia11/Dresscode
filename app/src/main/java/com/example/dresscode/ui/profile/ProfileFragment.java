package com.example.dresscode.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dresscode.LoginActivity;
import com.example.dresscode.data.prefs.AuthRepository;
import com.example.dresscode.data.prefs.UserPreferencesRepository;
import com.example.dresscode.databinding.FragmentProfileBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private UserPreferencesRepository prefs;
    private AuthRepository auth;
    private ActivityResultLauncher<String> pickAvatarLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        prefs = new UserPreferencesRepository(requireContext());
        auth = new AuthRepository(requireContext());

        pickAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    File copied = copyToAppStorage(uri);
                    if (copied == null) {
                        return;
                    }
                    Uri fileUri = Uri.fromFile(copied);
                    auth.setAvatarUri(fileUri.toString());
                    binding.imageAvatar.setImageURI(null);
                    binding.imageAvatar.setImageURI(fileUri);
                }
        );

        setupGender();
        setupAccount();
        return binding.getRoot();
    }

    private void setupGender() {
        String gender = prefs.getGender();
        if (UserPreferencesRepository.GENDER_MALE.equals(gender)) {
            binding.chipGenderMale.setChecked(true);
        } else if (UserPreferencesRepository.GENDER_FEMALE.equals(gender)) {
            binding.chipGenderFemale.setChecked(true);
        } else {
            binding.chipGenderUnset.setChecked(true);
        }

        binding.groupGender.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == binding.chipGenderMale.getId()) {
                prefs.setGender(UserPreferencesRepository.GENDER_MALE);
            } else if (id == binding.chipGenderFemale.getId()) {
                prefs.setGender(UserPreferencesRepository.GENDER_FEMALE);
            } else {
                prefs.setGender(UserPreferencesRepository.GENDER_UNSET);
            }
        });
    }

    private void setupAccount() {
        String username = auth.getCurrentUsername();
        binding.textUser.setText(getString(com.example.dresscode.R.string.label_current_user) + (username == null ? "-" : username));

        String nickname = auth.getNickname();
        if (nickname != null) {
            binding.editNickname.setText(nickname);
        }

        String avatar = auth.getAvatarUri();
        if (avatar != null && !avatar.trim().isEmpty()) {
            binding.imageAvatar.setImageURI(null);
            binding.imageAvatar.setImageURI(Uri.parse(avatar));
        } else {
            binding.imageAvatar.setImageURI(null);
        }

        binding.btnPickAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));

        binding.btnSaveProfile.setOnClickListener(v -> {
            String nick = binding.editNickname.getText() == null ? "" : binding.editNickname.getText().toString();
            auth.setNickname(nick);
        });

        binding.btnLogout.setOnClickListener(v -> {
            auth.logout();
            startActivity(new android.content.Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });
    }

    private File copyToAppStorage(Uri source) {
        File dest = createAvatarFile();
        if (dest == null) {
            return null;
        }
        try (InputStream in = requireContext().getContentResolver().openInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null) {
                safeDeleteFile(dest);
                return null;
            }
            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return dest;
        } catch (Exception e) {
            safeDeleteFile(dest);
            return null;
        }
    }

    private File createAvatarFile() {
        File dir = new File(requireContext().getFilesDir(), "profile_avatars");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return new File(dir, "avatar_" + System.currentTimeMillis() + ".jpg");
    }

    private void safeDeleteFile(File file) {
        try {
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (prefs != null) {
            prefs.close();
            prefs = null;
        }
        auth = null;
        binding = null;
    }
}
