package com.example.dresscode.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dresscode.data.prefs.UserPreferencesRepository;
import com.example.dresscode.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private UserPreferencesRepository prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        prefs = new UserPreferencesRepository(requireContext());
        setupGender();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (prefs != null) {
            prefs.close();
            prefs = null;
        }
        binding = null;
    }
}
