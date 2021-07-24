package dnd.jon.spellbook;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.javatuples.Quartet;
import org.javatuples.Sextet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import dnd.jon.spellbook.CastingTime.CastingTimeType;
import dnd.jon.spellbook.Duration.DurationType;
import dnd.jon.spellbook.Range.RangeType;

import org.apache.commons.lang3.SerializationUtils;

public class CharacterProfile implements Parcelable {

    // Member values
    private String name;
    private final SpellFilterStatus spellFilterStatus;
    private final SortFilterStatus sortFilterStatus;
    private final SpellSlotStatus spellSlotStatus;

    // Keys for loading/saving
    private static final String charNameKey = "CharacterName";
    private static final String spellsKey = "Spells";
    private static final String spellNameKey = "SpellName";
    private static final String spellIDKey = "SpellID";
    private static final String favoriteKey = "Favorite";
    private static final String preparedKey = "Prepared";
    private static final String knownKey = "Known";
    private static final String sort1Key = "SortField1";
    private static final String sort2Key = "SortField2";
    private static final String classFilterKey = "FilterClass";
    private static final String reverse1Key = "Reverse1";
    private static final String reverse2Key = "Reverse2";
    private static final String booksFilterKey = "BookFilters";
    private static final String statusFilterKey = "StatusFilter";
    private static final String quantityRangesKey = "QuantityRanges";
    private static final String ritualKey = "Ritual";
    private static final String notRitualKey = "NotRitual";
    private static final String concentrationKey = "Concentration";
    private static final String notConcentrationKey = "NotConcentration";
    private static final String minSpellLevelKey = "MinSpellLevel";
    private static final String maxSpellLevelKey = "MaxSpellLevel";
    private static final String versionCodeKey = "VersionCode";
    private static final String componentsFiltersKey = "ComponentsFilters";
    private static final String notComponentsFiltersKey = "NotComponentsFilters";
    private static final String useTCEExpandedListsKey = "UseTCEExpandedLists";
    private static final String applyFiltersToSpellListsKey = "ApplyFiltersToSpellLists";
    private static final String applyFiltersToSearchKey = "ApplyFiltersToSearch";
    private static final String spellFilterStatusKey = "SpellFilterStatus";
    private static final String sortFilterStatusKey = "SortFilterStatus";
    private static final String spellSlotStatusKey = "SpellSlotStatus";

    private static final Version V2_10_0 = new Version(2,10,0);
    private static final Version V2_11_0 = new Version(2,11,0);

    private static final HashMap<Class<? extends Enum<?>>, Quartet<Boolean,Function<Object,Boolean>, String, String>> enumInfo = new HashMap<Class<? extends Enum<?>>, Quartet<Boolean,Function<Object,Boolean>,String,String>>() {{
       put(Sourcebook.class, new Quartet<>(true, (sb) -> sb == Sourcebook.PLAYERS_HANDBOOK, "HiddenSourcebooks",""));
       put(CasterClass.class, new Quartet<>(false, (x) -> true, "HiddenCasters", ""));
       put(School.class, new Quartet<>(false, (x) -> true, "HiddenSchools", ""));
       put(CastingTimeType.class, new Quartet<>(false, (x) -> true, "HiddenCastingTimeTypes", "CastingTimeFilters"));
       put(DurationType.class, new Quartet<>(false, (x) -> true, "HiddenDurationTypes", "DurationFilters"));
       put(RangeType.class, new Quartet<>(false, (x) -> true, "HiddenRangeTypes", "RangeFilters"));
    }};
    private static final HashMap<String, Class<? extends QuantityType>> keyToQuantityTypeMap = new HashMap<>();
    static {
        for (Class<? extends Enum<?>> cls : enumInfo.keySet()) {
            if (QuantityType.class.isAssignableFrom(cls)) {
                final Class<? extends QuantityType> quantityType = (Class<? extends QuantityType>) cls;
                keyToQuantityTypeMap.put(enumInfo.get(cls).getValue3(), quantityType);
            }
        }
    }

