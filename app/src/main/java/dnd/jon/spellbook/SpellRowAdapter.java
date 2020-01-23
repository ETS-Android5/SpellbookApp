package dnd.jon.spellbook;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;

import dnd.jon.spellbook.databinding.SpellRowBinding;

public class SpellRowAdapter extends RecyclerView.Adapter<SpellRowAdapter.SpellRowHolder> implements Filterable {

    private static final Object sharedLock = new Object();

    // Inner class for holding the spell row views
    public class SpellRowHolder extends RecyclerView.ViewHolder {

        private Spell spell = null;
        private final SpellRowBinding binding;
        private MainActivity main;
        private Runnable postToggleAction = () -> {};

        // For convenience, we construct the adapter directly from the SpellRowBinding generated from the XML
        public SpellRowHolder(SpellRowBinding b) {
            super(b.getRoot());
            binding = b;
            main = (MainActivity) b.getRoot().getContext();
            itemView.setTag(this);
            itemView.setOnClickListener(listener);
            //itemView.setOnLongClickListener(longListener);
        }

        public void bind(Spell s) {
            spell = s;
            binding.setSpell(spell);
            binding.executePendingBindings();

            //Set the buttons to show the appropriate images
            if (main != null && main.getCharacterProfile() != null && spell != null) {
                binding.spellRowFavoriteButton.set(main.getCharacterProfile().isFavorite(spell));
                binding.spellRowPreparedButton.set(main.getCharacterProfile().isPrepared(spell));
                binding.spellRowKnownButton.set(main.getCharacterProfile().isKnown(spell));
            }


            // Set button callbacks
            postToggleAction = () -> {
                main.saveCharacterProfile();
                main.updateSpellWindow(spell, main.getCharacterProfile().isFavorite(spell), main.getCharacterProfile().isPrepared(spell), main.getCharacterProfile().isKnown(spell));
            };
            binding.spellRowFavoriteButton.setCallback( () -> { main.getCharacterProfile().toggleFavorite(spell); postToggleAction.run(); } );
            binding.spellRowPreparedButton.setCallback( () -> { main.getCharacterProfile().togglePrepared(spell); postToggleAction.run(); } );
            binding.spellRowKnownButton.setCallback( () -> { main.getCharacterProfile().toggleKnown(spell); postToggleAction.run(); } );

        }

        public Spell getSpell() { return spell; }
    }

    // Inner class for filtering the list
    private class SpellFilter extends Filter {

        private final CharacterProfile cp;

        SpellFilter(CharacterProfile cp) {
            this.cp = cp;
        }

        boolean filterItem(Spell s, CasterClass[] visibleClasses, School[] visibleSchools, boolean isText, String text) {

            // Get the spell name
            final String spellName = s.getName().toLowerCase();

            // Run through the various filtering fields

            // Level
            final int spellLevel = s.getLevel();
            if ( (spellLevel > cp.getMaxSpellLevel()) || (spellLevel < cp.getMinSpellLevel()) ) { return true; }

            // Classes
            boolean classHide = true;
            for (CasterClass casterClass : visibleClasses) {
                if (s.usableByClass(casterClass)) {
                    classHide = false;
                    break;
                }
            }
            if (classHide) { return true; }

            // Schools
            boolean schoolHide = true;
            for (School school : visibleSchools) {
                if (s.getSchool() == school) {
                    schoolHide = false;
                    break;
                }
            }
            if (schoolHide) { return true; }

            // The rest of the filtering conditions
            boolean toHide = (cp.filterFavorites() && !cp.isFavorite(s));
            toHide = toHide || (cp.filterKnown() && !cp.isKnown(s));
            toHide = toHide || (cp.filterPrepared() && !cp.isPrepared(s));
            toHide = toHide || (isText && !spellName.contains(text));
            toHide = toHide || (!cp.getSourcebookFilter(s.getSourcebook()));
            return toHide;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            synchronized (sharedLock) {
                // Filter the list of spells
                final String searchText = (constraint != null) ? constraint.toString() : "";
                final FilterResults filterResults = new FilterResults();
                filteredSpellList = new ArrayList<>();
                final CasterClass[] visibleClasses = cp.getVisibleClasses();
                final School[] visibleSchools = cp.getVisibleSchools();
                final boolean isText = !searchText.isEmpty();
                for (Spell s : spellList) {
                    if (!filterItem(s, visibleClasses, visibleSchools, isText, searchText)) {
                        filteredSpellList.add(s);
                    }
                }
                filterResults.values = filteredSpellList;
                filterResults.count = filteredSpellList.size();

                return filterResults;
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    // Member values
    // References to the RecyclerView and the MainActivity
    // Also the list of spells, and the click listeners
    private MainActivity main;
    private RecyclerView recyclerView;
    private final ArrayList<Spell> spellList;
    private ArrayList<Spell> filteredSpellList;
    private final View.OnClickListener listener = (View view) -> {
        final SpellRowHolder srh = (SpellRowHolder) view.getTag();
        final Spell spell = srh.getSpell();
        int pos = srh.getLayoutPosition();
        main.openSpellWindow(spell, pos);
    };
    private final View.OnLongClickListener longListener = (View view) -> {
        final SpellRowHolder srh = (SpellRowHolder) view.getTag();
        final Spell spell = srh.getSpell();
        main.openSpellPopup(view, spell);
        return true;
    };


    // Constructor from the list of spells
    SpellRowAdapter(ArrayList<Spell> spells) {
        spellList = spells;
        filteredSpellList = spells;
    }

    // Filterable methods
    public Filter getFilter() {
        synchronized (sharedLock) {
            return new SpellFilter(main.getCharacterProfile());
        }
    }

    // For use from MainActivity
    void filter() {
        synchronized (sharedLock) {
            getFilter().filter(null);
        }
    }
    void singleSort(SortField sf, boolean reverse) {
        synchronized (sharedLock) {
            ArrayList<Pair<SortField,Boolean>> sortParameters = new ArrayList<Pair<SortField,Boolean>>() {{
                add(new Pair<>(sf, reverse));
            }};
            Collections.sort(spellList, new SpellComparator(sortParameters));
            filter();
            notifyDataSetChanged();

//            Collections.sort(filteredSpellList, new SpellOneFieldComparator(sf1, reverse));
//            notifyDataSetChanged();
        }
    }

    void doubleSort(SortField sf1, SortField sf2, boolean reverse1, boolean reverse2) {
        synchronized (sharedLock) {
//            Collections.sort(filteredSpellList, new SpellTwoFieldComparator(sf1, sf2, reverse1, reverse2));
//            notifyDataSetChanged();

            ArrayList<Pair<SortField,Boolean>> sortParameters = new ArrayList<Pair<SortField,Boolean>>() {{
                add(new Pair<>(sf1, reverse1));
                add(new Pair<>(sf2, reverse2));
            }};
            Collections.sort(spellList, new SpellComparator(sortParameters));
            filter();
            notifyDataSetChanged();
        }
    }

    // ViewHolder methods
    public SpellRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        SpellRowBinding binding = SpellRowBinding.inflate(inflater, parent, false);
        return new SpellRowHolder(binding);
    }

    public void onBindViewHolder(SpellRowHolder holder, int position) {
        final Spell spell = filteredSpellList.get(position);
        holder.bind(spell);
    }

    public int getItemCount() {
        return filteredSpellList.size();
    }

    // When attached to a recycler view, set the relevant values
    @Override
    public void onAttachedToRecyclerView(RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        recyclerView = rv;
        main = (MainActivity) recyclerView.getContext();
    }
}
