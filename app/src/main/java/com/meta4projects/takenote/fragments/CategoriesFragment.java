package com.meta4projects.takenote.fragments;

import static android.app.Activity.RESULT_OK;
import static com.meta4projects.takenote.others.Utils.categoryNames;
import static com.meta4projects.takenote.others.Utils.loadNativeAd;
import static com.meta4projects.takenote.others.Utils.showToast;
import static com.meta4projects.takenote.others.Utils.updateAllCategories;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.CategoryActivity;
import com.meta4projects.takenote.activities.MainActivity;
import com.meta4projects.takenote.adapters.CategoriesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.others.Utils;

import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment {

    public static final String CATEGORY_NAME_EXTRA = "com.meta4projects.takenote.adapters.CATEGORY_NAME";
    private final List<Category> categoryList = new ArrayList<>();

    private RecyclerView recyclerViewCategories;
    private TextView textViewEmptyCategory;
    private CategoriesAdapter categoryAdapter;
    private ActivityResultLauncher<Intent> categoryLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        categoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) updateCategories();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        MainActivity mainActivity = (MainActivity) requireActivity();
        ImageView hamburger = view.findViewById(R.id.hamburger);
        ImageView search = view.findViewById(R.id.search);
        hamburger.setOnClickListener(v -> mainActivity.hamburgerClick());
        search.setOnClickListener(v -> mainActivity.searchClick());

        recyclerViewCategories = view.findViewById(R.id.recyclerViewCategories);
        textViewEmptyCategory = view.findViewById(R.id.text_empty_category);

        TemplateView templateView = view.findViewById(R.id.fragment_categories_native_ad_notes).findViewById(R.id.native_ad);
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_category_fragment_unit_id));

        categoryAdapter = new CategoriesAdapter(categoryList, true);
        categoryAdapter.setListener(categoryName -> {
            Intent intent = new Intent(getActivity(), CategoryActivity.class);
            intent.putExtra(CATEGORY_NAME_EXTRA, categoryName);
            categoryLauncher.launch(intent);
        });

        recyclerViewCategories.setAdapter(categoryAdapter);
        recyclerViewCategories.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        FloatingActionButton buttonAddCategory = view.findViewById(R.id.button_categories_add_category);
        buttonAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        setCategories();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setCategories();
    }

    private void setEmptyViewsCategory(List<Category> categories) {
        if (categories.isEmpty()) textViewEmptyCategory.setVisibility(View.VISIBLE);
        else textViewEmptyCategory.setVisibility(View.GONE);
    }

    private void setCategories() {
        AsyncTask.execute(() -> {
            final List<Category> categories = NoteDatabase.getINSTANCE(requireContext().getApplicationContext()).categoryDao().getAllCategories();
            updateAllCategories(categories, getActivity());

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(categories);
                categoryAdapter.notifyDataSetChanged();
                setEmptyViewsCategory(categories);
            });
        });
    }

    private void showAddCategoryDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_add_category, requireActivity().findViewById(R.id.layout_add_category_dialog), false);
        final AlertDialog dialogAddCategory = Utils.getDialogView(requireContext(), view);

        final EditText editTextCategory = view.findViewById(R.id.input_add_category);

        view.findViewById(R.id.text_add_category).setOnClickListener(v -> {
            final String categoryName = editTextCategory.getText().toString().trim();
            boolean nameExists = false;

            for (String name : categoryNames) {
                if (categoryName.equalsIgnoreCase(name)) {
                    nameExists = true;
                    break;
                }
            }

            if (nameExists) showToast("category name already exists!", getActivity());
            else if (categoryName.isEmpty())
                showToast("category name cannot be empty!", getActivity());
            else {
                dialogAddCategory.dismiss();
                AsyncTask.execute(() -> {
                    final Category category = new Category(categoryName);
                    NoteDatabase.getINSTANCE(requireContext().getApplicationContext()).categoryDao().insertCategory(category);

                    final List<Category> categories = NoteDatabase.getINSTANCE(requireContext().getApplicationContext()).categoryDao().getAllCategories();
                    updateAllCategories(categories, getActivity());

                    requireActivity().runOnUiThread(() -> {
                        int lastIndex = categoryList.size();
                        categoryList.add(lastIndex, category);
                        categoryAdapter.notifyItemInserted(lastIndex);
                        recyclerViewCategories.smoothScrollToPosition(lastIndex);
                        setEmptyViewsCategory(categories);

                        showToast("new category added successfully!", getActivity());
                    });
                });
            }
        });

        view.findViewById(R.id.text_cancel_category).setOnClickListener(v -> dialogAddCategory.dismiss());

        dialogAddCategory.show();
    }

    private void updateCategories() {
        AsyncTask.execute(() -> {
            final List<Category> categories = NoteDatabase.getINSTANCE(requireContext().getApplicationContext()).categoryDao().getAllCategories();
            updateAllCategories(categories, getActivity());

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(categories);
                categoryAdapter.notifyDataSetChanged();
                setEmptyViewsCategory(categories);
            });
        });
    }
}
