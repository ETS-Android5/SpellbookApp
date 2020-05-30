package dnd.jon.spellbook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import dnd.jon.spellbook.databinding.SpellTableBinding;

public class SpellTableFragment extends Fragment {

    private SpellTableBinding binding;
    private SpellbookViewModel spellbookViewModel;
    private SpellRowAdapter adapter;
    private boolean filterPending = false;
    private boolean sortPending = false;
    private int rootVisibility;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SpellTableBinding.inflate(inflater);
        spellbookViewModel = new ViewModelProvider(this).get(SpellbookViewModel.class);

        adapter = new SpellRowAdapter(getContext());
        spellbookViewModel.getAllSpells().observe(this, adapter::setSpells);
        binding.spellRecycler.setAdapter(adapter);

        setupSwipeRefreshLayout();

        // If there are pending sorts
        final View rootView = binding.getRoot();
        rootVisibility = rootView.getVisibility();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int newVisibility = rootView.getVisibility();
            if (rootVisibility != newVisibility) {
                rootVisibility = newVisibility;
                if (newVisibility == View.VISIBLE) {
                    sortIfPending();
                    filterIfPending();
                }
            }
        });

        // Filter and sort when dictated by the view model
        final LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        spellbookViewModel.getFilterNeeded().observe(lifecycleOwner, this::onSortFlagSet);
        spellbookViewModel.getSortNeeded().observe(lifecycleOwner, this::onFilterFlagSet);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupSwipeRefreshLayout() {
        // Set up the 'swipe down to filter' behavior of the RecyclerView
        final SwipeRefreshLayout swipeLayout = binding.swipeRefreshLayout;
        swipeLayout.setOnRefreshListener(() -> {
            spellbookViewModel.setFilterNeeded(true);
            swipeLayout.setRefreshing(false);
        });

        // Configure the refreshing colors
        swipeLayout.setColorSchemeResources(R.color.darkBrown, R.color.lightBrown, R.color.black);
    }

    private void onSortFlagSet(boolean flag) {
        if (!flag) { return; }
        if (rootVisibility == View.VISIBLE) {
            adapter.sort();
        } else {
            sortPending = true;
        }
    }

    private void onFilterFlagSet(boolean flag) {
        if (!flag) { return; }
        if (rootVisibility == View.VISIBLE) {
            adapter.filter();
        } else {
            filterPending = true;
        }
    }

    private void sortIfPending() {
        if (sortPending) {
            adapter.sort();
            sortPending = false;
            spellbookViewModel.setSortNeeded(false);
        }
    }

    private void filterIfPending() {
        if (filterPending) {
            adapter.filter();
            filterPending = false;
            spellbookViewModel.setFilterNeeded(false);
        }
    }

}
