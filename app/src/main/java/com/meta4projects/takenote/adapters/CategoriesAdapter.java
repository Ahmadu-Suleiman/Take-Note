package com.meta4projects.takenote.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.CategoryActivity;
import com.meta4projects.takenote.database.entities.Category;

import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryHolder> {

    public static final int CATEGORY_REQUEST_CODE = 333;

    public static final String CATEGORY_NAME_EXTRA = "com.meta4projects.takenote.adapters.CATEGORY_NAME";
    boolean isBig;
    private final List<Category> categories;
    private final Fragment fragment;

    public CategoriesAdapter(List<Category> categories, Fragment fragment) {
        this.categories = categories;
        this.fragment = fragment;
    }

    public CategoriesAdapter(List<Category> categories, Fragment fragment, boolean isBig) {
        this.categories = categories;
        this.fragment = fragment;
        this.isBig = isBig;
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (isBig) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_category_detailed, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_category, parent, false);
        }

        return new CategoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, final int position) {
        final Category category = categories.get(position);
        holder.setCategory(category);
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(fragment.getActivity(), CategoryActivity.class);
                intent.putExtra(CATEGORY_NAME_EXTRA, category.getName());
                fragment.startActivityForResult(intent, CATEGORY_REQUEST_CODE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class CategoryHolder extends RecyclerView.ViewHolder {

        final ConstraintLayout layout;
        final TextView textViewCategoryName;
        final TextView textViewCategoryNumber;

        public CategoryHolder(@NonNull View itemView) {
            super(itemView);

            layout = itemView.findViewById(R.id.layout_category);
            textViewCategoryName = itemView.findViewById(R.id.textView_category_name);
            textViewCategoryNumber = itemView.findViewById(R.id.textView_category_num);
        }

        void setCategory(Category category) {
            textViewCategoryName.setText(category.getName());
            textViewCategoryNumber.setText(String.valueOf(category.getNotesInCategory().size()));
        }
    }

}
