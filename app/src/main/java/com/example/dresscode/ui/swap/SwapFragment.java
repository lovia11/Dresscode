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
import com.example.dresscode.ui.swap.adapter.SwapClosetFavoriteAdapter;
import com.example.dresscode.ui.swap.adapter.SwapFavoriteAdapter;
import com.example.dresscode.ui.swap.adapter.SwapHistoryAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class SwapFragment extends Fragment {

    public static final String ARG_PRESELECT_OUTFIT_ID = "preselect_outfit_id";

    private FragmentSwapBinding binding;
    private SwapViewModel viewModel;
    private SwapFavoriteAdapter adapter;
    private SwapClosetFavoriteAdapter closetAdapter;
    private SwapHistoryAdapter historyAdapter;

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

        boolean fromOutfits = false;
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null) {
                long preselectId = args.getLong(ARG_PRESELECT_OUTFIT_ID, -1L);
                if (preselectId > 0) {
                    viewModel.selectOutfit(preselectId);
                    fromOutfits = true;
                }
            }
        }

        binding.btnBackToOutfits.setVisibility(fromOutfits ? View.VISIBLE : View.GONE);
        binding.btnBackToOutfits.setOnClickListener(v -> {
            androidx.navigation.NavController nav = androidx.navigation.fragment.NavHostFragment.findNavController(this);
            boolean popped = nav.popBackStack(com.example.dresscode.R.id.outfitsFragment, false);
            if (!popped) {
                nav.navigate(com.example.dresscode.R.id.outfitsFragment);
            }
        });

        adapter = new SwapFavoriteAdapter(new SwapFavoriteAdapter.Listener() {
            @Override
            public void onSelect(com.example.dresscode.data.local.OutfitCardRow item) {
                viewModel.selectOutfit(item.id);
            }

            @Override
            public void onPreview(com.example.dresscode.data.local.OutfitCardRow item) {
                if (item == null) {
                    return;
                }
                com.example.dresscode.ui.preview.ImagePreviewBottomSheet
                        .newInstance(item.title, item.tags, item.coverResId, "")
                        .show(getParentFragmentManager(), "image_preview");
            }
        });
        binding.recyclerFavorites.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.recyclerFavorites.setAdapter(adapter);

        closetAdapter = new SwapClosetFavoriteAdapter(new SwapClosetFavoriteAdapter.Listener() {
            @Override
            public void onSelect(com.example.dresscode.data.local.ClosetItemEntity item) {
                viewModel.selectClosetItem(item.id);
            }

            @Override
            public void onPreview(com.example.dresscode.data.local.ClosetItemEntity item) {
                if (item == null) {
                    return;
                }
                com.example.dresscode.ui.preview.ImagePreviewBottomSheet
                        .newInstance(item.name, buildMeta(item), 0, item.imageUri)
                        .show(getParentFragmentManager(), "image_preview");
            }

            private String buildMeta(com.example.dresscode.data.local.ClosetItemEntity item) {
                java.util.ArrayList<String> parts = new java.util.ArrayList<>();
                if (item.category != null && !item.category.trim().isEmpty() && !"未知".equals(item.category.trim())) {
                    parts.add(item.category.trim());
                }
                if (item.style != null && !item.style.trim().isEmpty() && !"未知".equals(item.style.trim())) {
                    parts.add(item.style.trim());
                }
                if (item.scene != null && !item.scene.trim().isEmpty() && !"未知".equals(item.scene.trim())) {
                    parts.add(item.scene.trim());
                }
                return parts.isEmpty() ? "" : android.text.TextUtils.join(" · ", parts);
            }
        });
        binding.recyclerFavoriteCloset.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.recyclerFavoriteCloset.setAdapter(closetAdapter);

        historyAdapter = new SwapHistoryAdapter(row -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(com.example.dresscode.R.string.action_delete)
                .setMessage(getString(com.example.dresscode.R.string.confirm_delete_history, row.sourceTitle))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(com.example.dresscode.R.string.action_delete, (d, w) -> viewModel.deleteHistory(row.id))
                .show());
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerHistory.setAdapter(historyAdapter);

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
            binding.textResultHint.setText("生成中...");
            viewModel.generateSwap();
        });

        viewModel.getFavoriteOutfits().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items, viewModel.getSelectedOutfitId().getValue());
            boolean empty = items == null || items.isEmpty();
            binding.textNoFavorites.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerFavorites.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getFavoriteClosetItems().observe(getViewLifecycleOwner(), items -> {
            closetAdapter.submitList(items, viewModel.getSelectedClosetItemId().getValue());
            boolean empty = items == null || items.isEmpty();
            binding.textNoFavoriteCloset.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerFavoriteCloset.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getSelectedLabel().observe(getViewLifecycleOwner(), label ->
                binding.textSelectedOutfit.setText(getString(com.example.dresscode.R.string.label_selected_outfit) + (label == null ? "" : label))
        );

        viewModel.getSelectedOutfitId().observe(getViewLifecycleOwner(), id ->
                adapter.submitList(viewModel.getFavoriteOutfits().getValue(), id)
        );

        viewModel.getSelectedClosetItemId().observe(getViewLifecycleOwner(), id ->
                closetAdapter.submitList(viewModel.getFavoriteClosetItems().getValue(), id)
        );

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

        binding.imagePerson.setOnClickListener(v -> {
            String uri = viewModel.getPersonImageUri().getValue();
            if (uri == null || uri.trim().isEmpty()) {
                return;
            }
            showImagePreview("人像预览", uri);
        });

        viewModel.getCanGenerate().observe(getViewLifecycleOwner(), can -> {
            boolean generating = Boolean.TRUE.equals(viewModel.getGenerating().getValue());
            binding.btnGenerate.setEnabled(!generating && Boolean.TRUE.equals(can));
        });

        viewModel.getGenerating().observe(getViewLifecycleOwner(), g -> {
            boolean generating = Boolean.TRUE.equals(g);
            binding.btnGenerate.setEnabled(!generating && Boolean.TRUE.equals(viewModel.getCanGenerate().getValue()));
            if (generating) {
                binding.textResultHint.setText("生成中...");
            }
        });

        viewModel.getGenerateError().observe(getViewLifecycleOwner(), err -> {
            if (err == null || err.trim().isEmpty()) {
                return;
            }
            if (!isAdded()) {
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage("换装失败：" + err)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });

        viewModel.getResultImageUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri == null || uri.trim().isEmpty()) {
                return;
            }
            try {
                binding.imageResult.setImageURI(null);
                binding.imageResult.setImageURI(Uri.parse(uri));
                binding.textResultHint.setText(com.example.dresscode.R.string.placeholder_swap_result);
            } catch (Exception e) {
                binding.imageResult.setImageURI(null);
            }
        });

        binding.imageResult.setOnClickListener(v -> {
            String uri = viewModel.getResultImageUri().getValue();
            if (uri == null || uri.trim().isEmpty()) {
                return;
            }
            showImagePreview("换装结果预览", uri);
        });

        viewModel.getHistory().observe(getViewLifecycleOwner(), rows -> historyAdapter.submitList(rows));

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

    private void showImagePreview(String title, String uriString) {
        if (!isAdded()) {
            return;
        }
        android.widget.ImageView imageView = new android.widget.ImageView(requireContext());
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        int pad = Math.round(getResources().getDisplayMetrics().density * 12);
        imageView.setPadding(pad, pad, pad, pad);
        try {
            imageView.setImageURI(Uri.parse(uriString));
        } catch (Exception ignored) {
            imageView.setImageURI(null);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(imageView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
