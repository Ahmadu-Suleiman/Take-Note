package com.meta4projects.takenote.fragments;

import static android.app.Activity.RESULT_OK;
import static com.meta4projects.takenote.fragments.CategoriesFragment.CATEGORY_NAME_EXTRA;
import static com.meta4projects.takenote.others.Utils.categoryNames;
import static com.meta4projects.takenote.others.Utils.getDialogView;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.CategoryActivity;
import com.meta4projects.takenote.activities.MainActivity;
import com.meta4projects.takenote.activities.NoteActivity;
import com.meta4projects.takenote.adapters.CategoriesAdapter;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

    public static final String ADD_NEW_NOTE_EXTRA = "com.meta4projects.takenote.ADD_NOTE";
    public static final String EDIT_NOTE_EXTRA = "com.meta4projects.takenote.activities.EDIT_NOTE";
    public static final String EDIT_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.EDIT_NOTE_ID";

    private final List<Note> notesList = new ArrayList<>();
    private final List<Category> categoryList = new ArrayList<>();

    private RecyclerView recyclerViewAllNotes, recyclerViewAllCategories;
    private TextView textViewEmptyNote, textViewCategoryTitle;
    private NotesAdapter notesAdapter;
    private CategoriesAdapter allCategoryAdapter;
    private ActivityResultLauncher<Intent> categoryLauncher;
    private ActivityResultLauncher<Intent> addNewNoteLauncher;
    private ActivityResultLauncher<Intent> editNoteLauncher;

    private ReviewInfo reviewInfo;
    private ReviewManager reviewManager;

    private ExtendedFloatingActionButton addFab;
    private FloatingActionButton buttonAddNote, buttonAddCategory;
    private TextView textViewAddNote, textViewAddCategory;

    private Boolean isAllFabsVisible;
    private int clickedNotePosition;

    private InterstitialAd interstitialAdNote;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialiseLaunchers();
        loadNoteInterstitial();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        MainActivity mainActivity = (MainActivity) requireActivity();
        ImageView hamburger = view.findViewById(R.id.hamburger);
        ImageView search = view.findViewById(R.id.search);
        hamburger.setOnClickListener(v -> mainActivity.hamburgerClick());
        search.setOnClickListener(v -> mainActivity.searchClick());

        recyclerViewAllNotes = view.findViewById(R.id.recyclerViewAllNotes);
        recyclerViewAllCategories = view.findViewById(R.id.recyclerViewAllCategories);

        textViewCategoryTitle = view.findViewById(R.id.text_categories_main);
        textViewEmptyNote = view.findViewById(R.id.text_empty_note_main);

        notesAdapter = new NotesAdapter(notesList, getActivity());
        recyclerViewAllNotes.setAdapter(notesAdapter);
        notesAdapter.setListener((position, note) -> {
            Intent intent = new Intent(getActivity(), NoteActivity.class);
            intent.putExtra(EDIT_NOTE_EXTRA, true);
            intent.putExtra(EDIT_NOTE_ID_EXTRA, note.getNoteId());

            clickedNotePosition = position;
            editNoteLauncher.launch(intent);
        });
        recyclerViewAllNotes.setLayoutManager(new LinearLayoutManager(getActivity()));

        allCategoryAdapter = new CategoriesAdapter(categoryList);
        allCategoryAdapter.setListener(categoryName -> {
            Intent intent = new Intent(getActivity(), CategoryActivity.class);
            intent.putExtra(CATEGORY_NAME_EXTRA, categoryName);
            categoryLauncher.launch(intent);
        });
        recyclerViewAllCategories.setAdapter(allCategoryAdapter);
        recyclerViewAllCategories.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

        TemplateView templateView = view.findViewById(R.id.note_native_ad_main).findViewById(R.id.native_ad);
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_main_note_unit_id));

        setActionButtons(view);
        setNotes();
        setCategories();

        reviewManager = ReviewManagerFactory.create(requireContext());
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) reviewInfo = task.getResult();
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setNotes();
        review();
    }


    private void loadNoteInterstitial() {
        InterstitialAd.load(requireContext(), getString(R.string.interstitial_note_unit_id), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                interstitialAdNote = interstitialAd;

                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        addNote();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        interstitialAdNote = null;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        addNote();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAdNote = null;
            }
        });
    }

    private void setActionButtons(View view) {
        addFab = view.findViewById(R.id.add_fab);
        buttonAddNote = view.findViewById(R.id.button_add_new_note);
        buttonAddCategory = view.findViewById(R.id.button_add_new_category);

        textViewAddNote = view.findViewById(R.id.add_new_note_text);
        textViewAddCategory = view.findViewById(R.id.add_category_text);

        buttonAddNote.setVisibility(View.GONE);
        buttonAddCategory.setVisibility(View.GONE);
        textViewAddNote.setVisibility(View.GONE);
        textViewAddCategory.setVisibility(View.GONE);

        isAllFabsVisible = false;
        addFab.shrink();

        addFab.setOnClickListener(v -> {
            if (!isAllFabsVisible) {
                buttonAddNote.show();
                buttonAddCategory.show();
                textViewAddNote.setVisibility(View.VISIBLE);
                textViewAddCategory.setVisibility(View.VISIBLE);


                addFab.extend();
                isAllFabsVisible = true;
            } else {
                buttonAddNote.hide();
                buttonAddCategory.hide();
                textViewAddNote.setVisibility(View.GONE);
                textViewAddCategory.setVisibility(View.GONE);

                addFab.shrink();
                isAllFabsVisible = false;
            }
        });

        buttonAddNote.setOnClickListener(v -> {
            if (interstitialAdNote != null) interstitialAdNote.show(requireActivity());
            else addNote();
        });

        buttonAddCategory.setOnClickListener(v -> showAddCategoryDialog());

    }

    private void addNote() {
        Intent intent = new Intent(getActivity(), NoteActivity.class);
        intent.putExtra(ADD_NEW_NOTE_EXTRA, true);
        addNewNoteLauncher.launch(intent);
        loadNoteInterstitial();
    }

    private void setNotes() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).noteDao().getAllNotes();

            requireActivity().runOnUiThread(() -> {
                notesList.clear();
                notesList.addAll(notes);
                notesAdapter.notifyDataSetChanged();
                setEmptyViewsNote(notes);
            });
        });
    }

    private void initialiseLaunchers() {
        addNewNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                onAddNote();
                updateCategories();
            }
        });

        editNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null) {
                boolean isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false);

                if (isMovedToTrash) onMoveNote();
                else onEditNote(data);
                updateCategories();
            }
        });

        categoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                updateAllNotes();
                updateCategories();
            }
        });
    }

    private void setEmptyViewsNote(List<Note> notes) {
        if (notes.isEmpty()) textViewEmptyNote.setVisibility(View.VISIBLE);
        else textViewEmptyNote.setVisibility(View.GONE);
    }

    private void setEmptyViewsCategory(List<Category> categories) {
        if (categories.size() == 0) {
            textViewCategoryTitle.setVisibility(View.GONE);
            recyclerViewAllCategories.setVisibility(View.GONE);
        } else {
            textViewCategoryTitle.setVisibility(View.VISIBLE);
            recyclerViewAllCategories.setVisibility(View.VISIBLE);
        }
    }

    private void setCategories() {
        AsyncTask.execute(() -> {
            final List<Category> categories = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).categoryDao().getAllCategories();
            updateAllCategories(categories, getActivity());

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(categories);
                allCategoryAdapter.notifyDataSetChanged();
                setEmptyViewsCategory(categories);
            });
        });
    }

    private void showAddCategoryDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_add_category, requireActivity().findViewById(R.id.layout_add_category_dialog), false);

        final AlertDialog dialogAddCategory = getDialogView(requireContext(), view);

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
                    NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).categoryDao().insertCategory(category);

                    final List<Category> categories = NoteDatabase.getINSTANCE(requireContext().getApplicationContext()).categoryDao().getAllCategories();
                    updateAllCategories(categories, getActivity());

                    requireActivity().runOnUiThread(() -> {
                        int lastIndex = categoryList.size();
                        categoryList.add(lastIndex, category);
                        allCategoryAdapter.notifyItemInserted(lastIndex);
                        recyclerViewAllCategories.smoothScrollToPosition(lastIndex);
                        setEmptyViewsCategory(categories);

                        showToast("new category added successfully!", getActivity());
                    });
                });
            }
        });

        view.findViewById(R.id.text_cancel_category).setOnClickListener(v -> dialogAddCategory.dismiss());
        dialogAddCategory.show();
    }

    private void onAddNote() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).noteDao().getAllNotes();

            requireActivity().runOnUiThread(() -> {
                notesList.add(0, notes.get(0));
                notesAdapter.notifyItemInserted(0);
                recyclerViewAllNotes.smoothScrollToPosition(0);
                showToast("note added successfully!", getActivity());
                setEmptyViewsNote(notes);
            });
        });
    }

    private void onEditNote(final Intent data) {
        updateAllNotes();

        boolean savedChanges = data.getBooleanExtra(NoteActivity.SAVED_CHANGES_EXTRA, false);
        if (savedChanges) showToast("note saved successfully!", getActivity());
        else showToast("note edited successfully!", getActivity());
    }

    private void onMoveNote() {
        notesList.remove(clickedNotePosition);

        notesAdapter.notifyItemRemoved(clickedNotePosition);
        showToast("note moved successfully!", getActivity());
        setEmptyViewsNote(notesList);
    }

    private void updateAllNotes() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).noteDao().getAllNotes();

            requireActivity().runOnUiThread(() -> {
                notesList.clear();
                notesList.addAll(notes);
                notesAdapter.notifyDataSetChanged();
                setEmptyViewsNote(notes);
                recyclerViewAllNotes.smoothScrollToPosition(0);
            });
        });
    }

    private void updateCategories() {
        AsyncTask.execute(() -> {
            final List<Category> categories = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).categoryDao().getAllCategories();
            updateAllCategories(categories, getActivity());

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(categories);
                allCategoryAdapter.notifyDataSetChanged();
                setEmptyViewsCategory(categories);
            });
        });
    }

    private void review() {
        if (reviewInfo != null) reviewManager.launchReviewFlow(requireActivity(), reviewInfo);
    }

    @NonNull
    @Override
    public String toString() {
        return "MainFragment{}";
    }
}