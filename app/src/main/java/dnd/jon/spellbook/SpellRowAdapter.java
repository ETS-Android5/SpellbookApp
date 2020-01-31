package dnd.jon.spellbook;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import android.util.Pair;

import org.javatuples.Sextet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiFunction;

import dnd.jon.spellbook.databinding.SpellRowBinding;

public class SpellRowAdapter extends RecyclerView.Adapter<SpellRowAdapter.SpellRowHolder> implements Filterable {

    private static final Object sharedLock = new Object();

    // Filters for SpellFilter
    private static final BiFunction<Spell,Sourcebook,Boolean> sourcebookFilter = (spell, sourcebook) -> spell.getSourcebook() == sourcebook;
    private static final BiFunction<Spell,School,Boolean> schoolFilter = (spell, school) -> spell.getSchool() == school;
    private static final BiFunction<Spell, CastingTime.CastingTimeType,Boolean> castingTimeTypeFilter = (spell, castingTimeType) -> spell.getCastingTime().type == castingTimeType;
    private static final BiFunction<Spell, Duration.DurationType, Boolean> durationTypeFilter = (spell, durationType) -> spell.getDuration().type == durationType;
    private static final BiFunction<Spell, Range.RangeType, Boolean> rangeTypeFilter = (spell, rangeType) -> spell.getRange().type == rangeType;

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

        private <E extends Enum<E>> boolean filterThroughArray(Spell spell, E[] enums, BiFunction<Spell,E,Boolean> filter) {
            for (E e : enums) {
                if (filter.apply(spell, e)) {
                    return false;
                }
            }
            return true;
        }