    private static final HashMap<Class<? extends QuantityType>, Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer>> defaultQuantityRangeFiltersMap = new HashMap<Class<? extends QuantityType>, Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer>>() {{
       put(CastingTimeType.class, new Sextet<>(CastingTime.class, TimeUnit.class, TimeUnit.SECOND, TimeUnit.HOUR, 0, 24));
       put(DurationType.class, new Sextet<>(Duration.class, TimeUnit.class, TimeUnit.SECOND, TimeUnit.DAY, 0, 30));
       put(RangeType.class, new Sextet<>(Range.class, LengthUnit.class, LengthUnit.FOOT, LengthUnit.MILE, 0, 1));
    }};
    private static final String[] rangeFilterKeys = { "MinUnit", "MaxUnit", "MinText", "MaxText" };


    // There are some warnings about unchecked assignments and calls here, but it's fine the way it's being used
    // This creates the default visibilities map based on our filters
    // It's a bit hacky, and relies on the filters accepting any Object
    private static final HashMap<Class<? extends Enum<?>>, EnumMap<? extends Enum<?>, Boolean>> defaultVisibilitiesMap = new HashMap<>();
    static {
        for (HashMap.Entry<Class<? extends Enum<?>>, Quartet<Boolean, Function<Object,Boolean>, String, String>>  entry: enumInfo.entrySet()) {
            final Class<? extends Enum<?>> enumType = entry.getKey();
            final Function<Object, Boolean> filter = entry.getValue().getValue1();
            final EnumMap enumMap = new EnumMap(enumType);
            if (enumType.getEnumConstants() != null)
            {
                for (int i = 0; i < enumType.getEnumConstants().length; ++i) {
                    enumMap.put(enumType.getEnumConstants()[i], filter.apply(enumType.getEnumConstants()[i]));
                }
            }
            defaultVisibilitiesMap.put(enumType, enumMap);
        }
    }

    private CharacterProfile(String name, SpellFilterStatus spellFilterStatus,
                             SortFilterStatus sortFilterStatus, SpellSlotStatus spellSlotStatus
            ) {
        this.name = name;
        this.spellFilterStatus = spellFilterStatus;
        this.sortFilterStatus = sortFilterStatus;
        this.spellSlotStatus = spellSlotStatus;
    }

    private CharacterProfile(String name, SpellFilterStatus spellFilterStatus) {
        this(name, spellFilterStatus, new SortFilterStatus(), new SpellSlotStatus());
    }

    CharacterProfile(String nameIn) { this(nameIn, new SpellFilterStatus()); }

    protected CharacterProfile(Parcel in) {
        name = in.readString();
        spellFilterStatus = in.readParcelable(SpellFilterStatus.class.getClassLoader());
        sortFilterStatus = in.readParcelable(SortFilterStatus.class.getClassLoader());
        spellSlotStatus = in.readParcelable(SpellSlotStatus.class.getClassLoader());
    }

    public static final Creator<CharacterProfile> CREATOR = new Creator<CharacterProfile>() {
        @Override
        public CharacterProfile createFromParcel(Parcel in) {
            return new CharacterProfile(in);
        }

        @Override
        public CharacterProfile[] newArray(int size) {
            return new CharacterProfile[size];
        }
    };

    CharacterProfile duplicate() {
        return new CharacterProfile(name, spellFilterStatus.duplicate(), sortFilterStatus.duplicate(), spellSlotStatus.duplicate());
    }

    // Basic getters
    String getName() { return name; }
    SpellFilterStatus getSpellFilterStatus() { return spellFilterStatus; }
    SortFilterStatus getSortFilterStatus() { return sortFilterStatus; }
    SpellSlotStatus getSpellSlotStatus() { return spellSlotStatus; }

    // Basic setters
    void setName(String name) { this.name = name; }

