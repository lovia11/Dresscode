package com.example.dresscode.ui.swap;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dresscode.databinding.FragmentSwapBinding;
import com.example.dresscode.ui.swap.adapter.SwapFavoriteAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class SwapFragment extends Fragment {

    private FragmentSwapBinding binding;
    private SwapViewModel viewModel;
    private SwapFavoriteAdapter adapter;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private File pendingImageFile;
    private Uri pendingImageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSwapBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(this).get(SwapViewModel.class);

        adapter = new SwapFavoriteAdapter(item -> viewModel.setSelectedOutfitId(item.id));
        binding.recyclerFavorites.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.recyclerFavorites.setAdapter(adapter);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && pendingImageFile != null && pendingImageUri != null) {
                        viewModel.setPersonImageUri(pendingImageUri.toString());
                        pendingImageFile = null;
                        pendingImageUri = null;
                    } else {
                        cleanupPendingImage();
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    viewModel.setPersonImageUri(uri.toString());
                }
        );

        binding.btnTakePerson.setOnClickListener(v -> startCameraCapture());
        binding.btnPickPerson.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.btnGenerate.setOnClickListener(v -> {
            String personUri = viewModel.getPersonImageUri().getValue();
            if (personUri == null || personUri.trim().isEmpty()) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(com.example.dresscode.R.string.placeholder_swap_select_person)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            binding.imageResult.setImageURI(null);
            binding.imageResult.setImageURI(Uri.parse(personUri));
            binding.textResultHint.setText(com.example.dresscode.R.string.placeholder_swap_result);
        });

        viewModel.getFavoriteOutfits().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items, viewModel.getSelectedOutfitId().getValue());
            boolean empty = items == null || items.isEmpty();
            binding.textNoFavorites.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerFavorites.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getSelectedOutfit().observe(getViewLifecycleOwner(), selected -> {
            if (selected == null) {
                binding.textSelectedOutfit.setText(getString(com.example.dresscode.R.string.label_selected_outfit));
            } else {
                binding.textSelectedOutfit.setText(getString(com.example.dresscode.R.string.label_selected_outfit) + selected.title);
            }
            adapter.submitList(viewModel.getFavoriteOutfits().getValue(), viewModel.getSelectedOutfitId().getValue());
        });

        viewModel.getPersonImageUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri == null || uri.trim().isEmpty()) {
                binding.textPersonHint.setVisibility(View.VISIBLE);
                binding.imagePerson.setImageURI(null);
            } else {
                binding.textPersonHint.setVisibility(View.GONE);
                binding.imagePerson.setImageURI(null);
                binding.imagePerson.setImageURI(Uri.parse(uri));
            }
        });

        viewModel.getCanGenerate().observe(getViewLifecycleOwner(), can -> binding.btnGenerate.setEnabled(Boolean.TRUE.equals(can)));

        return binding.getRoot();
    }

    private void startCameraCapture() {
        File imageFile = createImageFile();
        if (imageFile == null) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(com.example.dresscode.R.string.error_image_import_failed)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        pendingImageFile = imageFile;
        pendingImageUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                imageFile
        );
        takePictureLauncher.launch(pendingImageUri);
    }

    private File createImageFile() {
        File dir = new File(requireContext().getFilesDir(), "swap_person_images");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return new File(dir, "person_" + System.currentTimeMillis() + ".jpg");
    }

    private void cleanupPendingImage() {
        if (pendingImageFile != null) {
            try {
                if (pendingImageFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    pendingImageFile.delete();
                }
            } catch (Exception ignored) {
            }
        }
        pendingImageFile = null;
        pendingImageUri = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