        private boolean filterItem(Spell s, Sourcebook[] visibleSourcebooks, CasterClass[] visibleClasses, School[] visibleSchools, CastingTime.CastingTimeType[] visibleCastingTimeTypes, Duration.DurationType[] visibleDurationTypes, Range.RangeType[] visibleRangeTypes, Pair<CastingTime,CastingTime> castingTimeBounds, Pair<Duration,Duration> durationBounds, Pair<Range,Range> rangeBounds, boolean isText, String text) {

            // Get the spell name
            final String spellName = s.getName().toLowerCase();

            // Run through the various filtering fields

            // Level
            final int spellLevel = s.getLevel();
            if ( (spellLevel > cp.getMaxSpellLevel()) || (spellLevel < cp.getMinSpellLevel()) ) { return true; }

            // Sourcebooks
            final boolean sourcebookHide = filterThroughArray(s, visibleSourcebooks, sourcebookFilter);
            if (sourcebookHide) { return true; }

            // Classes
            final boolean classHide = filterThroughArray(s, visibleClasses, Spell::usableByClass);
            if (classHide) { return true; }

            // Schools
            final boolean schoolHide = filterThroughArray(s, visibleSchools, schoolFilter);
            if (schoolHide) { return true; }

            // Casting time types
            final boolean castingTimeTypeHide = filterThroughArray(s, visibleCastingTimeTypes, castingTimeTypeFilter);
            if (castingTimeTypeHide) { return true; }

            // Duration types
            final boolean durationTypeHide = filterThroughArray(s, visibleDurationTypes, durationTypeFilter);
            if (durationTypeHide) { return true; }

            // Range types
            final boolean rangeTypeHide = filterThroughArray(s, visibleRangeTypes, rangeTypeFilter);
            if (rangeTypeHide) { return true; }

            // Casting time bounds
            if (castingTimeBounds != null) {
                CastingTime castingTime = s.getCastingTime();
                if (castingTime.compareTo(castingTimeBounds.first) < 0 || castingTime.compareTo(castingTimeBounds.second) < 0) { return true; }
            }

            // Duration bounds
            if (durationBounds != null) {
                Duration duration = s.getDuration();
                if (duration.compareTo(durationBounds.first) < 0 || duration.compareTo(durationBounds.second) < 0) { return true; }
            }

            // Range bounds
            if (rangeBounds != null) {
                Range range = s.getRange();
                if (range.compareTo(rangeBounds.first) < 0 || range.compareTo(rangeBounds.second) < 0) { return true; }
            }


            // The rest of the filtering conditions
            boolean toHide = (cp.filterFavorites() && !cp.isFavorite(s));
            toHide = toHide || (cp.filterKnown() && !cp.isKnown(s));
            toHide = toHide || (cp.filterPrepared() && !cp.isPrepared(s));
            toHide = toHide || (isText && !spellName.contains(text));
            return toHide;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            synchronized (sharedLock) {
                // Filter the list of spells
                final String searchText = (constraint != null) ? constraint.toString() : "";
                final FilterResults filterResults = new FilterResults();
                filteredSpellList = new ArrayList<>();
                final Sourcebook[] visibleSourcebooks = cp.getVisibleValues(Sourcebook.class);
                final CasterClass[] visibleClasses = cp.getVisibleValues(CasterClass.class);
                final School[] visibleSchools = cp.getVisibleValues(School.class);
                final CastingTime.CastingTimeType[] visibleCastingTimeTypes = cp.getVisibleValues(CastingTime.CastingTimeType.class);
                final Duration.DurationType[] visibleDurationTypes = cp.getVisibleValues(Duration.DurationType.class);
                final Range.RangeType[] visibleRangeTypes = cp.getVisibleValues(Range.RangeType.class);
                final boolean isText = !searchText.isEmpty();
                Pair<CastingTime,CastingTime> castingTimeMinMax = null;
                if (cp.getSpanningTypeVisibility(CastingTime.CastingTimeType.class)) {
                    Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, String, String> data = cp.getQuantityRangeInfo(CastingTime.CastingTimeType.class);
                    final int minNumber = SpellbookUtils.parseFromString(data.getValue4(), 0);
                    final int maxNumber = SpellbookUtils.parseFromString(data.getValue5(), Integer.MAX_VALUE);
                    if (minNumber != 0 || maxNumber != Integer.MAX_VALUE) {
                        final CastingTime minCastingTime = new CastingTime(CastingTime.CastingTimeType.TIME, minNumber, (TimeUnit) data.getValue2(), "");
                        final TimeUnit maxUnit = (maxNumber != Integer.MAX_VALUE) ? (TimeUnit) data.getValue3() : TimeUnit.SECOND;
                        final CastingTime maxCastingTime = new CastingTime(CastingTime.CastingTimeType.TIME, maxNumber, maxUnit, "");
                        castingTimeMinMax = new Pair<>(minCastingTime, maxCastingTime);
                    }
                }
                Pair<Duration,Duration> durationMinMax = null;
                if (cp.getSpanningTypeVisibility(Duration.DurationType.class)) {
                    Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, String, String> data = cp.getQuantityRangeInfo(Duration.DurationType.class);
                    final int minNumber = SpellbookUtils.parseFromString(data.getValue4(), 0);
                    final int maxNumber = SpellbookUtils.parseFromString(data.getValue5(), Integer.MAX_VALUE);
                    if (minNumber != 0 || maxNumber != Integer.MAX_VALUE) {
                        final Duration minDuration = new Duration(Duration.DurationType.SPANNING, minNumber, (TimeUnit) data.getValue2(), "");
                        final TimeUnit maxUnit = (maxNumber != Integer.MAX_VALUE) ? (TimeUnit) data.getValue3() : TimeUnit.SECOND;
                        final Duration maxDuration = new Duration(Duration.DurationType.SPANNING, maxNumber, maxUnit, "");
                        durationMinMax = new Pair<>(minDuration, maxDuration);
                    }
                }
                Pair<Range,Range> rangeMinMax = null;
                if (cp.getSpanningTypeVisibility(Range.RangeType.class)) {
                    Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, String, String> data = cp.getQuantityRangeInfo(Range.RangeType.class);
                    final int minNumber = SpellbookUtils.parseFromString(data.getValue4(), 0);
                    final int maxNumber = SpellbookUtils.parseFromString(data.getValue5(), Integer.MAX_VALUE);
                    if (minNumber != 0 || maxNumber != Integer.MAX_VALUE) {
                        final Range minRange = new Range(Range.RangeType.RANGED, minNumber, (LengthUnit) data.getValue2(), "");
                        final LengthUnit maxUnit = (maxNumber != Integer.MAX_VALUE) ? (LengthUnit) data.getValue3() : LengthUnit.FOOT;
                        final Range maxRange = new Range(Range.RangeType.RANGED, maxNumber, maxUnit, "");
                        rangeMinMax = new Pair<>(minRange, maxRange);
                    }
                }
                for (Spell s : spellList) {
                    if (!filterItem(s, visibleSourcebooks, visibleClasses, visibleSchools, visibleCastingTimeTypes, visibleDurationTypes, visibleRangeTypes, castingTimeMinMax, durationMinMax, rangeMinMax, isText, searchText)) {
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
