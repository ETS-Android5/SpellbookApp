package dnd.jon.spellbook;

import android.app.Application;
import android.text.TextUtils;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import org.javatuples.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SpellbookViewModel extends AndroidViewModel {

    // The repositories of spells and characters
    private final SpellRepository spellRepository;
    private final CharacterRepository characterRepository;

    // Whether or not we're on a tablet
    private final boolean onTablet;

    // The character profile itself
    // We'll update this in the database when we save
    private CharacterProfile profile;

    // These fields describe the current sorting/filtering state for this profile
    // We keep them in the ViewModel so that it's easier to alert/receive changes from views
    // When we switch profiles, these values will get saved into the character database
    private final MutableLiveData<String> currentCharacterName = new MutableLiveData<>();
    private final Map<String,SpellStatus> spellStatuses = new HashMap<>();
    private final MutableLiveData<SortField> firstSortField = new MutableLiveData<>(SortField.NAME);
    private final MutableLiveData<SortField> secondSortField = new MutableLiveData<>(SortField.NAME);
    private final MutableLiveData<Boolean> firstSortReverse = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> secondSortReverse = new MutableLiveData<>(false);
    private final MutableLiveData<StatusFilterField> statusFilter = new MutableLiveData<>(StatusFilterField.ALL);
    private final MutableLiveData<Integer> minLevel = new MutableLiveData<>(Spellbook.MIN_SPELL_LEVEL);
    private final MutableLiveData<Integer> maxLevel = new MutableLiveData<>(Spellbook.MAX_SPELL_LEVEL);
    private final MutableLiveData<Boolean> ritualFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> notRitualFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> concentrationFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> notConcentrationFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> verbalFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> notVerbalFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> somaticFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> notSomaticFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> materialFilter = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> notMaterialFilter = new MutableLiveData<>(true);
    private final EnumLiveFlags<Sourcebook> visibleSourcebooks = new EnumLiveFlags<>(Sourcebook.class, (sb) -> sb == Sourcebook.PLAYERS_HANDBOOK);
    private final EnumLiveFlags<School> visibleSchools = new EnumLiveFlags<>(School.class);
    private final EnumLiveFlags<CasterClass> visibleClasses = new EnumLiveFlags<>(CasterClass.class);
    private final EnumLiveFlags<CastingTime.CastingTimeType> visibleCastingTimeTypes = new EnumLiveFlags<>(CastingTime.CastingTimeType.class);
    private final EnumLiveFlags<Duration.DurationType> visibleDurationTypes = new EnumLiveFlags<>(Duration.DurationType.class);
    private final EnumLiveFlags<Range.RangeType> visibleRangeTypes = new EnumLiveFlags<>(Range.RangeType.class);
    private final MutableLiveData<String> filterText = new MutableLiveData<>("");

    // These maps store the current minimum and maximum quantities for each class
    private final Map<Class<? extends QuantityType>, Pair<MutableLiveData<Unit>, MutableLiveData<Integer>>> minQuantityValues = new HashMap<Class<? extends QuantityType>, Pair<MutableLiveData<Unit>, MutableLiveData<Integer>>>() {{
        for (Map.Entry<Class<? extends QuantityType>, Pair<Unit,Integer>> entry : defaultMinQuantityValues.entrySet()) {
            put(entry.getKey(), new Pair<>(new MutableLiveData<>(entry.getValue().getValue0()), new MutableLiveData<>(entry.getValue().getValue1())));
        }
    }};
    private final Map<Class<? extends QuantityType>, Pair<MutableLiveData<Unit>, MutableLiveData<Integer>>> maxQuantityValues = new HashMap<Class<? extends QuantityType>, Pair<MutableLiveData<Unit>, MutableLiveData<Integer>>>() {{
        for (Map.Entry<Class<? extends QuantityType>, Pair<Unit,Integer>> entry : defaultMaxQuantityValues.entrySet()) {
            put(entry.getKey(), new Pair<>(new MutableLiveData<>(entry.getValue().getValue0()), new MutableLiveData<>(entry.getValue().getValue1())));
        }
    }};

    // These fields describe the current spell and which of the favorite/prepared/known lists it's on
    private final MutableLiveData<Spell> currentSpell = new MutableLiveData<>();
    private final LiveData<Boolean> currentSpellFavorite = Transformations.map(currentSpell, (spell) -> getStatusForSpell(spell).isFavorite());
    private final LiveData<Boolean> currentSpellKnown = Transformations.map(currentSpell, (spell) -> getStatusForSpell(spell).isKnown());
    private final LiveData<Boolean> currentSpellPrepared = Transformations.map(currentSpell, (spell) -> getStatusForSpell(spell).isPrepared());

    // These fields control the execution of the sorting and filtering
    private boolean spellTableVisible = true; // Is the table of spells currently visible (i.e., do we need to sort/filter, or can it be delayed)?
    private boolean sortPending = false; // If true, we need to sort when the spells next become visible
    private boolean filterPending = false; // If true, we need to filter when the spells next become visible
    private final MutableLiveData<Void> sortEmitter = new MutableLiveData<>(null); // Setting this to true triggers a sort action
    private final MutableLiveData<Void> filterEmitter = new MutableLiveData<>(null); // Setting this to true triggers a filter action

    // The current list of spells
    // When filterSignal emits a signal, we get the updated spells from the database
    private final LiveData<List<Spell>> currentSpells = Transformations.switchMap(filterEmitter, (v) -> getVisibleSpells());

    // This map allows access to the item visibility flags by class
    private final Map<Class<? extends Named>, LiveMap<? extends Named, Boolean>> classToFlagsMap = new HashMap<Class<? extends Named>, LiveMap<? extends Named, Boolean>>() {{
       put(CasterClass.class, visibleClasses);
       put(Sourcebook.class, visibleSourcebooks);
       put(School.class, visibleSchools);
       put(CastingTime.CastingTimeType.class, visibleCastingTimeTypes);
       put(Duration.DurationType.class, visibleDurationTypes);
       put(Range.RangeType.class, visibleRangeTypes);
    }};

    // This map allows access to the spanning type visibility flags by clas
    private final Map<Class<? extends QuantityType>, LiveData<Boolean>> spanningVisibilities = new HashMap<Class<? extends QuantityType>, LiveData<Boolean>>() {{
       put(CastingTime.CastingTimeType.class, getVisibility(CastingTime.CastingTimeType.spanningType()));
       put(Duration.DurationType.class, getVisibility(Duration.DurationType.spanningType()));
       put(Range.RangeType.class, getVisibility(Range.RangeType.spanningType()));
    }};

    // These static maps store the default minimum and maximum quantities for each relevant class
    private static final Map<Class<? extends QuantityType>, Pair<Unit, Integer>> defaultMinQuantityValues = new HashMap<Class<? extends QuantityType>, Pair<Unit, Integer>>() {{
        put(CastingTime.CastingTimeType.class, new Pair<>(TimeUnit.SECOND, 0));
        put(Duration.DurationType.class, new Pair<>(TimeUnit.SECOND, 0));
        put(Range.RangeType.class, new Pair<>(LengthUnit.FOOT, 0));
    }};
    private static final Map<Class<? extends QuantityType>, Pair<Unit, Integer>> defaultMaxQuantityValues = new HashMap<Class<? extends QuantityType>, Pair<Unit, Integer>>() {{
        put(CastingTime.CastingTimeType.class, new Pair<>(TimeUnit.HOUR, 24));
        put(Duration.DurationType.class, new Pair<>(TimeUnit.DAY, 30));
        put(Range.RangeType.class, new Pair<>(LengthUnit.MILE, 1));
    }};

    // For getting the base values of the min or max values of a certain quantity
    // This is used internally to get the correct values to pass to the function that gets the filtered values from the repository
    private int quantityBaseValue(Map<Class<? extends QuantityType>, Pair<MutableLiveData<Unit>, MutableLiveData<Integer>>> map, Class<? extends Named> quantityType) {
        final Pair<MutableLiveData<Unit>, MutableLiveData<Integer>> data = map.get(quantityType);
        if (data == null) { return 0; }
        final Unit unit = data.getValue0().getValue();
        final Integer value = data.getValue1().getValue();
        if (unit == null || value == null) { return 0; }
        return unit.value() * value;
    }
    private int minBaseValue(Class<? extends Named> quantityType) { return quantityBaseValue(minQuantityValues, quantityType); }
    private int maxBaseValue(Class<? extends Named> quantityType) { return quantityBaseValue(maxQuantityValues, quantityType); }

    // Constructor
    public SpellbookViewModel(Application application) {
        super(application);
        spellRepository = new SpellRepository(application);
        characterRepository = new CharacterRepository(application);
        onTablet = application.getResources().getBoolean(R.bool.isTablet);
    }

    // Returns the current list of visible spells (for observation)
    LiveData<List<Spell>> getCurrentSpells() { return currentSpells; }

    // For internal use - gets the current spell list from the repository
    private LiveData<List<Spell>> getVisibleSpells() {
        final int minCastingTimeBaseValue = minBaseValue(CastingTime.CastingTimeType.class);
        final int maxCastingTimeBaseValue = maxBaseValue(CastingTime.CastingTimeType.class);
        final int minDurationBaseValue = minBaseValue(Duration.DurationType.class);
        final int maxDurationBaseValue = maxBaseValue(Duration.DurationType.class);
        final int minRangeBaseValue = minBaseValue(Range.RangeType.class);
        final int maxRangeBaseValue = maxBaseValue(Range.RangeType.class);
        final Collection<String> filterNames = getCurrentFilterNames();
        if (filterNames != null) { System.out.println("FilterNames: " + TextUtils.join(", ", filterNames)); }
        return spellRepository.getVisibleSpells(filterNames, minLevel.getValue(), maxLevel.getValue(), ritualFilter.getValue(), notRitualFilter.getValue(),
                concentrationFilter.getValue(), notConcentrationFilter.getValue(), verbalFilter.getValue(), notVerbalFilter.getValue(), somaticFilter.getValue(), notSomaticFilter.getValue(),
                materialFilter.getValue(), notMaterialFilter.getValue(), visibleSourcebooks.onValues(), visibleClasses.onValues(), visibleSchools.onValues(),
                visibleCastingTimeTypes.onValues(), minCastingTimeBaseValue, maxCastingTimeBaseValue,
                visibleDurationTypes.onValues(), minDurationBaseValue, maxDurationBaseValue,
                visibleRangeTypes.onValues(), minRangeBaseValue, maxRangeBaseValue,
                filterText.getValue(), firstSortField.getValue(), secondSortField.getValue(), firstSortReverse.getValue(), secondSortReverse.getValue()
        );
    }

    // For observing the currently selected spell and whether it's on one of the filtering lists
    LiveData<Spell> getCurrentSpell() { return currentSpell; }
    LiveData<Boolean> isCurrentSpellFavorite() { return currentSpellFavorite; }
    LiveData<Boolean> isCurrentSpellPrepared() { return currentSpellPrepared; }
    LiveData<Boolean> isCurrentSpellKnown() { return currentSpellKnown; }

    // Get the spell's SpellStatus
    SpellStatus getStatusForSpell(Spell spell) {
        final String spellName = spell.getName();
        if (spellStatuses.containsKey(spellName)) {
            return spellStatuses.get(spellName);
        } else {
            return new SpellStatus();
        }
    }

    // For observing the list of character names
    LiveData<List<String>> getAllCharacterNames() { return characterRepository.getAllCharacterNames(); }

    // The current number of characters
    int getCharactersCount() { return characterRepository.getAllCharacterNames().getValue().size(); }

    // Add and remove characters from the repository
    void addCharacter(CharacterProfile cp) { characterRepository.insert(cp); }
    //void deleteCharacter(CharacterProfile cp) { characterRepository.delete(cp); }

    // Set current state to reflect that of the profile with the given name
    void setCharacter(String name) {
        profile = characterRepository.getCharacter(name);
        currentCharacterName.setValue(profile.getName());
        firstSortField.setValue(profile.getFirstSortField());
        secondSortField.setValue(profile.getSecondSortField());
        firstSortReverse.setValue(profile.getFirstSortReverse());
        secondSortReverse.setValue(profile.getSecondSortReverse());
        statusFilter.setValue(profile.getStatusFilter());
        minLevel.setValue(profile.getMinLevel());
        maxLevel.setValue(profile.getMaxLevel());
        visibleSourcebooks.setItems(profile.getVisibleSourcebooks());
        visibleSchools.setItems(profile.getVisibleSchools());
        visibleClasses.setItems(profile.getVisibleClasses());
        visibleCastingTimeTypes.setItems(profile.getVisibleCastingTimeTypes());
        visibleDurationTypes.setItems(profile.getVisibleDurationTypes());
        visibleRangeTypes.setItems(profile.getVisibleRangeTypes());
        ritualFilter.setValue(profile.getRitualFilter());
        notRitualFilter.setValue(profile.getNotRitualFilter());
        concentrationFilter.setValue(profile.getConcentrationFilter());
        notConcentrationFilter.setValue(profile.getNotConcentrationFilter());
        verbalFilter.setValue(profile.getVerbalFilter());
        notVerbalFilter.setValue(profile.getNotVerbalFilter());
        somaticFilter.setValue(profile.getSomaticFilter());
        notSomaticFilter.setValue(profile.getNotSomaticFilter());
        materialFilter.setValue(profile.getMaterialFilter());
        notMaterialFilter.setValue(profile.getNotMaterialFilter());
        setQuantityBoundsFromProfile(CastingTime.CastingTimeType.class, CharacterProfile::getMinCastingTime, CharacterProfile::getMaxCastingTime);
        setQuantityBoundsFromProfile(Duration.DurationType.class, CharacterProfile::getMinDuration, CharacterProfile::getMaxDuration);
        setQuantityBoundsFromProfile(Range.RangeType.class, CharacterProfile::getMinRange, CharacterProfile::getMaxRange);
        spellStatuses.clear();
        spellStatuses.putAll(profile.getSpellStatuses());
    }

    // Two generic helper functions for setCharacter above
    private <T extends QuantityType> void setQuantity(Class<T> type, Quantity quantity, BiConsumer<Class<? extends QuantityType>, Unit> unitSetter, BiConsumer<Class<? extends QuantityType>,Integer> valueSetter) {
        unitSetter.accept(type, quantity.unit);
        valueSetter.accept(type, quantity.value);
    }
    private <T extends QuantityType> void setQuantityBoundsFromProfile(Class<T> type, Function<CharacterProfile,Quantity> minQuantityGetter, Function<CharacterProfile,Quantity> maxQuantityGetter) {
        if (profile == null) { return; }
        setQuantity(type, minQuantityGetter.apply(profile), this::setMinUnit, this::setMinValue);
        setQuantity(type, maxQuantityGetter.apply(profile), this::setMaxUnit, this::setMaxValue);
    }

    // Delete the character profile with the given name
    void deleteCharacter(String name) { characterRepository.deleteCharacterByName(name); }


    // Get the names of the spells on the favorite/known/prepared lists
    // It's basically the same for each, so we can just call the same function with a different predicate
    private Collection<String> getFilterNames(Predicate<SpellStatus> propertyGetter) { return spellStatuses.entrySet().stream().filter((e) -> propertyGetter.test(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList()); }
    private Collection<String> getFavoriteNames() { return getFilterNames(SpellStatus::isFavorite); }
    private Collection<String> getKnownNames() { return getFilterNames(SpellStatus::isKnown); }
    private Collection<String> getPreparedNames() { return getFilterNames(SpellStatus::isPrepared); }

    // Get the names of the spells that are on the current filter list
    // Just return on of the above functions based on a switch statement
    private Collection<String> getCurrentFilterNames() {
        final StatusFilterField sf = statusFilter.getValue();
        System.out.println("The current status filter is " + sf);
        if (sf == null) { return null; }
        switch (sf) {
            case ALL:
                return null;
            case FAVORITES:
                return getFavoriteNames();
            case PREPARED:
                return getPreparedNames();
            case KNOWN:
                return getKnownNames();
        }
        return null;
    }

    // For a view to observe whether or not a sort is needed
    LiveData<Void> getSortSignal() { return sortEmitter; }

    // Get the LiveData for the current character name, sort options, status filter field, and min and max level
    LiveData<String> getCharacterName() { return currentCharacterName; }
    LiveData<SortField> getFirstSortField() { return firstSortField; }
    LiveData<SortField> getSecondSortField() { return secondSortField; }
    LiveData<Boolean> getFirstSortReverse() { return firstSortReverse; }
    LiveData<Boolean> getSecondSortReverse() { return secondSortReverse; }
    LiveData<Integer> getMinLevel() { return minLevel; }
    LiveData<Integer> getMaxLevel() { return maxLevel; }
    LiveData<StatusFilterField> getStatusFilter() { return statusFilter; }

    // Observe whether the visibility flag for a Named item is set
    LiveData<Boolean> getVisibility(Named named) {
        final Class<? extends Named> cls = named.getClass();
        final LiveMap map = classToFlagsMap.get(cls);
        return (map != null) ? map.get(cls.cast(named)) : null;
    }

    // Observe one of the yes/no filters
    LiveData<Boolean> getRitualFilter(boolean b) { return b ? ritualFilter : notRitualFilter; }
    LiveData<Boolean> getConcentrationFilter(boolean b) { return b ? concentrationFilter : notConcentrationFilter; }
    LiveData<Boolean> getVerbalFilter(boolean b) { return b ? verbalFilter: notVerbalFilter; }
    LiveData<Boolean> getSomaticFilter(boolean b) { return b ? somaticFilter : notSomaticFilter; }
    LiveData<Boolean> getMaterialFilter(boolean b) { return b ? materialFilter : notMaterialFilter; }

    // Observe values for the min/max units and values for the quantity classes
    LiveData<Unit> getMaxUnit(Class<? extends QuantityType> quantityType) { return maxQuantityValues.get(quantityType).getValue0(); }
    LiveData<Unit> getMinUnit(Class<? extends QuantityType> quantityType) { return minQuantityValues.get(quantityType).getValue0(); }
    LiveData<Integer> getMaxValue(Class<? extends QuantityType> quantityType) { return maxQuantityValues.get(quantityType).getValue1(); }
    LiveData<Integer> getMinValue(Class<? extends QuantityType> quantityType) { return minQuantityValues.get(quantityType).getValue1(); }

    // Observe whether or not the spanning type is visible for a given QuantityType class
    LiveData<Boolean> getSpanningTypeVisible(Class<? extends QuantityType> quantityType){ return spanningVisibilities.get(quantityType); }

    // Are we on a tablet?
    boolean areOnTablet() { return onTablet; }

    // Get the default values for the min/max units and values for the quantity classes
    static Unit getDefaultMaxUnit(Class<? extends QuantityType> quantityType) { return defaultMaxQuantityValues.get(quantityType).getValue0(); }
    static Unit getDefaultMinUnit(Class<? extends QuantityType> quantityType) { return defaultMinQuantityValues.get(quantityType).getValue0(); }
    static Integer getDefaultMaxValue(Class<? extends QuantityType> quantityType) { return defaultMaxQuantityValues.get(quantityType).getValue1(); }
    static Integer getDefaultMinValue(Class<? extends QuantityType> quantityType) { return defaultMinQuantityValues.get(quantityType).getValue1(); }

    // Use this to set LiveData that a view might need to observe
    // This will avoid infinite loops
    // The second argument will perform any necessary actions after a change
    private <T> void setIfNeeded(MutableLiveData<T> liveData, T t, Runnable postChangeAction) {
        if (t != liveData.getValue()) {
            liveData.setValue(t);
            if (postChangeAction != null) {
                postChangeAction.run();
            }
        }
    }

    // A version with no runnable effect
    private <T> void setIfNeeded(MutableLiveData<T> liveData, T t) {
        setIfNeeded(liveData, t, null);
    }

//    // The same thing, but for live maps
//    private <K,V> void setIfNeeded(LiveMap<K,V> liveMap, K k, V v, Runnable postChangeAction) {
//        final LiveData<V> data = liveMap.get(k);
//        if (data != null && data.getValue() != v) {
//            liveMap.set(k, v);
//            if (postChangeAction != null) {
//                postChangeAction.run();
//            }
//        }
//    }
//
//    // With no runnable effect
//    private <K,V> void setIfNeeded(LiveMap<K,V> liveMap, K k, V v) {
//        setIfNeeded(liveMap, k, v, null);
//    }


    // These are to be used as the last argument in setIfNeeded above
    // These set a sort and filter, respectively, when next the table of spells is visible
    private final Runnable setSortFlag = this::setToSort;
    private final Runnable setFilterFlag = this::setToFilter;

    // Set the sorting parameters, level range, and status filter
    // The associated LiveData values are only updated if necessary
    void setFirstSortField(SortField sortField) { setIfNeeded(firstSortField, sortField, setSortFlag); }
    void setSecondSortField(SortField sortField) { setIfNeeded(secondSortField, sortField, setSortFlag); }
    void setFirstSortReverse(Boolean reverse) { setIfNeeded(firstSortReverse, reverse, setSortFlag); }
    void setSecondSortReverse(Boolean reverse) { setIfNeeded(secondSortReverse, reverse, setSortFlag); }
    void setStatusFilter(StatusFilterField sff) { setIfNeeded(statusFilter, sff, setFilterFlag); }
    void setMinLevel(Integer level) { setIfNeeded(minLevel, level, setFilterFlag); }
    void setMaxLevel(Integer level) { setIfNeeded(maxLevel, level, setFilterFlag); }

    // An alternative way to set the sort fields, where one can give the desired level to the function
    // The functions for the sort fields and reverses are partial specializations of this private generic function
    private <T> void setFieldByLevel(T t, int level, Consumer<T> firstSetter, Consumer<T> secondSetter) {
        switch (level) {
            case 1:
                firstSetter.accept(t);
            case 2:
                secondSetter.accept(t);
        }
    }
    void setSortField(SortField sortField, int level) { setFieldByLevel(sortField, level, this::setFirstSortField, this::setSecondSortField); }
    void setSortReverse(Boolean reverse,int level) { setFieldByLevel(reverse, level, this::setFirstSortReverse, this::setSecondSortReverse); }


    void setFilterText(String text) { setIfNeeded(filterText, text, setFilterFlag); }
    void setCurrentSpell(Spell spell) { setIfNeeded(currentSpell, spell); }

    void setToFilter() {
        System.out.println("In setFilterNeeded");
        if (spellTableVisible) {
            System.out.println("Setting filterNeeded to true");
            emitFilterSignal();
        } else {
            filterPending = true;
        }
    }
    void setToSort() {
        if (spellTableVisible) {
            emitSortSignal();
        } else {
            sortPending = true;
        }
    }
    private void liveEmit(MutableLiveData<Void> emitter) { emitter.setValue(null); }
    private void emitSortSignal() { liveEmit(sortEmitter); }
    private void emitFilterSignal() { liveEmit(filterEmitter); }
    private void onTableBecomesVisible() {
        System.out.println("Table became visible");
        if (filterPending) {
            emitFilterSignal();
            filterPending = false;
        } else if (sortPending) {
            emitSortSignal();
            sortPending = false;
        }
    }
    void setSpellTableVisible(Boolean visible) {
        spellTableVisible = visible;
        if (visible) { onTableBecomesVisible(); }
    }

    // This function sets the value of the appropriate LiveData filter, specified by tf, to be b
    private void setYNFilter(MutableLiveData<Boolean> filterT, MutableLiveData<Boolean> filterF, boolean tf, Boolean b) {
        final MutableLiveData<Boolean> filter = tf ? filterT : filterF;
        setIfNeeded(filter, b, setFilterFlag);
    }

    // The specific cases for the ritual, concentration, and component filters
    void setRitualFilter(boolean tf, Boolean b) { setYNFilter(ritualFilter, notRitualFilter, tf, b); }
    void setConcentrationFilter(boolean tf, Boolean b) { setYNFilter(concentrationFilter, notConcentrationFilter, tf, b); }
    void setVerbalFilter(boolean tf, Boolean b) { setYNFilter(verbalFilter, notVerbalFilter, tf, b); }
    void setSomaticFilter(boolean tf, Boolean b) { setYNFilter(somaticFilter, notSomaticFilter, tf, b); }
    void setMaterialFilter(boolean tf, Boolean b) { setYNFilter(materialFilter, notMaterialFilter, tf, b); }

    // Set the visibility flag for the given item to the given value
    void setVisibility(Named named, Boolean visibility) {
        final Class<? extends Named> cls = named.getClass();
        final LiveMap map = classToFlagsMap.get(cls);
        if (map != null && map.get(named) != null) {
            if (map.get(named).getValue() != visibility) {
                map.set(named, visibility);
                setToFilter();
            }
        }
    }

    // Toggle the visibility of the given named item
    void toggleVisibility(Named named) {
        final LiveData<Boolean> data = getVisibility(named);
        if ( (data != null) && (data.getValue() != null) ) {
            setVisibility(named, !data.getValue());
        }
    }

    // Set the range values for a specific class to their defaults
    void setRangeToDefaults(Class<? extends QuantityType> quantityType) {
        final Pair<Unit,Integer> minDefaults = defaultMinQuantityValues.get(quantityType);
        final Pair<Unit,Integer> maxDefaults = defaultMaxQuantityValues.get(quantityType);
        final Pair<MutableLiveData<Unit>,MutableLiveData<Integer>> minValues = minQuantityValues.get(quantityType);
        final Pair<MutableLiveData<Unit>,MutableLiveData<Integer>> maxValues = maxQuantityValues.get(quantityType);
        minValues.getValue0().setValue(minDefaults.getValue0());
        minValues.getValue1().setValue(minDefaults.getValue1());
        maxValues.getValue0().setValue(maxDefaults.getValue0());
        maxValues.getValue1().setValue(maxDefaults.getValue1());
    }

    // These functions, and their specializations below, set the min/max units and values
    // setExtremeUnit and setExtremeValue can probably be combined into one function with a bit of work
    private void setExtremeUnit(Map<Class<? extends QuantityType>, Pair<MutableLiveData<Unit>, MutableLiveData<Integer>>> map, Class<? extends QuantityType> quantityType, Unit unit) {
        final Pair<MutableLiveData<Unit>, MutableLiveData<Integer>> pair = map.get(quantityType);
        if (pair == null) { return; }
        final MutableLiveData<Unit> liveData = pair.getValue0();
        if (liveData == null) { return; }
        setIfNeeded(liveData, unit, setFilterFlag);
    }
    private void setExtremeValue(Map<Class<? extends QuantityType>, Pair<MutableLiveData<Unit>, MutableLiveData<Integer>>> map, Class<? extends QuantityType> quantityType, Integer value) {
        final Pair<MutableLiveData<Unit>, MutableLiveData<Integer>> pair = map.get(quantityType);
        if (pair == null) { return; }
        final MutableLiveData<Integer> liveData = pair.getValue1();
        if (liveData == null) { return; }
        setIfNeeded(liveData, value, setFilterFlag);
    }
    void setMinUnit(Class<? extends QuantityType> quantityType, Unit unit) { setExtremeUnit(minQuantityValues, quantityType, unit); }
    void setMaxUnit(Class<? extends QuantityType> quantityType, Unit unit) { setExtremeUnit(maxQuantityValues, quantityType, unit); }
    void setMinValue(Class<? extends QuantityType> quantityType, Integer value) { setExtremeValue(minQuantityValues, quantityType, value); }
    void setMaxValue(Class<? extends QuantityType> quantityType, Integer value) { setExtremeValue(maxQuantityValues, quantityType, value); }

    // Check whether a given spell is on one of the spell lists
    // It's the same for each list, so the specific list functions just call this general function
    private boolean isProperty(Spell s, Function<SpellStatus,Boolean> property) {
        if (spellStatuses.containsKey(s.getName())) {
            SpellStatus status = spellStatuses.get(s.getName());
            return property.apply(status);
        }
        return false;
    }
    boolean isFavorite(Spell spell) { return isProperty(spell, SpellStatus::isFavorite); }
    boolean isPrepared(Spell spell) { return isProperty(spell, SpellStatus::isPrepared); }
    boolean isKnown(Spell spell) { return isProperty(spell, SpellStatus::isKnown); }

    // Setting whether a spell is on a given spell list
    // General function followed by specific cases
    private void setProperty(Spell s, Boolean val, BiConsumer<SpellStatus,Boolean> propSetter) {
        String spellName = s.getName();
        if (spellStatuses.containsKey(spellName)) {
            SpellStatus status = spellStatuses.get(spellName);
            if (status != null) {
                propSetter.accept(status, val);
                // spellStatuses.put(spellName, status);
                if (status.noneTrue()) { // We can remove the key if all three are false
                    spellStatuses.remove(spellName);
                }
            }
        } else if (val) { // If the key doesn't exist, we only need to modify if val is true
            SpellStatus status = new SpellStatus();
            propSetter.accept(status, true);
            spellStatuses.put(spellName, status);
        }
    }
    void setFavorite(Spell s, Boolean fav) { setProperty(s, fav, SpellStatus::setFavorite); }
    void setPrepared(Spell s, Boolean prep) { setProperty(s, prep, SpellStatus::setPrepared); }
    void setKnown(Spell s, Boolean known) { setProperty(s, known, SpellStatus::setKnown); }

    // Toggling whether a given property is set for a given spell
    // General function followed by specific cases
    private void toggleProperty(Spell s, Function<SpellStatus,Boolean> property, BiConsumer<SpellStatus,Boolean> propSetter) { setProperty(s, !isProperty(s, property), propSetter); }
    void toggleFavorite(Spell s) { toggleProperty(s, SpellStatus::isFavorite, SpellStatus::setFavorite); }
    void togglePrepared(Spell s) { toggleProperty(s, SpellStatus::isPrepared, SpellStatus::setPrepared); }
    void toggleKnown(Spell s) { toggleProperty(s, SpellStatus::isKnown, SpellStatus::setKnown); }

    // Get the names of a collection of named values, as either an array or a list
    <T extends Named> String[] namesArray(Collection<T> collection) { return collection.stream().map(T::getDisplayName).toArray(String[]::new); }
    <T extends Named> List<String> namesList(Collection<T> collection) { return Arrays.asList(namesArray(collection)); }
}