    // Constructing a map from a list of hidden values
    // Used for JSON decoding
    @SuppressWarnings("unchecked")
    private static EnumMap<?,Boolean> mapFromHiddenNames(EnumMap<? extends Enum<?>,Boolean> defaultMap, boolean nonTrivialFilter, Function<Object,Boolean> filter, JSONObject json, String key, Method constructorFromName) throws JSONException, IllegalAccessException, InvocationTargetException {
        final EnumMap map = SerializationUtils.clone(defaultMap);
        if (nonTrivialFilter) {
            for (Enum<?> e : defaultMap.keySet()) {
                map.put(e, true);
            }
        }
        if (json.has(key)) {
            final JSONArray jsonArray = json.getJSONArray(key);
            for (int i = 0; i < jsonArray.length(); ++i) {
                final String name = jsonArray.getString(i);
                final Enum<?> value = (Enum<?>) constructorFromName.invoke(null, name);
                map.put(value, false);
            }
        }
        return map;
    }

    // Save to a file
    boolean save(File filename) {
        try {
            final JSONObject cpJSON = toJSON();
            //System.out.println("Saving JSON: " + cpJSON.toString());
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
                bw.write(cpJSON.toString());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Create a JSON object representing the profile
    // This is what we for saving
    // We can reconstruct the profile using fromJSON
    JSONObject toJSON() throws JSONException {

        // The JSON object
        final JSONObject json = new JSONObject();

        // Store the data
        json.put(charNameKey, name);
        json.put(spellFilterStatusKey, spellFilterStatus.toJSON());
        json.put(sortFilterStatusKey, sortFilterStatus.toJSON());
        json.put(statusFilterKey, sortFilterStatus.getStatusFilterField().getInternalName());
        json.put(versionCodeKey, GlobalInfo.VERSION_CODE);

        return json;
    }

    // Construct a profile from a JSON object
    // Basically the inverse to toJSON
    static CharacterProfile fromJSON(JSONObject json) throws JSONException {
        //System.out.println(json.toString(4));
        if (json.has(versionCodeKey)) {
            final String versionCode = json.getString(versionCodeKey);
            final Version version = SpellbookUtils.coalesce(Version.fromString(versionCode), GlobalInfo.VERSION);
            if (version.compareTo(V2_10_0) >= 0) {
                return fromJSONNew(json, version);
            } else {
                return fromJSONPre2_10(json);
            }
        } else {
            return fromJSONOld(json);
        }
    }

    private static CharacterProfile fromJSONv2_12(JSONObject json) throws JSONException {
        final String charName = json.getString(charNameKey);

        final JSONObject sortFilterJSON = json.getJSONObject(sortFilterStatusKey);
        final SortFilterStatus sortFilterStatus = SortFilterStatus.fromJSON(sortFilterJSON);

        final JSONObject spellFilterJSON = json.getJSONObject(spellFilterStatusKey);
        final SpellFilterStatus spellFilterStatus = SpellFilterStatus.fromJSON(spellFilterJSON);

        final JSONObject spellSlotJSON = json.getJSONObject(spellSlotStatusKey);
        final SpellSlotStatus spellSlotStatus = SpellSlotStatus.fromJSON(spellSlotJSON);

        return new CharacterProfile(charName, spellFilterStatus, sortFilterStatus, spellSlotStatus);

    }

    // For backwards compatibility
    // so that when people update, their old profiles are still usable
    private static CharacterProfile fromJSONOld(JSONObject json) throws JSONException {

        final String charName = json.getString(charNameKey);

        // Get the spell map, assuming it exists
        // If it doesn't, we just get an empty map
        final Map<String, SpellStatus> spellStatusNameMap = new HashMap<>();
        if (json.has(spellsKey)) {
            final JSONArray jsonArray = json.getJSONArray(spellsKey);
            for (int i = 0; i < jsonArray.length(); ++i) {
                final JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Get the name and array of statuses
                final String spellName = jsonObject.getString(spellNameKey);

                // Load the spell statuses
                final boolean fav = jsonObject.getBoolean(favoriteKey);
                final boolean prep = jsonObject.getBoolean(preparedKey);
                final boolean known = jsonObject.getBoolean(knownKey);
                final SpellStatus status = new SpellStatus(fav, prep, known);

                // Add to the map
                spellStatusNameMap.put(spellName, status);
            }
        }
        final Map<Integer,SpellStatus> spellStatusMap = convertStatusMap(spellStatusNameMap);

        // Get the first sort field, if present
        final SortField sortField1 = json.has(sort1Key) ? SortField.fromInternalName(json.getString(sort1Key)) : SortField.NAME;

        // Get the second sort field, if present
        final SortField sortField2 = json.has(sort2Key) ? SortField.fromInternalName(json.getString(sort2Key)) : SortField.NAME;

        // Get the sort reverse variables
        final boolean reverse1 = json.optBoolean(reverse1Key, false);
        final boolean reverse2 = json.optBoolean(reverse2Key, false);

        // Set up the visibility map
        final HashMap<Class<? extends Enum<?>>, EnumMap<? extends Enum<?>, Boolean>> visibilitiesMap = SerializationUtils.clone(defaultVisibilitiesMap);

        // If there was a filter class before, that's now the only visible class
        final CasterClass filterClass = json.has(classFilterKey) ? CasterClass.fromInternalName(json.getString(classFilterKey)) : null;
        if (filterClass != null) {
            final EnumMap<? extends Enum<?>, Boolean> casterMap = SerializationUtils.clone(defaultVisibilitiesMap.get(CasterClass.class));
            for (EnumMap.Entry<? extends Enum<?>, Boolean> entry : casterMap.entrySet()) {
                if (entry.getKey() != filterClass) {
                    entry.setValue(false);
                }
            }
            visibilitiesMap.put(CasterClass.class, casterMap);
        }

        // What was the sourcebooks filter map is now the sourcebook entry of the visibilities map
        if (json.has(booksFilterKey)) {
            final EnumMap<Sourcebook, Boolean> sourcebookMap = new EnumMap<>(Sourcebook.class);
            final JSONObject booksJSON = json.getJSONObject(booksFilterKey);
            for (Sourcebook sb : Sourcebook.values()) {
                if (booksJSON.has(sb.getInternalName())) {
                    sourcebookMap.put(sb, booksJSON.getBoolean(sb.getInternalName()));
                } else {
                    final boolean b = (sb == Sourcebook.PLAYERS_HANDBOOK); // True if PHB, false otherwise
                    sourcebookMap.put(sb, b);
                }
            }

            final List<Sourcebook> oldSourcebooks = Arrays.asList(Sourcebook.PLAYERS_HANDBOOK, Sourcebook.XANATHARS_GTE, Sourcebook.SWORD_COAST_AG);
            for (Sourcebook sb : Sourcebook.values()) {
                if (!oldSourcebooks.contains(sb)) {
                    sourcebookMap.put(sb, false);
                }
            }
            visibilitiesMap.put(Sourcebook.class, sourcebookMap);
        }

        // Get the status filter
        final StatusFilterField statusFilter = json.has(statusFilterKey) ? StatusFilterField.fromDisplayName(json.getString(statusFilterKey)) : StatusFilterField.ALL;

        // We no longer need the default filter statuses, and the spinners no longer have the default text

        // Everything else that the profiles have is new, so we'll use the defaults
        final Map<Class<? extends QuantityType>, Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer>> quantityRangesMap = SerializationUtils.clone(defaultQuantityRangeFiltersMap);
        final int minLevel = Spellbook.MIN_SPELL_LEVEL;
        final int maxLevel = Spellbook.MAX_SPELL_LEVEL;

        // Return the profile
        //return new CharacterProfile(charName, spellStatusMap, sortField1, sortField2, visibilitiesMap, quantityRangesMap, reverse1, reverse2, statusFilter, true, true, true, true, new boolean[]{true,true,true}, new boolean[]{true,true,true}, minLevel, maxLevel, false, false, false);
        return new CharacterProfile(charName); // Dummy result for testing
    }

    // For character profiles from this version of the app
    private static CharacterProfile fromJSONNew(JSONObject json, Version version) throws JSONException {

        final String charName = json.getString(charNameKey);

        // Get the first sort field, if present
        final SortField sortField1 = json.has(sort1Key) ? SortField.fromInternalName(json.getString(sort1Key)) : SortField.NAME;

        // Get the second sort field, if present
        final SortField sortField2 = json.has(sort2Key) ? SortField.fromInternalName(json.getString(sort2Key)) : SortField.NAME;

        final Map<Integer, SpellStatus> spellStatusMap = new HashMap<>();
        if (json.has(spellsKey)) {
            final JSONArray jsonArray = json.getJSONArray(spellsKey);
            for (int i = 0; i < jsonArray.length(); ++i) {
                final JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Get the name and array of statuses
                final Integer spellID = jsonObject.getInt(spellIDKey);

                // Load the spell statuses
                final boolean fav = jsonObject.getBoolean(favoriteKey);
                final boolean prep = jsonObject.getBoolean(preparedKey);
                final boolean known = jsonObject.getBoolean(knownKey);
                final SpellStatus status = new SpellStatus(fav, prep, known);

                // Add to the map
                spellStatusMap.put(spellID, status);
            }
        }

        // Create the visibility maps
        final Map<Class<? extends Enum<?>>, EnumMap<? extends Enum<?>, Boolean>> visibilitiesMap = SerializationUtils.clone(defaultVisibilitiesMap);
        for (Map.Entry<Class<? extends Enum<?>>, Quartet<Boolean, Function<Object,Boolean>, String, String>> entry : enumInfo.entrySet()) {
            final Class<? extends Enum<?>> cls = entry.getKey();
            Quartet<Boolean, Function<Object,Boolean>, String, String> entryValue = entry.getValue();
            final String key = entryValue.getValue2();
            final Function<Object,Boolean> filter = entryValue.getValue1();
            final boolean nonTrivialFilter = entryValue.getValue0();
            final EnumMap<? extends Enum<?>, Boolean> defaultMap = defaultVisibilitiesMap.get(cls);
            try {
                final String constructorName = "fromInternalName";
                final Method constructorFromName = cls.getDeclaredMethod(constructorName, String.class);
                final EnumMap<? extends Enum<?>, Boolean> map = mapFromHiddenNames(defaultMap, nonTrivialFilter, filter, json, key, constructorFromName);
                visibilitiesMap.put(cls, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // New sourcebooks from 2.10 -> 2.11
        if (version.equals(V2_10_0)) {
            final EnumMap<Sourcebook,Boolean> sourcebookMap = (EnumMap<Sourcebook, Boolean>) visibilitiesMap.get(Sourcebook.class);
            if (sourcebookMap != null) {
                final List<Sourcebook> newSourcebooks = Arrays.asList(Sourcebook.ACQUISITIONS_INC, Sourcebook.LOST_LAB_KWALISH, Sourcebook.EXPLORERS_GTW, Sourcebook.RIME_FROSTMAIDEN);
                for (Sourcebook sb : newSourcebooks) {
                    sourcebookMap.put(sb, false);
                }
            }
        }

        // Create the range filter map
        final HashMap<Class<? extends QuantityType>, Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer>> quantityRangesMap = SerializationUtils.clone(defaultQuantityRangeFiltersMap);
        if (json.has(quantityRangesKey)) {
            try {
                final JSONObject quantityRangesJSON = json.getJSONObject(quantityRangesKey);
                final Iterator<String> it = quantityRangesJSON.keys();
                while (it.hasNext()) {
                    final String key = it.next();
                    final Class<? extends QuantityType> quantityType = keyToQuantityTypeMap.get(key);
                    final Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer> defaultData = quantityRangesMap.get(quantityType);
                    final Class<? extends Quantity> quantityClass = defaultData.getValue0();
                    final Class<? extends Unit> unitClass = defaultData.getValue1();
                    final JSONObject rangeJSON = quantityRangesJSON.getJSONObject(key);
                    final Method method = unitClass.getDeclaredMethod("fromInternalName", String.class);
                    final Unit unit1 = SpellbookUtils.coalesce((Unit) method.invoke(null, rangeJSON.getString(rangeFilterKeys[0])), defaultData.getValue2());
                    final Unit unit2 = SpellbookUtils.coalesce((Unit) method.invoke(null, rangeJSON.getString(rangeFilterKeys[1])), defaultData.getValue3());
                    final Integer val1 = SpellbookUtils.coalesce(SpellbookUtils.intParse(rangeJSON.getString(rangeFilterKeys[2])), defaultData.getValue4());
                    final Integer val2 = SpellbookUtils.coalesce(SpellbookUtils.intParse(rangeJSON.getString(rangeFilterKeys[3])), defaultData.getValue5());
                    final Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer> sextet =
                            new Sextet<>(quantityClass, unitClass, unit1, unit2, val1, val2);
                    //System.out.println("min unit is " + ((Unit) method.invoke(null, rangeJSON.getString(rangeFilterKeys[0]))).getInternalName());
                    quantityRangesMap.put(quantityType, sextet);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Get the sort reverse variables
        final boolean reverse1 = json.optBoolean(reverse1Key, false);
        final boolean reverse2 = json.optBoolean(reverse2Key, false);

        // Get the filter statuses for ritual and concentration
        final boolean ritualFilter = json.optBoolean(ritualKey, true);
        final boolean notRitualFilter = json.optBoolean(notRitualKey, true);
        final boolean concentrationFilter = json.optBoolean(concentrationKey, true);
        final boolean notConcentrationFilter = json.optBoolean(notConcentrationKey, true);
        final JSONArray componentsJSON = json.optJSONArray(componentsFiltersKey);
        final boolean[] componentsFilters = new boolean[]{true, true, true};
        if (componentsJSON != null && componentsJSON.length() == 3) {
            for (int i = 0; i < componentsJSON.length(); ++i) {
                componentsFilters[i] = componentsJSON.getBoolean(i);
            }
        }
        final JSONArray notComponentsJSON = json.optJSONArray(notComponentsFiltersKey);
        final boolean[] notComponentsFilters = new boolean[]{true, true, true};
        if (notComponentsJSON != null && notComponentsJSON.length() == 3) {
            for (int i = 0; i < notComponentsJSON.length(); ++i) {
                notComponentsFilters[i] = notComponentsJSON.getBoolean(i);
            }
        }

        // Get the min and max spell levels
        final int minLevel = json.optInt(minSpellLevelKey, Spellbook.MIN_SPELL_LEVEL);
        final int maxLevel = json.optInt(maxSpellLevelKey, Spellbook.MAX_SPELL_LEVEL);

        // Get the status filter
        final StatusFilterField statusFilter = json.has(statusFilterKey) ? StatusFilterField.fromDisplayName(json.getString(statusFilterKey)) : StatusFilterField.ALL;

        // Get the other toggle settings, if present
        final boolean useExpLists = json.optBoolean(useTCEExpandedListsKey, false);
        final boolean applyFilters = json.optBoolean(applyFiltersToSpellListsKey, false);
        final boolean applyToSearch = json.optBoolean(applyFiltersToSearchKey, false);

        // Return the profile
        //return new CharacterProfile(charName, spellStatusMap, sortField1, sortField2, visibilitiesMap, quantityRangesMap, reverse1, reverse2, statusFilter, ritualFilter, notRitualFilter, concentrationFilter, notConcentrationFilter, componentsFilters, notComponentsFilters, minLevel, maxLevel, useExpLists, applyFilters, applyToSearch);
        return new CharacterProfile(charName); // Dummy result for testing

    }

    // For character profiles from versions of the app before 2.10 that have a version code
    private static CharacterProfile fromJSONPre2_10(JSONObject json) throws JSONException {

        //System.out.println("The JSON is " + json.toString());

        final String charName = json.getString(charNameKey);

        // Get the spell map, assuming it exists
        // If it doesn't, we just get an empty map
        final Map<String, SpellStatus> spellStatusNameMap = new HashMap<>();
        if (json.has(spellsKey)) {
            final JSONArray jarr = json.getJSONArray(spellsKey);
            for (int i = 0; i < jarr.length(); ++i) {
                final JSONObject jobj = jarr.getJSONObject(i);

                // Get the name and array of statuses
                final String spellName = jobj.getString(spellNameKey);

                // Load the spell statuses
                final boolean fav = jobj.getBoolean(favoriteKey);
                final boolean prep = jobj.getBoolean(preparedKey);
                final boolean known = jobj.getBoolean(knownKey);
                final SpellStatus status = new SpellStatus(fav, prep, known);

                // Add to the map
                spellStatusNameMap.put(spellName, status);
            }
        }
        final Map<Integer,SpellStatus> spellStatusMap = convertStatusMap(spellStatusNameMap);

        // Get the first sort field, if present
        final SortField sortField1 = json.has(sort1Key) ? SortField.fromInternalName(json.getString(sort1Key)) : SortField.NAME;

        // Get the second sort field, if present
        final SortField sortField2 = json.has(sort2Key) ? SortField.fromInternalName(json.getString(sort2Key)) : SortField.NAME;

        // Create the visibility maps
        final Map<Class<? extends Enum<?>>, EnumMap<? extends Enum<?>, Boolean>> visibilitiesMap = SerializationUtils.clone(defaultVisibilitiesMap);
        for (Map.Entry<Class<? extends Enum<?>>, Quartet<Boolean, Function<Object,Boolean>, String, String>> entry : enumInfo.entrySet()) {
            final Class<? extends Enum<?>> cls = entry.getKey();
            Quartet<Boolean, Function<Object,Boolean>, String, String> entryValue = entry.getValue();
            final String key = entryValue.getValue2();
            final Function<Object,Boolean> filter = entryValue.getValue1();
            final boolean nonTrivialFilter = entryValue.getValue0();
            final EnumMap<? extends Enum<?>, Boolean> defaultMap = defaultVisibilitiesMap.get(cls);
            try {
                final Method constructorFromName = cls.getDeclaredMethod("fromInternalName", String.class);
                final EnumMap<? extends Enum<?>, Boolean> map = mapFromHiddenNames(defaultMap, nonTrivialFilter, filter, json, key, constructorFromName);
                visibilitiesMap.put(cls, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If at least one class is hidden, hide the Artificer
        final EnumMap<CasterClass,Boolean> ccMap = (EnumMap<CasterClass,Boolean>) visibilitiesMap.get(CasterClass.class);
        if (ccMap != null) {
            boolean hasOneHidden = false;
            for (boolean vis : ccMap.values()) {
                if (!vis) {
                    hasOneHidden = true;
                    break;
                }
            }
            if (hasOneHidden) {
                ccMap.put(CasterClass.ARTIFICER, false);
            }
        }
        
        // Set newer sourcebooks to be not visible
        final EnumMap<Sourcebook,Boolean> sbMap = (EnumMap<Sourcebook,Boolean>) visibilitiesMap.get(Sourcebook.class);
        if (sbMap != null) {
            final List<Sourcebook> oldSourcebooks = Arrays.asList(Sourcebook.PLAYERS_HANDBOOK, Sourcebook.XANATHARS_GTE, Sourcebook.SWORD_COAST_AG);
            for (Sourcebook sb : Sourcebook.values()) {
                if (!oldSourcebooks.contains(sb)) {
                    sbMap.put(sb, false);
                }
            }
        }

        // Create the range filter map
        final Map<Class<? extends QuantityType>, Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer>> quantityRangesMap = SerializationUtils.clone(defaultQuantityRangeFiltersMap);
        if (json.has(quantityRangesKey)) {
            try {
                final JSONObject quantityRangesJSON = json.getJSONObject(quantityRangesKey);
                final Iterator<String> it = quantityRangesJSON.keys();
                while (it.hasNext()) {
                    final String key = it.next();
                    final Class<? extends QuantityType> quantityType = keyToQuantityTypeMap.get(key);
                    final Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer> defaultData = quantityRangesMap.get(quantityType);
                    final Class<? extends Quantity> quantityClass = defaultData.getValue0();
                    final Class<? extends Unit> unitClass = defaultData.getValue1();
                    final JSONObject rangeJSON = quantityRangesJSON.getJSONObject(key);
                    final Method method = unitClass.getDeclaredMethod("fromInternalName", String.class);
                    final Unit unit1 = SpellbookUtils.coalesce((Unit) method.invoke(null, rangeJSON.getString(rangeFilterKeys[0])), defaultData.getValue2());
                    final Unit unit2 = SpellbookUtils.coalesce((Unit) method.invoke(null, rangeJSON.getString(rangeFilterKeys[1])), defaultData.getValue3());
                    final Integer val1 = SpellbookUtils.coalesce(SpellbookUtils.intParse(rangeJSON.getString(rangeFilterKeys[2])), defaultData.getValue4());
                    final Integer val2 = SpellbookUtils.coalesce(SpellbookUtils.intParse(rangeJSON.getString(rangeFilterKeys[3])), defaultData.getValue5());
                    final Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer> sextet =
                            new Sextet<>(quantityClass, unitClass, unit1, unit2, val1, val2);
                    //System.out.println("min unit is " + ((Unit) method.invoke(null, rangeJSON.getString(rangeFilterKeys[0]))).getInternalName());
                    quantityRangesMap.put(quantityType, sextet);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Get the sort reverse variables
        final boolean reverse1 = json.optBoolean(reverse1Key, false);
        final boolean reverse2 = json.optBoolean(reverse2Key, false);

        // Get the filter statuses for ritual and concentration
        final boolean ritualFilter = json.optBoolean(ritualKey, true);
        final boolean notRitualFilter = json.optBoolean(notRitualKey, true);
        final boolean concentrationFilter = json.optBoolean(concentrationKey, true);
        final boolean notConcentrationFilter = json.optBoolean(notConcentrationKey, true);
        final JSONArray componentsJSON = json.optJSONArray(componentsFiltersKey);
        final boolean[] componentsFilters = new boolean[]{true, true, true};
        if (componentsJSON != null && componentsJSON.length() == 3) {
            for (int i = 0; i < componentsJSON.length(); ++i) {
                componentsFilters[i] = componentsJSON.getBoolean(i);
            }
        }
        final JSONArray notComponentsJSON = json.optJSONArray(notComponentsFiltersKey);
        final boolean[] notComponentsFilters = new boolean[]{true, true, true};
        if (notComponentsJSON != null && notComponentsJSON.length() == 3) {
            for (int i = 0; i < notComponentsJSON.length(); ++i) {
                notComponentsFilters[i] = notComponentsJSON.getBoolean(i);
            }
        }

        // Get the min and max spell levels
        final int minLevel = json.optInt(minSpellLevelKey, Spellbook.MIN_SPELL_LEVEL);
        final int maxLevel = json.optInt(maxSpellLevelKey, Spellbook.MAX_SPELL_LEVEL);

        // Get the status filter
        final StatusFilterField statusFilter = json.has(statusFilterKey) ? StatusFilterField.fromDisplayName(json.getString(statusFilterKey)) : StatusFilterField.ALL;

        // Return the profile
        //return new CharacterProfile(charName, spellStatusMap, sortField1, sortField2, visibilitiesMap, quantityRangesMap, reverse1, reverse2, statusFilter, ritualFilter, notRitualFilter, concentrationFilter, notConcentrationFilter, componentsFilters, notComponentsFilters, minLevel, maxLevel, false, false, false);
        return new CharacterProfile(charName); // Dummy result for testing
    }

    private static Map<Integer,SpellStatus> convertStatusMap(Map<String,SpellStatus> oldMap) {
        final Set<String> scagCantrips = new HashSet<String>() {{
            add("Booming Blade");
            add("Green-Flame Blade");
            add("Lightning Lure");
            add("Sword Burst");
        }};
        final List<Spell> englishSpells = MainActivity.englishSpells;
        final Map<String,Integer> idMap = new HashMap<>();
        for (Spell spell : englishSpells) {
            idMap.put(spell.getName(), spell.getID());
        }
        final Map<Integer,SpellStatus> newMap = new HashMap<>();
        for (Map.Entry<String,SpellStatus> entry : oldMap.entrySet()) {
            String name = entry.getKey();
            final SpellStatus status = entry.getValue();
            if (scagCantrips.contains(name)) {
                name = name + " (SCAG)";
            }
            final Integer id = idMap.get(name);
            if (id != null) {
                newMap.put(id, status);
            }
        }
        return newMap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeParcelable(spellFilterStatus, i);
        parcel.writeParcelable(sortFilterStatus, i);
        parcel.writeParcelable(spellSlotStatus, i);
    }
}
