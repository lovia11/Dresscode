package com.example.dresscode.ui.closet;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.dresscode.data.prefs.AuthRepository;
import com.example.dresscode.R;
import com.example.dresscode.data.local.ClosetItemEntity;
import com.example.dresscode.data.repository.AiTagRepository;
import com.example.dresscode.databinding.DialogAddClothingBinding;
import com.example.dresscode.databinding.FragmentClosetBinding;
import com.example.dresscode.ui.closet.adapter.ClosetAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class ClosetFragment extends Fragment {

    private FragmentClosetBinding binding;
    private ClosetViewModel viewModel;
    private ClosetAdapter adapter;
    private String owner;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private File pendingImageFile;
    private Uri pendingImageUri;
    private AiTagRepository aiTagRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentClosetBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(this).get(ClosetViewModel.class);
        owner = new AuthRepository(requireContext()).getCurrentUsernameOrEmpty();
        aiTagRepository = new AiTagRepository(requireContext());
        adapter = new ClosetAdapter(new ClosetAdapter.Listener() {
            @Override
            public void onLongPress(ClosetItemEntity item) {
                showItemActions(item);
            }

            @Override
            public void onToggleFavorite(ClosetItemEntity item) {
                viewModel.setFavorite(item.id, !item.isFavorite);
            }
        });

        binding.recyclerCloset.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerCloset.setAdapter(adapter);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && pendingImageFile != null) {
                        showAddItemDialog(pendingImageFile, pendingImageUri);
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
                    File copied = copyToAppStorage(uri);
                    if (copied == null) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setMessage(R.string.error_image_import_failed)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        return;
                    }
                    Uri copiedUri = Uri.fromFile(copied);
                    showAddItemDialog(copied, copiedUri);
                }
        );

        binding.fabAddClothing.setOnClickListener(v -> {
            String[] actions = new String[]{
                    getString(R.string.action_take_photo),
                    getString(R.string.action_pick_from_gallery)
            };
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.title_add_clothing)
                    .setItems(actions, (dialog, which) -> {
                        if (which == 0) {
                            startCameraCapture();
                        } else {
                            pickImageLauncher.launch("image/*");
                        }
                    })
                    .show();
        });

        viewModel.getClosetItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            boolean empty = items == null || items.isEmpty();
            binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerCloset.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        return binding.getRoot();
    }

    private void startCameraCapture() {
        File imageFile = createImageFile();
        if (imageFile == null) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.error_image_import_failed)
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

    private void showAddItemDialog(File imageFile, Uri imageUri) {
        DialogAddClothingBinding dialogBinding = DialogAddClothingBinding.inflate(getLayoutInflater());
        Uri previewUri = imageFile != null ? Uri.fromFile(imageFile) : imageUri;
        dialogBinding.imagePreview.setImageURI(previewUri);

        String[] categories = getResources().getStringArray(R.array.closet_categories);
        dialogBinding.inputCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                categories
        ));
        dialogBinding.inputCategory.setText(categories.length > 0 ? categories[0] : "", false);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_add_clothing)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, (d, w) -> {
                    safeDeleteFile(imageFile);
                    if (pendingImageFile != null && pendingImageFile.equals(imageFile)) {
                        cleanupPendingImage();
                    }
                })
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = valueOrEmpty(dialogBinding.inputName.getText() == null ? null : dialogBinding.inputName.getText().toString());
                    String category = valueOrEmpty(dialogBinding.inputCategory.getText() == null ? null : dialogBinding.inputCategory.getText().toString());
                    String color = valueOrEmpty(dialogBinding.inputColor.getText() == null ? null : dialogBinding.inputColor.getText().toString());
                    String season = valueOrEmpty(dialogBinding.inputSeason.getText() == null ? null : dialogBinding.inputSeason.getText().toString());
                    String style = valueOrEmpty(dialogBinding.inputStyle.getText() == null ? null : dialogBinding.inputStyle.getText().toString());
                    String scene = valueOrEmpty(dialogBinding.inputScene.getText() == null ? null : dialogBinding.inputScene.getText().toString());

                    if (name.isEmpty()) {
                        name = getString(R.string.default_clothing_name, category.isEmpty() ? getString(R.string.default_category) : category);
                    }
                    if (color.isEmpty()) {
                        color = getString(R.string.default_unknown);
                    }
                    if (season.isEmpty()) {
                        season = getString(R.string.default_unknown);
                    }
                    if (style.isEmpty()) {
                        style = getString(R.string.default_unknown);
                    }
                    if (scene.isEmpty()) {
                        scene = getString(R.string.default_unknown);
                    }
                    if (category.isEmpty()) {
                        category = getString(R.string.default_category);
                    }

                    String storedUri = imageFile != null ? Uri.fromFile(imageFile).toString() : imageUri.toString();
                    ClosetItemEntity item = new ClosetItemEntity(
                            owner,
                            name,
                            category,
                            storedUri,
                            color,
                            season,
                            style,
                            scene,
                            false,
                            System.currentTimeMillis()
                    );
                    viewModel.add(item);
                    if (pendingImageFile != null && pendingImageFile.equals(imageFile)) {
                        pendingImageFile = null;
                        pendingImageUri = null;
                    }
                })
                .create();
        dialog.show();

        // AI 先打标，再让用户确认（失败就让用户手动填）
        dialogBinding.textAiHint.setText(R.string.hint_ai_tagging);
        dialogBinding.textAiHint.setVisibility(View.VISIBLE);
        android.widget.Button btnSave = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        if (btnSave != null) {
            btnSave.setEnabled(false);
        }

        aiTagRepository.tagImageUriAsync(previewUri, new AiTagRepository.ResultCallback() {
            @Override
            public void onSuccess(String model, String resultJson) {
                if (!isAdded()) {
                    return;
                }
                applyAiToDialog(dialogBinding, resultJson);
                dialogBinding.textAiHint.setVisibility(View.GONE);
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                dialogBinding.textAiHint.setText(R.string.hint_ai_tag_failed);
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                }
            }
        });
    }

    private void applyAiToDialog(DialogAddClothingBinding dialogBinding, String resultJson) {
        if (resultJson == null || resultJson.trim().isEmpty()) {
            return;
        }
        try {
            JsonElement el = JsonParser.parseString(resultJson);
            if (!el.isJsonObject()) {
                return;
            }
            JsonObject obj = el.getAsJsonObject();

            String category = pickString(obj, "category");
            if (category.isEmpty()) {
                category = inferCategoryFromKeywords(obj);
            }
            if (!category.isEmpty()) {
                dialogBinding.inputCategory.setText(mapCategory(category), false);
            }

            String style = mapStyle(pickString(obj, "style"));
            if (!style.isEmpty()) {
                dialogBinding.inputStyle.setText(style);
            }

            String season = mapSeason(pickString(obj, "season"));
            if (!season.isEmpty()) {
                dialogBinding.inputSeason.setText(season);
            }

            String scene = mapScene(pickString(obj, "scene"));
            if (!scene.isEmpty()) {
                dialogBinding.inputScene.setText(scene);
            }

            String color = mapColor(pickFirstArrayString(obj, "colors"));
            if (!color.isEmpty()) {
                dialogBinding.inputColor.setText(color);
            }

            String currentName = valueOrEmpty(dialogBinding.inputName.getText() == null ? null : dialogBinding.inputName.getText().toString());
            if (currentName.isEmpty()) {
                String n = mapCategory(category.isEmpty() ? getString(R.string.default_category) : category);
                String s = style.isEmpty() ? "" : (" · " + style);
                dialogBinding.inputName.setText((n + s).trim());
            }
        } catch (Exception ignored) {
        }
    }

    private String pickString(JsonObject obj, String key) {
        try {
            if (obj == null || key == null) {
                return "";
            }
            JsonElement el = obj.get(key);
            if (el == null || el.isJsonNull()) {
                return "";
            }
            String v = el.getAsString();
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String pickFirstArrayString(JsonObject obj, String key) {
        try {
            if (obj == null || key == null) {
                return "";
            }
            JsonElement el = obj.get(key);
            if (el == null || !el.isJsonArray()) {
                return "";
            }
            if (el.getAsJsonArray().size() == 0) {
                return "";
            }
            JsonElement first = el.getAsJsonArray().get(0);
            if (first == null || first.isJsonNull()) {
                return "";
            }
            String v = first.getAsString();
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String inferCategoryFromKeywords(JsonObject obj) {
        try {
            JsonElement el = obj == null ? null : obj.get("keywords");
            if (el == null || !el.isJsonArray()) {
                return "";
            }
            String text = el.toString().toLowerCase(Locale.US);
            if (text.contains("dress") || text.contains("skirt")) {
                return "连衣裙";
            }
            if (text.contains("coat") || text.contains("jacket") || text.contains("outer")) {
                return "外套";
            }
            if (text.contains("pants") || text.contains("jeans") || text.contains("trousers")) {
                return "下装";
            }
            if (text.contains("shoe") || text.contains("sneaker") || text.contains("boot")) {
                return "鞋子";
            }
            if (text.contains("top") || text.contains("shirt") || text.contains("t-shirt") || text.contains("tank")) {
                return "上衣";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private String mapCategory(String raw) {
        String s = valueOrEmpty(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("top") || lower.contains("shirt")) {
            return "上衣";
        }
        if (lower.contains("bottom") || lower.contains("pants") || lower.contains("jeans")) {
            return "下装";
        }
        if (lower.contains("outer") || lower.contains("coat") || lower.contains("jacket")) {
            return "外套";
        }
        if (lower.contains("dress")) {
            return "连衣裙";
        }
        if (lower.contains("shoe") || lower.contains("sneaker") || lower.contains("boot")) {
            return "鞋子";
        }
        if (lower.contains("accessory") || lower.contains("bag")) {
            return "配饰";
        }
        return s;
    }

    private String mapStyle(String raw) {
        String s = valueOrEmpty(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("casual")) {
            return "休闲";
        }
        if (lower.contains("commute") || lower.contains("work")) {
            return "通勤";
        }
        if (lower.contains("sport")) {
            return "运动";
        }
        if (lower.contains("date") || lower.contains("romantic")) {
            return "约会";
        }
        return s;
    }

    private String mapSeason(String raw) {
        String s = valueOrEmpty(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("spring") && lower.contains("summer")) {
            return "春夏";
        }
        if (lower.contains("autumn") && lower.contains("winter")) {
            return "秋冬";
        }
        if (lower.contains("spring") && lower.contains("autumn")) {
            return "春秋";
        }
        if (lower.contains("winter")) {
            return "冬";
        }
        if (lower.contains("summer")) {
            return "夏";
        }
        if (lower.contains("spring")) {
            return "春";
        }
        if (lower.contains("autumn") || lower.contains("fall")) {
            return "秋";
        }
        if (lower.contains("all")) {
            return "春秋";
        }
        return s;
    }

    private String mapScene(String raw) {
        String s = valueOrEmpty(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("everyday") || lower.contains("daily")) {
            return "出街";
        }
        if (lower.contains("campus") || lower.contains("school")) {
            return "校园";
        }
        if (lower.contains("commute") || lower.contains("work")) {
            return "通勤";
        }
        if (lower.contains("sport")) {
            return "运动";
        }
        return s;
    }

    private String mapColor(String raw) {
        String s = valueOrEmpty(raw);
        if (s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase(Locale.US);
        if (lower.contains("black")) {
            return "黑";
        }
        if (lower.contains("white")) {
            return "白";
        }
        if (lower.contains("gray") || lower.contains("grey")) {
            return "灰";
        }
        if (lower.contains("blue")) {
            return "蓝";
        }
        if (lower.contains("red")) {
            return "红";
        }
        if (lower.contains("green")) {
            return "绿";
        }
        if (lower.contains("brown")) {
            return "棕";
        }
        return s;
    }

    private void showItemActions(ClosetItemEntity item) {
        String[] actions = new String[]{
                getString(R.string.action_edit),
                getString(R.string.action_delete)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.name)
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        showEditItemDialog(item);
                    } else if (which == 1) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.action_delete)
                                .setMessage(getString(R.string.confirm_delete_clothing, item.name))
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.action_delete, (dd, ww) -> viewModel.delete(item.id))
                                .show();
                    }
                })
                .show();
    }

    private void showEditItemDialog(ClosetItemEntity item) {
        DialogAddClothingBinding dialogBinding = DialogAddClothingBinding.inflate(getLayoutInflater());
        try {
            dialogBinding.imagePreview.setImageURI(Uri.parse(item.imageUri));
        } catch (Exception e) {
            dialogBinding.imagePreview.setImageURI(null);
        }

        dialogBinding.inputName.setText(item.name);

        String[] categories = getResources().getStringArray(R.array.closet_categories);
        dialogBinding.inputCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                categories
        ));
        dialogBinding.inputCategory.setText(item.category, false);
        dialogBinding.inputColor.setText(item.color);
        dialogBinding.inputSeason.setText(item.season);
        dialogBinding.inputStyle.setText(item.style);
        dialogBinding.inputScene.setText(item.scene);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_edit_clothing)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name = valueOrEmpty(dialogBinding.inputName.getText() == null ? null : dialogBinding.inputName.getText().toString());
                    String category = valueOrEmpty(dialogBinding.inputCategory.getText() == null ? null : dialogBinding.inputCategory.getText().toString());
                    String color = valueOrEmpty(dialogBinding.inputColor.getText() == null ? null : dialogBinding.inputColor.getText().toString());
                    String season = valueOrEmpty(dialogBinding.inputSeason.getText() == null ? null : dialogBinding.inputSeason.getText().toString());
                    String style = valueOrEmpty(dialogBinding.inputStyle.getText() == null ? null : dialogBinding.inputStyle.getText().toString());
                    String scene = valueOrEmpty(dialogBinding.inputScene.getText() == null ? null : dialogBinding.inputScene.getText().toString());

                    if (name.isEmpty()) {
                        name = getString(R.string.default_clothing_name, category.isEmpty() ? getString(R.string.default_category) : category);
                    }
                    if (color.isEmpty()) {
                        color = getString(R.string.default_unknown);
                    }
                    if (season.isEmpty()) {
                        season = getString(R.string.default_unknown);
                    }
                    if (style.isEmpty()) {
                        style = getString(R.string.default_unknown);
                    }
                    if (scene.isEmpty()) {
                        scene = getString(R.string.default_unknown);
                    }
                    if (category.isEmpty()) {
                        category = getString(R.string.default_category);
                    }

                    viewModel.update(item.id, name, category, color, season, style, scene);
                })
                .show();
    }

    private File createImageFile() {
        File dir = new File(requireContext().getFilesDir(), "closet_images");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        String filename = String.format(Locale.US, "closet_%d.jpg", System.currentTimeMillis());
        return new File(dir, filename);
    }

    private File copyToAppStorage(Uri source) {
        File dest = createImageFile();
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

    private void cleanupPendingImage() {
        if (pendingImageFile != null) {
            safeDeleteFile(pendingImageFile);
        }
        pendingImageFile = null;
        pendingImageUri = null;
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

    private String valueOrEmpty(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
