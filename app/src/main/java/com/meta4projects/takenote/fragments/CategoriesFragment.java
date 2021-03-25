package com.meta4projects.takenote.fragments;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.adapters.CategoriesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.meta4projects.takenote.others.Util.categoryNames;
import static com.meta4projects.takenote.others.Util.showToast;
import static com.meta4projects.takenote.others.Util.updateAllCategories;

public class CategoriesFragment extends Fragment {

    private RecyclerView recyclerViewCategories;

    private ImageView imageViewEmptyCategory;
    private TextView textViewEmptyCategory;

    private CategoriesAdapter categoryAdapter;
    private final List<Category> categoryList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        recyclerViewCategories = view.findViewById(R.id.recyclerViewCategories);

        imageViewEmptyCategory = view.findViewById(R.id.empty_image_add_category);
        textViewEmptyCategory = view.findViewById(R.id.text_empty_category);

        categoryAdapter = new CategoriesAdapter(categoryList, this, true);
        recyclerViewCategories.setAdapter(categoryAdapter);
        recyclerViewCategories.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        FloatingActionButton buttonAddCategory = view.findViewById(R.id.button_categories_add_category);
        buttonAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });

        setCategories();

        return view;
    }

    private void setEmptyViewsCategory(List<Category> categories) {
        if (categories.size() == 0) {
            imageViewEmptyCategory.setVisibility(View.VISIBLE);
            textViewEmptyCategory.setVisibility(View.VISIBLE);
        } else {
            imageViewEmptyCategory.setVisibility(View.GONE);
            textViewEmptyCategory.setVisibility(View.GONE);
        }
    }

    private void setCategories() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Category> categories = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).categoryDao().getAllCategories();
                updateAllCategories(categories, getActivity());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        categoryList.addAll(categories);
                        categoryAdapter.notifyDataSetChanged();
                        setEmptyViewsCategory(categories);
                    }
                });
            }
        });
    }

    private void showAddCategoryDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_add_category, (ViewGroup) Objects.requireNonNull(getActivity()).findViewById(R.id.layout_add_category_dialog), false);

        final AlertDialog dialogAddCategory = new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();

        if (dialogAddCategory.getWindow() != null) {
            dialogAddCategory.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        final EditText editTextCategory = view.findViewById(R.id.input_add_category);

        view.findViewById(R.id.text_add_category).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String categoryName = editTextCategory.getText().toString().trim();
                boolean nameExists = false;

                for (String name : categoryNames) {
                    if (categoryName.equalsIgnoreCase(name)) {
                        nameExists = true;
                        break;
                    }
                }

                if (nameExists) {
                    showToast("category name already exists!", getActivity());
                } else {
                    dialogAddCategory.dismiss();
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            final Category category = new Category(categoryName);
                            NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).categoryDao().insertCategory(category);

                            final List<Category> categories = NoteDatabase.getINSTANCE(getActivity().getApplicationContext()).categoryDao().getAllCategories();
                            updateAllCategories(categories, getActivity());

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int lastIndex = categoryList.size();
                                    categoryList.add(lastIndex, category);
                                    categoryAdapter.notifyItemInserted(lastIndex);
                                    recyclerViewCategories.smoothScrollToPosition(lastIndex);
                                    setEmptyViewsCategory(categories);

                                    showToast("new category added successfully!", getActivity());
                                }
                            });
                        }
                    });
                }
            }
        });

        view.findViewById(R.id.text_cancel_category).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogAddCategory.dismiss();
            }
        });

        dialogAddCategory.show();
    }

    private void updateCategories() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Category> categories = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).categoryDao().getAllCategories();
                updateAllCategories(categories, getActivity());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        categoryList.clear();
                        categoryList.addAll(categories);
                        categoryAdapter.notifyDataSetChanged();
                        setEmptyViewsCategory(categories);
                    }
                });
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            updateCategories();
        }
    }
}
