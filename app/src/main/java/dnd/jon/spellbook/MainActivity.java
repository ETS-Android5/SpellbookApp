package dnd.jon.spellbook;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;

import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.EditText;
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.javatuples.Triplet;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Sextet;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import androidx.databinding.DataBindingUtil;
import androidx.viewbinding.ViewBinding;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar;

import dnd.jon.spellbook.databinding.ActivityMainBinding;
import dnd.jon.spellbook.databinding.ComponentsFilterLayoutBinding;
import dnd.jon.spellbook.databinding.FilterBlockFeaturedLayoutBinding;
import dnd.jon.spellbook.databinding.FilterOptionBinding;
import dnd.jon.spellbook.databinding.FilterOptionsLayoutBinding;
import dnd.jon.spellbook.databinding.SortFilterLayoutBinding;
import dnd.jon.spellbook.databinding.FilterBlockLayoutBinding;
import dnd.jon.spellbook.databinding.FilterBlockRangeLayoutBinding;
import dnd.jon.spellbook.databinding.ItemFilterViewBinding;
import dnd.jon.spellbook.databinding.LevelFilterLayoutBinding;
import dnd.jon.spellbook.databinding.RangeFilterLayoutBinding;
import dnd.jon.spellbook.databinding.RitualConcentrationLayoutBinding;
import dnd.jon.spellbook.databinding.SortLayoutBinding;
import dnd.jon.spellbook.databinding.SpellWindowBinding;
import dnd.jon.spellbook.databinding.YesNoFilterViewBinding;

public class MainActivity extends AppCompatActivity {

    // The spells file and storage
    private String spellsFilename;
    private static final String englishSpellsFilename = "Spells.json";
    private static List<Spell> baseSpells = new ArrayList<>();
    static List<Spell> englishSpells = new ArrayList<>();

    // The settings file
    private static final String settingsFile = "Settings.json";

    // UI elements
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    //private NavigationView rightNavView;
    private ExpandableListView rightExpLV;
    private ExpandableListAdapter rightAdapter;
    private SearchView searchView;
    private MenuItem searchViewIcon;
    private MenuItem filterMenuIcon;
    private ConstraintLayout spellsCL;
    private ScrollView filterSV;

    private static final String profilesDirName = "Characters";
    //private static final String createdSpellDirName = "CreatedSpells";
    private File profilesDir;
    //private File createdSpellsDir;
    //private Map<File,String> directories = new HashMap<>();

    // The character profile and settings
    private CharacterProfile characterProfile;
    private Settings settings;

    // Dialogs
    private View characterSelect = null;
    private CharacterSelectionDialog selectionDialog = null;

    // For filtering stuff
    private boolean filterVisible = false;
    private final HashMap<Class<? extends NameDisplayable>, List<ItemFilterViewBinding>> classToBindingsMap = new HashMap<>();
    private final List<YesNoFilterViewBinding> yesNoBindings = new ArrayList<>();
    private final HashMap<Class<? extends QuantityType>, RangeFilterLayoutBinding> classToRangeMap = new HashMap<>();
    private final Map<Class<? extends NameDisplayable>, Map<NameDisplayable,ToggleButton>> filterButtonMaps = new HashMap<>();

    // For passing the spell and its index to the SpellWindow
    private static final String spellBundleKey = "SPELL";
    private static final String spellIndexBundleKey = "SPELL_INDEX";

    // For feedback messages
    private static final String devEmail = "dndspellbookapp@gmail.com";
    private static final String emailMessage = "[Android] Feedback";

    // Logging tag
    private static final String TAG = "MainActivity";

    // IDs of text for the update dialog
    private static final int updateDialogTitleID = R.string.update_02_13_title;
    private static final int updateDialogDescriptionID = R.string.update_02_13_description;

    // The map ID -> StatusFilterField relating left nav bar items to the corresponding spell status filter
    private static final HashMap<Integer,StatusFilterField> statusFilterIDs = new HashMap<Integer,StatusFilterField>() {{
       put(R.id.nav_all, StatusFilterField.ALL);
       put(R.id.nav_favorites, StatusFilterField.FAVORITES);
       put(R.id.nav_prepared, StatusFilterField.PREPARED);
       put(R.id.nav_known, StatusFilterField.KNOWN);
    }};

    private static final HashMap<Class<? extends NameDisplayable>,  Quartet<Boolean, Function<SortFilterLayoutBinding, ViewBinding>, Integer, Integer>> filterBlockInfo = new HashMap<Class<? extends NameDisplayable>, Quartet<Boolean, Function<SortFilterLayoutBinding, ViewBinding>, Integer, Integer>>() {{
        put(Sourcebook.class, new Quartet<>(false, (b) -> b.sourcebookFilterBlock, R.string.sourcebook_filter_title, R.integer.sourcebook_filter_columns));
        put(CasterClass.class, new Quartet<>(false, (b) -> b.casterFilterBlock, R.string.caster_filter_title, R.integer.caster_filter_columns));
        put(School.class, new Quartet<>(false, (b) -> b.schoolFilterBlock, R.string.school_filter_title, R.integer.school_filter_columns));
        put(CastingTime.CastingTimeType.class, new Quartet<>(true, (b) -> b.castingTimeFilterRange, R.string.casting_time_type_filter_title, R.integer.casting_time_type_filter_columns));
        put(Duration.DurationType.class, new Quartet<>(true, (b) -> b.durationFilterRange, R.string.duration_type_filter_title, R.integer.duration_type_filter_columns));
        put(Range.RangeType.class, new Quartet<>(true, (b) -> b.rangeFilterRange, R.string.range_type_filter_title, R.integer.range_type_filter_columns));
    }};

    // The Triples consist of
    // Superclass, min text, max text, max entry length
    private static final HashMap<Class<? extends QuantityType>, Triplet<Class<? extends Unit>, Integer, Integer>> rangeViewInfo = new HashMap<Class<? extends QuantityType>, Triplet<Class<? extends Unit>, Integer, Integer>>()  {{
        put(CastingTime.CastingTimeType.class, new Triplet<>(TimeUnit.class, R.string.casting_time_range_text, R.integer.casting_time_max_length));
        put(Duration.DurationType.class, new Triplet<>(TimeUnit.class, R.string.duration_range_text, R.integer.duration_max_length));
        put(Range.RangeType.class, new Triplet<>(LengthUnit.class, R.string.range_range_text, R.integer.range_max_length));
    }};

    // Header/expanding views
    private final HashMap<View,View> expandingViews = new HashMap<>();

    // The UI elements for sorting and searching
    private Spinner sort1;
    private Spinner sort2;
    private SortDirectionButton sortArrow1;
    private SortDirectionButton sortArrow2;

    // The RecyclerView and adapter for the table of spells
    private RecyclerView spellRecycler;
    private SpellRowAdapter spellAdapter;

    // For listening to keyboard visibility events
    //private Unregistrar unregistrar;

    // The file extension for character files
    private static final String CHARACTER_EXTENSION = ".json";

    // Keys for Bundles
    private static final String FAVORITE_KEY = "FAVORITE";
    private static final String KNOWN_KEY = "KNOWN";
    private static final String PREPARED_KEY = "PREPARED";
    private static final String FILTER_VISIBLE_KEY = "FILTER_VISIBLE";

    // Whether or not this is running on a tablet
    private boolean onTablet;

    // Perform sorting and filtering only if we're on a tablet layout
    // This is useful for the sort/filter window stuff
    // On a phone layout, we can defer these operations until the sort/filter window is closed, as the spells aren't visible until then
    // But on a tablet layout they're always visible, so we need to account for that
    private final Runnable sortOnTablet = () -> { if (onTablet) { sort(); } };
    private final Runnable filterOnTablet = () -> { if (onTablet) { filter(); } };

    // For view and data binding
    private ActivityMainBinding amBinding = null;
    private SpellWindowBinding spellWindowBinding = null;
    private ConstraintLayout spellWindowCL = null;
    private SortFilterLayoutBinding sortFilterBinding = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the main activity binding
        amBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(amBinding.getRoot());

        // Get the sort/filter binding
        sortFilterBinding = amBinding.sortFilterWindow;

        // Are we on a tablet or not?
        // If we're on a tablet, do the necessary setup
        onTablet = getResources().getBoolean(R.bool.isTablet);
        if (onTablet) { tabletSetup(); }

        // Where are the spells located?
        spellsFilename = getResources().getString(R.string.spells_filename);

        // Get the main views
        spellsCL = amBinding.mainConstraintLayout;
        filterSV = amBinding.sortFilterScroll;

        // For keyboard visibility listening
        KeyboardVisibilityEvent.setEventListener(this, (isOpen) -> {
            if (!isOpen) {
                clearViewTypeFocus(EditText.class);
            }
        });

        // Re-set the current spell after a rotation (only needed on tablet)
        if (onTablet && savedInstanceState != null) {
            final Spell spell = savedInstanceState.containsKey(spellBundleKey) ? savedInstanceState.getParcelable(spellBundleKey) : null;
            final int spellIndex = savedInstanceState.containsKey(spellIndexBundleKey) ? savedInstanceState.getInt(spellIndexBundleKey) : -1;
            if (spell != null) {
                spellWindowBinding.setSpell(spell);
                spellWindowBinding.setSpellIndex(spellIndex);
                spellWindowBinding.executePendingBindings();
                spellWindowCL.setVisibility(View.VISIBLE);
                if (savedInstanceState.containsKey(FAVORITE_KEY) && savedInstanceState.containsKey(PREPARED_KEY) && savedInstanceState.containsKey(KNOWN_KEY)) {
                    updateSpellWindow(spell, savedInstanceState.getBoolean(FAVORITE_KEY), savedInstanceState.getBoolean(PREPARED_KEY), savedInstanceState.getBoolean(KNOWN_KEY));
                }
            }
        }

        // Whether or not we want the filter to be visible
        if (savedInstanceState != null) {
            filterVisible = savedInstanceState.containsKey(FILTER_VISIBLE_KEY) && savedInstanceState.getBoolean(FILTER_VISIBLE_KEY);
        }

        // Set the toolbar as the app bar for the activity
        final Toolbar toolbar = amBinding.toolbar;
        setSupportActionBar(toolbar);

        // The DrawerLayout and the left navigation view
        drawerLayout = amBinding.drawerLayout;
        navView = amBinding.sideMenu;
        final NavigationView.OnNavigationItemSelectedListener navViewListener = new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                final int index = menuItem.getItemId();
                boolean close = false;
                if (index == R.id.subnav_charselect) {
                    openCharacterSelection();
                } else if (index == R.id.nav_feedback) {
                    sendFeedback();
                } else if (index == R.id.nav_rate_us) {
                    openPlayStoreForRating();
                } else if (index == R.id.nav_whats_new) {
                    showUpdateDialog(false);
                //} else if (index == R.id.create_a_spell) {
                //    openSpellCreationWindow();
                } else if (statusFilterIDs.containsKey(index)) {
                    final StatusFilterField sff = statusFilterIDs.get(index);
                    characterProfile.setStatusFilter(sff);
                    saveCharacterProfile();
                    close = true;
                }
                filter();
                saveSettings();

                // This piece of code makes the drawer close when an item is selected
                // At the moment, we only want that for when choosing one of favorites, known, prepared
                if (close && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            }
        };
        navView.setNavigationItemSelectedListener(navViewListener);

        // This listener will stop the spell recycler's scrolling when the navigation drawer is opened
        // This prevents a crash that could occur if a filter button is selected while the spell recycler is still scrolling
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }
            @Override public void onDrawerClosed(@NonNull View drawerView) { }
            @Override public void onDrawerStateChanged(int newState) { }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                if (spellRecycler != null) {
                    spellRecycler.stopScroll();
                }
            }

        });

        // Set the hamburger button to open the left nav
        final ActionBarDrawerToggle leftNavToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.left_navigation_drawer_open, R.string.left_navigation_drawer_closed);
        drawerLayout.addDrawerListener(leftNavToggle);
        leftNavToggle.syncState();
        leftNavToggle.setDrawerSlideAnimationEnabled(true); // Whether or not the hamburger button changes to the arrow when the drawer is open
        leftNavToggle.setDrawerIndicatorEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener((v) -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Set up the right navigation view
        setupRightNav();

        // Set up the sort/filter view
        setupSortFilterView();

        //View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        /*int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);*/

        // Create any necessary directories
        // If they already exist, this function does nothing
        profilesDir = createFileDirectory(profilesDirName);
        //createdSpellsDir = createFileDirectory(createdSpellDirName);

        // Load the spell data
        // Since this is a static variable, we only need to do this once, when the app turns on
        // Doing it this way saves us from repeating this work every time the activity is recreated (such as from a rotation)
        if (baseSpells.isEmpty()) {
            try {
                final JSONArray jsonArray = loadJSONArrayfromAsset(spellsFilename);
                final SpellCodec codec = new SpellCodec(this);
                baseSpells = codec.parseSpellList(jsonArray);
            } catch (Exception e) {
                e.printStackTrace();
                this.finish();
            }
        }

        if (englishSpells.isEmpty()) {
            try {
                final JSONArray jsonArray = loadJSONArrayfromAsset(englishSpellsFilename);
                final SpellCodec codec = new SpellCodec(this);
                englishSpells = codec.parseSpellList(jsonArray, true);
            } catch (Exception e) {
                e.printStackTrace();
                this.finish();
            }
        }

        // Load any created spells
        //loadCreatedSpells();

        // Load the settings and the character profile
        try {

            // Load the settings
            final JSONObject json = loadJSONfromData(settingsFile);
            //System.out.println(json.toString());
            settings = new Settings(json);

            // Test
            //final JSONObject testJSON = new JSONObject("{\"CharacterName\":\"2B\",\"Spells\":[],\"SortField1\":\"Name\",\"SortField2\":\"Name\",\"Reverse1\":false,\"Reverse2\":false,\"HiddenCastingTimeTypes\":[],\"HiddenSourcebooks\":[\"XGE\",\"SCAG\",\"TCE\"],\"HiddenRangeTypes\":[],\"HiddenSchools\":[],\"HiddenDurationTypes\":[],\"HiddenCasters\":[],\"QuantityRanges\":{\"CastingTimeFilters\":{\"MinUnit\":\"second\",\"MaxUnit\":\"hour\",\"MinText\":\"0\",\"MaxText\":\"24\"},\"RangeFilters\":{\"MinUnit\":\"foot\",\"MaxUnit\":\"mile\",\"MinText\":\"0\",\"MaxText\":\"1\"},\"DurationFilters\":{\"MinUnit\":\"second\",\"MaxUnit\":\"day\",\"MinText\":\"0\",\"MaxText\":\"30\"}},\"StatusFilter\":\"All\",\"Ritual\":true,\"NotRitual\":true,\"Concentration\":true,\"NotConcentration\":true,\"ComponentsFilters\":[true,true,true],\"NotComponentsFilters\":[true,true,true],\"MinSpellLevel\":0,\"MaxSpellLevel\":9,\"ApplyFiltersToSpellLists\":false,\"ApplyFiltersToSearch\":false,\"UseTCEExpandedLists\":false,\"VersionCode\":\"2.10.0\"}");
            //final CharacterProfile test = CharacterProfile.fromJSON(testJSON);

            // Load the character profile
            final String charName = settings.characterName();
            if (charName != null) {
                loadCharacterProfile(charName, true);

                // Set the character's name in the side menu
                if (characterProfile != null) {
                    setSideMenuCharacterName();
                }
            }

        } catch (Exception e) {
            String s = loadAssetAsString(new File(settingsFile));
            Log.v(TAG, "Error loading settings");
            Log.v(TAG, "The settings file content is: " + s);
            settings = new Settings();
            final List<String> characterList = charactersList();
            if (characterList.size() > 0) {
                final String firstCharacter = characterList.get(0);
                settings.setCharacterName(firstCharacter);
            }
            e.printStackTrace();
            saveSettings();
        }

        // If the character profile is null, we create one
        //System.out.println("Do we need to open a character creation window?");
        //System.out.println( (settings.characterName() == null) || characterProfile == null );
        if ( (settings.characterName() == null) || characterProfile == null ) {
            openCharacterCreationDialog();
        }

        // Set up the RecyclerView that holds the cells
        setupSpellRecycler(baseSpells);

        // Set up the SwipeRefreshLayout
        setupSwipeRefreshLayout();

        // Set up the side menu
        setupSideMenu();

        // The right nav drawer often gets in the way of fast scrolling on a phone
        // Since we can open it from the action bar, we'll lock it closed from swiping
        if (!onTablet) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        }

        // Set the correct view visibilities
        if (filterVisible) {
            updateWindowVisibilities();
        }

        // Sort and filter if the filter isn't visible
        if (!filterVisible && characterProfile != null) {
            sort();
            filter();
        }

        // If we need to, open the update dialog
        showUpdateDialog(true);

    }


    // Add actions to the action bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);

        // Get the filter menu button
        // Set up the state, if necessary
        filterMenuIcon = menu.findItem(R.id.action_filter);
        if (filterVisible) {
            final int filterIcon = onTablet ? R.drawable.ic_data : R.drawable.ic_list;
            filterMenuIcon.setIcon(filterIcon);
        }

        // Associate searchable configuration with the SearchView
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchViewIcon = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchViewIcon.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Set up the state, if necessary
        if (filterVisible && !onTablet) {
            searchViewIcon.setVisible(false);
        }

        // Set up the SearchView functions
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                spellRecycler.stopScroll();
                spellAdapter.getFilter().filter(text);
                return true;
            }
        });

        return true;
    }

    // To handle actions
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                toggleWindowVisibilities();
                return true;
            case R.id.action_info:
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        //System.out.println("Calling onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        //System.out.println("Calling onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        //System.out.println("Calling onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        //System.out.println("Calling onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        //System.out.println("Calling onDestroy");
        super.onDestroy();
    }

    // Necessary for handling rotations
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (onTablet && amBinding != null && spellWindowBinding.getSpell() != null) {
            outState.putParcelable(spellBundleKey, spellWindowBinding.getSpell());
            outState.putInt(spellIndexBundleKey, spellWindowBinding.getSpellIndex());
            outState.putBoolean(FAVORITE_KEY, spellWindowBinding.favoriteButton.isSet());
            outState.putBoolean(PREPARED_KEY, spellWindowBinding.preparedButton.isSet());
            outState.putBoolean(KNOWN_KEY, spellWindowBinding.knownButton.isSet());
        }
        outState.putBoolean(FILTER_VISIBLE_KEY, filterVisible);
    }

    // Close the drawer with the back button if it's open
    @Override
    public void onBackPressed() {
        // InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else if (filterVisible) {
            toggleWindowVisibilities();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.SPELL_WINDOW_REQUEST && resultCode == RESULT_OK) {
            final Spell s = data.getParcelableExtra(SpellWindow.SPELL_KEY);
            final boolean fav = data.getBooleanExtra(SpellWindow.FAVORITE_KEY, false);
            final boolean known = data.getBooleanExtra(SpellWindow.KNOWN_KEY, false);
            final boolean prepared = data.getBooleanExtra(SpellWindow.PREPARED_KEY, false);
            final int index = data.getIntExtra(SpellWindow.INDEX_KEY, -1);
            final boolean wasFav = characterProfile.isFavorite(s);
            final boolean wasKnown = characterProfile.isKnown(s);
            final boolean wasPrepared = characterProfile.isPrepared(s);
            characterProfile.setFavorite(s, fav);
            characterProfile.setKnown(s, known);
            characterProfile.setPrepared(s, prepared);
            final boolean changed = (wasFav != fav) || (wasKnown != known) || (wasPrepared != prepared);
            final Menu menu = navView.getMenu();
            final boolean oneChecked = menu.findItem(R.id.nav_favorites).isChecked() || menu.findItem(R.id.nav_known).isChecked() || menu.findItem(R.id.nav_prepared).isChecked();

            // If the spell's status changed, take care of the necessary changes
            if (changed) {

                // Re-display the spells if we have at least one filter selected
                if (oneChecked) {
                    filter();
                } else {
                    spellAdapter.notifyItemChanged(index);
                }

                // Save
                saveCharacterProfile();
                saveSettings();
            }

        } else if (requestCode == RequestCodes.SPELL_CREATION_REQUEST && resultCode == RESULT_OK) {

        }
    }

    void openSpellWindow(Spell spell, int pos) {

        // On a phone, we're going to open a new window by starting a SpellWindow activity
        if (!onTablet) {
            try {
                final Intent intent = new Intent(MainActivity.this, SpellWindow.class);
                intent.putExtra(SpellWindow.SPELL_KEY, spell);
                intent.putExtra(SpellWindow.TEXT_SIZE_KEY, settings.spellTextSize());
                intent.putExtra(SpellWindow.FAVORITE_KEY, characterProfile.isFavorite(spell));
                intent.putExtra(SpellWindow.PREPARED_KEY, characterProfile.isPrepared(spell));
                intent.putExtra(SpellWindow.KNOWN_KEY, characterProfile.isKnown(spell));
                intent.putExtra(SpellWindow.USE_EXPANDED_KEY, characterProfile.getUseTCEExpandedLists());
                intent.putExtra(SpellWindow.INDEX_KEY, pos);
                startActivityForResult(intent, RequestCodes.SPELL_WINDOW_REQUEST);
                overridePendingTransition(R.anim.right_to_left_enter, R.anim.identity);
            } catch (Exception e) { e.printStackTrace(); }
        }

        // On a tablet, we'll show the spell info on the right-hand side of the screen
        else {
            //spellWindowCL.setVisibility(View.VISIBLE);
            spellWindowBinding.setSpell(spell);
            spellWindowBinding.setSpellIndex(pos);
            spellWindowBinding.setUseExpanded(characterProfile.getUseTCEExpandedLists());
            spellWindowBinding.executePendingBindings();
//            if (spellWindowBinding.getSpell() != null) {
//                System.out.println("binding spell is " + spellWindowBinding.getSpell().getName());
//            } else {
//                System.out.println("binding spell is null");
//            }
//            System.out.println("The current spell is now " + spell.getName());
            filterVisible = false;
            updateWindowVisibilities();
            final ToggleButton favoriteButton = spellWindowCL.findViewById(R.id.favorite_button);
            favoriteButton.set(characterProfile.isFavorite(spell));
            final ToggleButton preparedButton = spellWindowCL.findViewById(R.id.prepared_button);
            preparedButton.set(characterProfile.isPrepared(spell));
            final ToggleButton knownButton = spellWindowCL.findViewById(R.id.known_button);
            knownButton.set(characterProfile.isKnown(spell));
        }
    }

    void openSpellPopup(View view, Spell spell) {
        final SpellStatusPopup ssp = new SpellStatusPopup(this, spell);
        ssp.showUnderView(view);
    }

    void setupSpellRecycler(List<Spell> spells) {
        spellRecycler = amBinding.spellRecycler;
        final RecyclerView.LayoutManager spellLayoutManager = new LinearLayoutManager(this);
        spellAdapter = new SpellRowAdapter(spells);
        spellRecycler.setAdapter(spellAdapter);
        spellRecycler.setLayoutManager(spellLayoutManager);

        // If we're on a tablet, we need to keep track of the index of the currently selected spell when the list changes
        if (onTablet) {
            spellAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    updateSpellIndex(spellAdapter.getSpellIndex(spellWindowBinding.getSpell()));
                }
            });
        }
    }

    void setupSortElements() {

        // Get various UI elements
        final SortLayoutBinding sortBinding = sortFilterBinding.sortBlock;
        sort1 = sortBinding.sortField1Spinner;
        sort2 = sortBinding.sortField2Spinner;
        sortArrow1 = sortBinding.sortField1Arrow;
        sortArrow2 = sortBinding.sortField2Arrow;

        // Set the views to be expanded
        expandingViews.put(sortBinding.sortHeader, sortBinding.sortContent);

        // Set tags for the sorting UI elements
        sort1.setTag(1);
        sort2.setTag(2);
        sortArrow1.setTag(1);
        sortArrow2.setTag(2);

        // Populate the dropdown spinners
        final int sortTextSize = 18;
        final NamedSpinnerAdapter sortAdapter1 = new NamedSpinnerAdapter<>(this, SortField.class, DisplayUtils::getDisplayName, sortTextSize);
        final NamedSpinnerAdapter sortAdapter2 = new NamedSpinnerAdapter<>(this, SortField.class, DisplayUtils::getDisplayName, sortTextSize);
        sort1.setAdapter(sortAdapter1);
        sort2.setAdapter(sortAdapter2);


        // Set what happens when the sort spinners are changed
        final AdapterView.OnItemSelectedListener sortListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (characterProfile == null) { return; }
                final int tag = (int) adapterView.getTag();
                final SortField sf = (SortField) adapterView.getItemAtPosition(position);;
                characterProfile.setSortField(sf, tag);
                saveCharacterProfile();
                sortOnTablet.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
        sort1.setOnItemSelectedListener(sortListener);
        sort2.setOnItemSelectedListener(sortListener);

        // Set what happens when the arrow buttons are pressed
        final SortDirectionButton.OnClickListener arrowListener = (View view) -> {
            final SortDirectionButton b = (SortDirectionButton) view;
            b.onPress();
            sort();
            final boolean up = b.pointingUp();
            if (characterProfile == null) { return; }
            //try {
                final int tag = (int) view.getTag();
                characterProfile.setSortReverse(up, tag);

            //} catch (Exception e) {
            //    e.printStackTrace();
            //}
            saveCharacterProfile();
        };
        sortArrow1.setOnClickListener(arrowListener);
        sortArrow2.setOnClickListener(arrowListener);

    }

    private void setupSwipeRefreshLayout() {
        // Set up the 'swipe down to filter' behavior of the RecyclerView
        final SwipeRefreshLayout swipeLayout = amBinding.swipeRefreshLayout;
        swipeLayout.setOnRefreshListener(() -> {
            filter();
            swipeLayout.setRefreshing(false);
        });

        // Configure the refreshing colors
        swipeLayout.setColorSchemeResources(R.color.darkBrown, R.color.lightBrown, R.color.black);
    }

    private void setupRightNav() {

        // Get the right navigation view and the ExpandableListView
        //rightNavView = amBinding.rightMenu;
        rightExpLV = amBinding.navRightExpandable;

        // Get the list of group names, as an Array
        // The group names are the headers in the expandable list
        final List<String> rightNavGroups = Arrays.asList(getResources().getStringArray(R.array.right_group_names));

        // Get the names of the group elements, as Arrays
        final List<String[]> groups = new ArrayList<>();
        groups.add(getResources().getStringArray(R.array.basics_items));
        groups.add(getResources().getStringArray(R.array.casting_spell_items));
        final String[] casterNames = DisplayUtils.getDisplayNames(this, CasterClass.class);
        groups.add(casterNames);

        // For each group, get the text that corresponds to each child
        // Here, entries with the same index correspond to one another
        final List<Integer> basicsIDs = new ArrayList<>(Arrays.asList(R.string.what_is_a_spell, R.string.spell_level_info,
                R.string.known_and_prepared_spells, R.string.the_schools_of_magic, R.string.spell_slots_info, R.string.cantrips,
                R.string.rituals, R.string.the_weave_of_magic));
        final List<Integer> castingSpellIDs = new ArrayList<>(Arrays.asList(R.string.casting_time_info, R.string.range_info, R.string.components_info,
                R.string.duration_info, R.string.targets, R.string.areas_of_effect, R.string.saving_throws,
                R.string.attack_rolls, R.string.combining_magical_effects, R.string.casting_in_armor));
        final List<Integer> classInfoIDs = IntStream.of(LocalizationUtils.supportedSpellcastingInfoIDs()).boxed().collect(Collectors.toList());
        final List<List<Integer>> childTextLists = new ArrayList<>(Arrays.asList(basicsIDs, castingSpellIDs, classInfoIDs));

        // Create maps of the form group -> list of children, and group -> list of children's text
        final int nGroups = rightNavGroups.size();
        final Map<String, List<String>> childData = new HashMap<>();
        final Map<String, List<Integer>> childTextIDs = new HashMap<>();
        for (int i = 0; i < nGroups; ++i) {
            childData.put(rightNavGroups.get(i), Arrays.asList(groups.get(i)));
            childTextIDs.put(rightNavGroups.get(i), childTextLists.get(i));
        }

        // Create the tables array
        final int[] tableIDs = LocalizationUtils.supportedTableLayoutIDs();
        //final int[] tableIDs = new int[]{ R.string.bard_table, R.string.cleric_table, R.string.druid_table, R.string.paladin_table, R.string.ranger_table, R.string.sorcerer_table, R.string.warlock_table, R.string.wizard_table };

        // Create the adapter
        rightAdapter = new NavExpandableListAdapter(this, rightNavGroups, childData, childTextIDs, tableIDs);
        rightExpLV.setAdapter(rightAdapter);
        final View rightHeaderView = getLayoutInflater().inflate(R.layout.right_expander_header, null);
        rightExpLV.addHeaderView(rightHeaderView);

        // Set the callback that displays the appropriate popup when the list item is clicked
        rightExpLV.setOnChildClickListener((ExpandableListView elView, View view, int gp, int cp, long id) -> {
            final NavExpandableListAdapter adapter = (NavExpandableListAdapter) elView.getExpandableListAdapter();
            final String title = (String) adapter.getChild(gp, cp);
            final int textID = adapter.childTextID(gp, cp);
            final int tableID = adapter.getTableID(gp, cp);

            // Show a popup
            //SpellcastingInfoPopup popup = new SpellcastingInfoPopup(this, title, textID, true);
            //popup.show();

            // Show a full-screen activity
            final Intent intent = new Intent(MainActivity.this, SpellcastingInfoWindow.class);
            intent.putExtra(SpellcastingInfoWindow.TITLE_KEY, title);
            intent.putExtra(SpellcastingInfoWindow.INFO_KEY, textID);
            intent.putExtra(SpellcastingInfoWindow.TABLE_KEY, tableID);
            startActivity(intent);
            overridePendingTransition(R.anim.right_to_left_enter, R.anim.identity);

            return true;
        });
    }

    private void setupSideMenu() {
        final int[] ids = new int[]{ R.id.nav_character, R.id.nav_update_title, R.id.nav_feedback_title };
        for (int id : ids) {
            setSideMenuTitleText(id, null);
        }
    }

    public static void showKeyboard(EditText mEtSearch, Context context) {
        mEtSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEtSearch, 0);
    }

    public static void hideSoftKeyboard(View view, Context context) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    void filter() {
        if (spellAdapter == null) { return; }
        final CharSequence query = (searchView != null) ? searchView.getQuery() : "";
        spellAdapter.filter(query);
    }

    private void singleSort() {
        final SortField sf1 = characterProfile.getFirstSortField();
        final boolean reverse1 = sortArrow1.pointingUp();
        spellAdapter.singleSort(sf1, reverse1);
    }

    private void doubleSort() {
        final SortField sf1 = characterProfile.getFirstSortField();
        final SortField sf2 = characterProfile.getSecondSortField();
        final boolean reverse1 = sortArrow1.pointingUp();
        final boolean reverse2 = sortArrow2.pointingUp();
        spellAdapter.doubleSort(sf1, sf2, reverse1, reverse2);
    }

    private void sort() {
        doubleSort();
    }


    JSONArray loadJSONArrayfromAsset(String assetFilename) throws JSONException {
        String jsonStr;
        try {
            final InputStream is = getAssets().open(assetFilename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonStr = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new JSONArray(jsonStr);
    }

    JSONObject loadJSONObjectfromAsset(String assetFilename) throws JSONException {
        String jsonStr;
        try {
            final InputStream is = getAssets().open(assetFilename);
            final int size = is.available();
            final byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonStr = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new JSONObject(jsonStr);
    }

    JSONObject loadJSONfromData(File file) throws JSONException {
        String jsonStr;
        try {
            final FileInputStream is = new FileInputStream(file);
            final int size = is.available();
            final byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonStr = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new JSONObject(jsonStr);
    }

    String loadAssetAsString(File file) {
        try {
            InputStream is = new FileInputStream(file);
            final int size = is.available();
            final byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    JSONObject loadJSONfromData(String dataFilename) throws JSONException {
        return loadJSONfromData(new File(getApplicationContext().getFilesDir(), dataFilename));
    }

    private void saveJSON(JSONObject json, File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveJSON(JSONArray json, File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(json.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Saves the current settings to a file, in JSON format
    private boolean saveSettings() {
        final File settingsLocation = new File(getApplicationContext().getFilesDir(), settingsFile);
        return settings.save(settingsLocation);
    }

    CharacterProfile getProfileByName(String name) {

        final String characterFile = name + ".json";
        final File profileLocation = new File(profilesDir, characterFile);
        if (! (profileLocation.exists() && profileLocation.isFile()) ) {
            return null;
        }

        try {
            final JSONObject json = loadJSONfromData(profileLocation);
            return CharacterProfile.fromJSON(json);
        } catch (JSONException e) {
            final String charStr = loadAssetAsString(profileLocation);
            Log.v(TAG, "The offending JSON is: " + charStr);
            return null;
        }

    }

    void loadCharacterProfile(String charName, boolean initialLoad) {

        //System.out.println("Loading character: " + charName);

        // We don't need to do anything if the given character is already the current one
        boolean skip = (characterProfile != null) && charName.equals(characterProfile.getName());
        if (!skip) {
            final String charFile = charName + ".json";
            final File profileLocation = new File(profilesDir, charFile);
            try {
                final JSONObject charJSON = loadJSONfromData(profileLocation);
                final CharacterProfile profile = CharacterProfile.fromJSON(charJSON);
                //System.out.println("The file location is " + profileLocation);
                //System.out.println("The character JSON is " + charJSON);
                setCharacterProfile(profile, initialLoad);
                //System.out.println("characterProfile is " + characterProfile.getName());
            } catch (JSONException e) {
                final String charStr = loadAssetAsString(profileLocation);
                Log.v(TAG, "The offending JSON is: " + charStr);
                e.printStackTrace();
            }

        }
    }
    void loadCharacterProfile(String charName) {
        loadCharacterProfile(charName, false);
    }

    boolean saveCharacterProfile(CharacterProfile profile) {
        try {
            final String charFile = profile.getName() + ".json";
            final File profileLocation = new File(profilesDir, charFile);
            return profile.save(profileLocation);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    boolean saveCharacterProfile() { return saveCharacterProfile(characterProfile); }

    private void setSideMenuTitleText(int itemID, CharSequence text) {
        final Menu menu = amBinding.sideMenu.getMenu();
        final MenuItem menuItem = menu.findItem(itemID);
        final CharSequence title = text != null ? text : (menuItem.getTitle() != null ? menuItem.getTitle() : "");
        final SpannableString ss = new SpannableString(title);
        ss.setSpan(new ForegroundColorSpan(SpellbookUtils.defaultColor), 0, ss.length(), 0);
        menuItem.setTitle(ss);

    }

    private void setSideMenuCharacterName() {
        final String title = getString(R.string.prompt, getString(R.string.character), characterProfile.getName());
        setSideMenuTitleText(R.id.nav_character, title);
    }

    private void setFilterSettings() {

        // Set the min and max level entries
        sortFilterBinding.levelFilterRange.minLevelEntry.setText(String.valueOf(characterProfile.getMinSpellLevel()));
        sortFilterBinding.levelFilterRange.maxLevelEntry.setText(String.valueOf(characterProfile.getMaxSpellLevel()));

        // Set the filter option selectors appropriately
        sortFilterBinding.filterOptions.filterListsLayout.optionChooser.setChecked(characterProfile.getApplyFiltersToSpellLists());
        sortFilterBinding.filterOptions.filterSearchLayout.optionChooser.setChecked(characterProfile.getApplyFiltersToSearch());
        sortFilterBinding.filterOptions.useExpandedLayout.optionChooser.setChecked(characterProfile.getUseTCEExpandedLists());

        // Set the status filter
        final StatusFilterField sff = characterProfile.getStatusFilter();
        navView.getMenu().getItem(sff.getIndex()).setChecked(true);

        // Set the right values for the ranges views
        for (HashMap.Entry<Class<? extends QuantityType>, RangeFilterLayoutBinding> entry : classToRangeMap.entrySet()) {
            updateRangeView(entry.getKey(), entry.getValue());
        }
    }

    // When changing character profiles, this adjusts the sort settings to match the new profile
    private void setSortSettings() {

        // Set the spinners to the appropriate positions
        final NamedSpinnerAdapter<SortField> adapter = (NamedSpinnerAdapter<SortField>) sort1.getAdapter();
        final List<SortField> sortData = Arrays.asList(adapter.getData());
        final SortField sf1 = characterProfile.getFirstSortField();
        sort1.setSelection(sortData.indexOf(sf1), false);
        final SortField sf2 = characterProfile.getSecondSortField();
        sort2.setSelection(sortData.indexOf(sf2), false);

        // Set the sort directions
        final boolean reverse1 = characterProfile.getFirstSortReverse();
        if (reverse1) {
            sortArrow1.setUp();
        } else {
            sortArrow1.setDown();
        }
        final boolean reverse2 = characterProfile.getSecondSortReverse();
        if (reverse2) {
            sortArrow2.setUp();
        } else {
            sortArrow2.setDown();
        }

    }

    // Sets the given character profile to the active one
    // The boolean parameter should only be true if this is called during initial setup, when all of the UI elements may not be initialized yet
    void setCharacterProfile(CharacterProfile cp, boolean initialLoad) {
        //System.out.println("Setting character profile: " + cp.getName());
        characterProfile = cp;
        settings.setCharacterName(cp.getName());
        //System.out.println("Set characterProfile to " + cp.getName());

        setSideMenuCharacterName();
        setSortSettings();
        setFilterSettings();
        saveSettings();
        saveCharacterProfile();
        try {
            if (!initialLoad) {
                sort();
                filter();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        // Update the sort/filter bindings when we're changing characters
        updateSortFilterBindings();

        // Reset the spell view if on the tablet
        if (onTablet && !initialLoad) {
            spellWindowCL.setVisibility(View.INVISIBLE);
            spellWindowBinding.setSpell(null);
            spellWindowBinding.setSpellIndex(-1);
            spellWindowBinding.executePendingBindings();
        }
    }

    // Sets the given character profile to be the active one
    void setCharacterProfile(CharacterProfile cp) {
        setCharacterProfile(cp, false);
    }

    // Opens a character creation dialog
    void openCharacterCreationDialog() {
        final CreateCharacterDialog dialog = new CreateCharacterDialog();
        final Bundle args = new Bundle();
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "createCharacter");
    }

    void openFeedbackWindow() {
        final FeedbackDialog dialog = new FeedbackDialog();
        final Bundle args = new Bundle();
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "feedback");
    }

    void openOptionInfoDialog(FilterOptionBinding binding) {
        final OptionInfoDialog dialog = new OptionInfoDialog();
        final Bundle args = new Bundle();
        args.putString(OptionInfoDialog.TITLE_KEY, binding.getTitle());
        args.putString(OptionInfoDialog.DESCRIPTION_KEY, binding.getDescription());
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "filter_option_dialog");
    }

    // Opens the email chooser to send feedback
    // In the unlikely event that the user doesn't have an email application, a Toast message displays instead
    void sendFeedback() {
        final Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{devEmail});
        i.putExtra(Intent.EXTRA_SUBJECT, emailMessage);
        try {
            startActivity(Intent.createChooser(i, getString(R.string.send_email)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(R.string.no_email_clients), Toast.LENGTH_SHORT).show();
        }
    }

    // Deletes the character profile corresponding to the given name, if one exists
    boolean deleteCharacterProfile(String name) {
        final String charFile = name + ".json";
        final File profileLocation = new File(profilesDir, charFile);
        // final String profileLocationStr = profileLocation.toString();
        final boolean success = profileLocation.delete();

        if (!success) {
            Log.v(TAG, "Error deleting character: " + profileLocation);
        }
//        else {
//            System.out.println("Successfully deleted the data file for " + name);
//            System.out.println("File location was " + profileLocationStr);
//        }

        if (success && name.equals(characterProfile.getName())) {
            final ArrayList<String> characters = charactersList();
            if (characters.size() > 0) {
                loadCharacterProfile(characters.get(0));
                saveSettings();
            } else {
                openCharacterCreationDialog();
            }
        }

        return success;
    }

    // Returns the current list of characters
    ArrayList<String> charactersList() {
        final ArrayList<String> charList = new ArrayList<>();
        final int toRemove = CHARACTER_EXTENSION.length();
        for (File file : profilesDir.listFiles()) {
            final String filename = file.getName();
            if (filename.endsWith(CHARACTER_EXTENSION)) {
                final String charName = filename.substring(0, filename.length() - toRemove);
                charList.add(charName);
            }
        }
        charList.sort(String::compareToIgnoreCase);
        return charList;
    }

    // Opens a character selection dialog
    void openCharacterSelection() {
        final CharacterSelectionDialog dialog = new CharacterSelectionDialog();
        final Bundle args = new Bundle();
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "selectCharacter");
    }

    // This function performs filtering only if one of the spell lists is currently selected
    void filterIfStatusSet() {
        if (characterProfile.isStatusSet()) {
            filter();
        }
    }


    File getProfilesDir() { return profilesDir; }
    CharacterProfile getCharacterProfile() { return characterProfile; }
    Settings getSettings() { return settings; }
    CharacterSelectionDialog getSelectionDialog() { return selectionDialog; }
    View getCharacterSelect() { return characterSelect; }
    void setCharacterSelect(View v) { characterSelect = v;}
    void setSelectionDialog(CharacterSelectionDialog d) { selectionDialog = d; }
    boolean usingTablet() { return onTablet; }


    // This function takes care of any setup that's needed only on a tablet layout
    private void tabletSetup() {

        // Get the spell window binding
        spellWindowBinding = amBinding.spellWindowLayout;

        // Spell window background
        spellWindowCL = spellWindowBinding.spellWindowConstraint;
        spellWindowCL.setBackground(null);
        spellWindowCL.setVisibility(View.INVISIBLE);

        // Set button callbacks
        spellWindowBinding.favoriteButton.setOnClickListener((v) -> {
            characterProfile.toggleFavorite(spellWindowBinding.getSpell());
            spellAdapter.notifyItemChanged(spellWindowBinding.getSpellIndex());
            saveCharacterProfile();
        });
        spellWindowBinding.knownButton.setOnClickListener((v) -> {
            characterProfile.toggleKnown(spellWindowBinding.getSpell());
            spellAdapter.notifyItemChanged(spellWindowBinding.getSpellIndex());
            saveCharacterProfile();
        });
        spellWindowBinding.preparedButton.setOnClickListener((v) -> {
            characterProfile.togglePrepared(spellWindowBinding.getSpell());
            spellAdapter.notifyItemChanged(spellWindowBinding.getSpellIndex());
            //System.out.println("Current spell index is " + spellWindowBinding.getSpellIndex());
            saveCharacterProfile();
        });
    }

    // If we're on a tablet, this function updates the spell window to match its status in the character profile
    // This is called after one of the spell list buttons is pressed for that spell in the main table
    void updateSpellWindow(Spell spell, boolean favorite, boolean prepared, boolean known) {
        if (onTablet && (spellWindowCL.getVisibility() == View.VISIBLE) && (amBinding != null) && (spell.equals(spellWindowBinding.getSpell())) ) {
            spellWindowBinding.favoriteButton.set(favorite);
            spellWindowBinding.preparedButton.set(prepared);
            spellWindowBinding.knownButton.set(known);
        }
    }

    void updateSpellIndex(int index) {
        if (spellWindowBinding != null) {
            //System.out.println("New index is " + index);
            spellWindowBinding.setSpell(spellWindowBinding.getSpell());
            spellWindowBinding.setSpellIndex(index);
            spellWindowBinding.executePendingBindings();
        }
    }
    Spell getCurrentSpell() {
//        if (spellWindowBinding.getSpell() != null) {
//            System.out.println("binding spell is " + spellWindowBinding.getSpell().getName());
//        } else {
//            System.out.println("binding spell is null");
//        }
        return spellWindowBinding.getSpell();
    }

    // This function clears the current focus
    // It also closes the soft keyboard, if it's open
    private void clearCurrentFocus() {
        final View view = getCurrentFocus();
        if (view != null) {
            view.clearFocus();
        }
    }

    // This function clears the current focus ONLY IF the focused view is of the given type
    private <T extends View> void clearViewTypeFocus(Class<T> viewType) {
        //System.out.println("Running clearViewTypeFocus with viewType as " + viewType);
        final View view = getCurrentFocus();
        if (viewType.isInstance(view)) {
            view.clearFocus();
            //System.out.println("Cleared focus");
        }
    }

    private void hideSoftKeyboard() {
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isAcceptingText()) {
            imm.toggleSoftInput(0, 0);
        }
    }

    private Sextet<GridLayout,SortFilterHeaderView,View,Button,Button,Button> getFilterViews(ViewBinding binding) {
        if (binding instanceof FilterBlockLayoutBinding) {
            final FilterBlockLayoutBinding filterBinding = (FilterBlockLayoutBinding) binding;
            return new Sextet<>(filterBinding.filterGrid.filterGridLayout, filterBinding.filterHeader, filterBinding.filterBlockContent, filterBinding.selectAllButton, filterBinding.unselectAllButton, null);
        } else if (binding instanceof FilterBlockRangeLayoutBinding) {
            final FilterBlockRangeLayoutBinding filterBinding = (FilterBlockRangeLayoutBinding) binding;
            return new Sextet<>(filterBinding.filterGrid.filterGridLayout, filterBinding.filterHeader, filterBinding.filterRangeBlockContent, filterBinding.selectAllButton, filterBinding.unselectAllButton, null);
        } else if (binding instanceof FilterBlockFeaturedLayoutBinding) {
            final FilterBlockFeaturedLayoutBinding filterBinding = (FilterBlockFeaturedLayoutBinding) binding;
            return new Sextet<>(filterBinding.filterGrid.filterGridLayout, filterBinding.featuredFilterHeader, filterBinding.featuredBlockContent, filterBinding.featuredSelectAllButton, filterBinding.featuredUnselectAllButton, filterBinding.showMoreButton);
        }
        return new Sextet<>(null, null, null, null, null, null); // We shouldn't get here
    }


    // The code for populating the filters is all essentially the same
    // So we can just use this generic function to remove redundancy
    private <E extends Enum<E> & NameDisplayable> ArrayList<ItemFilterViewBinding> populateFilters(Class<E> enumType, E[] items, E[] featuredItems) {

        // Get the GridLayout and the appropriate column weight
        final Quartet<Boolean,Function<SortFilterLayoutBinding,ViewBinding>,Integer,Integer> data = filterBlockInfo.get(enumType);
        final boolean rangeNeeded = data.getValue0();
        final String title = stringFromID(data.getValue2());
        //final int size = (int) dimensionFromID(R.dimen.sort_filter_titles_text_size);
        final int columns = getResources().getInteger(data.getValue3());
        final ViewBinding filterBinding = data.getValue1().apply(sortFilterBinding);
//        final FilterBlockRangeLayoutBinding blockRangeBinding = (filterBinding instanceof FilterBlockRangeLayoutBinding) ? (FilterBlockRangeLayoutBinding) filterBinding : null;
//        final FilterBlockLayoutBinding blockBinding = (filterBinding instanceof FilterBlockLayoutBinding) ? (FilterBlockLayoutBinding) filterBinding : null;
//        final GridLayout gridLayout = rangeNeeded ? blockRangeBinding.filterGrid.filterGridLayout : blockBinding.filterGrid.filterGridLayout;
//        final Button selectAllButton = rangeNeeded ? blockRangeBinding.selectAllButton : blockBinding.selectAllButton;
//        final Button unselectAllButton = rangeNeeded ? blockRangeBinding.unselectAllButton : blockBinding.unselectAllButton;
//        final SortFilterHeaderView headerView = rangeNeeded ? blockRangeBinding.filterHeader : blockBinding.filterHeader;
//        final View contentView = rangeNeeded ? blockRangeBinding.filterRangeBlockContent : blockBinding.filterBlockContent;

        final boolean additional = featuredItems != null;
        final Sextet<GridLayout,SortFilterHeaderView,View,Button,Button,Button> filterViews = getFilterViews(filterBinding);
        final GridLayout gridLayout = filterViews.getValue0();
        final SortFilterHeaderView headerView = filterViews.getValue1();
        final View contentView = filterViews.getValue2();
        final Button selectAllButton = filterViews.getValue3();
        final Button unselectAllButton = filterViews.getValue4();
        final Button showMoreButton = filterViews.getValue5();
        headerView.setTitle(title);
        //headerView.setTitleSize(size);
        gridLayout.setColumnCount(columns);

        // Set up expanding header views
        expandingViews.put(headerView, contentView);

        // An empty list of bindings. We'll populate this and return it
        final ArrayList<ItemFilterViewBinding> bindings = new ArrayList<>();

        // The default thing to do for one of the filter buttons
        final Consumer<ToggleButton> defaultConsumer = (v) -> {
            characterProfile.toggleVisibility((E) v.getTag());
            saveCharacterProfile();
            filterOnTablet.run();
        };

        // Map for the buttons
        final Map<NameDisplayable,ToggleButton> buttons = new HashMap<>();
        filterButtonMaps.put(enumType, buttons);

        // Sort the enums by name
        final Comparator<E> comparator = Comparator.comparing(e -> DisplayUtils.getDisplayName(this, e));
        Arrays.sort(items, comparator);

        // We want to sort the items so that the featured items come before the other items
        final E[] sortedItems = items.clone();
        final Comparator<E> featuredPreSorter = (e1, e2) -> {
            final int r1 = ArrayUtils.contains(featuredItems, e1) ? 1 : 0;
            final int r2 = ArrayUtils.contains(featuredItems, e2) ? 1 : 0;
            return r2 - r1;
        };
        Arrays.sort(sortedItems, featuredPreSorter);

        final Collection<View> notFeaturedRows = new ArrayList<>();

        // Populate the list of bindings, one for each instance of the given Enum type
        for (E e : sortedItems) {

            // Create the layout parameters
            //final GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f),  GridLayout.spec(GridLayout.UNDEFINED, 1f));

            // Inflate the binding
            final ItemFilterViewBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_filter_view, null, false);

            // Bind the relevant values
            binding.setProfile(characterProfile);
            binding.setItem(e);
            binding.executePendingBindings();

            // Get the root view
            final View view = binding.getRoot();

            // Set up the toggle button
            final ToggleButton button = binding.itemFilterButton;
            buttons.put(e, button);
            button.setTag(e);
            final Consumer<ToggleButton> toggleButtonConsumer;

            // On a long press, turn off all other buttons in this grid, and turn this one on
            final Consumer<ToggleButton> longPressConsumer = (v) -> {
                if (!v.isSet()) { v.callOnClick(); }
                //final E item = (E) v.getTag();
                final Class<? extends NameDisplayable> type = (Class<? extends NameDisplayable>) e.getClass();
                final Map<NameDisplayable,ToggleButton> gridButtons = filterButtonMaps.get(type);
                if (gridButtons == null) { return; }
                SpellbookUtils.clickButtons(gridButtons.values(), (tb) -> (tb != v && tb.isSet()) );
            };
            button.setOnLongClickListener((v) -> { longPressConsumer.accept((ToggleButton) v); return true; });

            // If this is a spanning type, we want to also set up the range view, set the button to toggle the corresponding range view's visibility,
            // as well as do some other stuff
            final boolean spanning = ( rangeNeeded && (e instanceof QuantityType) && ( ((QuantityType) e).isSpanningType()) );
            if (spanning) {

                // Get the range view
                final FilterBlockRangeLayoutBinding blockRangeBinding = (FilterBlockRangeLayoutBinding) filterBinding;
                final RangeFilterLayoutBinding rangeBinding = blockRangeBinding.rangeFilter;

                // Add the range view to map of range views
                classToRangeMap.put( (Class<? extends QuantityType>) enumType, rangeBinding);

                // Set up the range view
                setupRangeView(rangeBinding, (QuantityType) e);

                toggleButtonConsumer = (v) -> {
                    defaultConsumer.accept(v);
                    rangeBinding.getRoot().setVisibility(rangeBinding.getRoot().getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                };
            } else {
                toggleButtonConsumer = defaultConsumer;
            }

            button.setOnClickListener(v -> toggleButtonConsumer.accept((ToggleButton) v));
            gridLayout.addView(view);
            bindings.add(binding);

            final boolean isAdditionalItem = !ArrayUtils.contains(featuredItems, e);
            if (isAdditionalItem) {
                notFeaturedRows.add(view);
                view.setVisibility(View.GONE);
            }
        }

        // Set up the select all button
        selectAllButton.setTag(enumType);
        selectAllButton.setOnClickListener((v) -> {
            final Class<? extends NameDisplayable> type = (Class<? extends NameDisplayable>) selectAllButton.getTag();
            final Map<NameDisplayable,ToggleButton> gridButtons = filterButtonMaps.get(type);
            if (gridButtons == null) { return; }
            SpellbookUtils.clickButtons(gridButtons.values(), (tb) -> !tb.isSet());
        });

        // Set up the unselect all button
        unselectAllButton.setTag(enumType);
        unselectAllButton.setOnClickListener((v) -> {
            final Class<? extends NameDisplayable> type = (Class<? extends NameDisplayable>) unselectAllButton.getTag();
            final Map<NameDisplayable,ToggleButton> gridButtons = filterButtonMaps.get(type);
            if (gridButtons == null) { return; }
            SpellbookUtils.clickButtons(gridButtons.values(), ToggleButton::isSet);
        });

        // Set up the show more/less button
        if (additional && showMoreButton != null) {
            showMoreButton.setTag(false);
            showMoreButton.setOnClickListener((v) -> {
                final boolean visible = (boolean) showMoreButton.getTag();
                for (View view : notFeaturedRows) {
                    view.setVisibility(visible ? View.GONE : View.VISIBLE);
                }
                showMoreButton.setTag(!visible);
                showMoreButton.setText(visible ? R.string.show_more : R.string.show_less);
            });
        }

        return bindings;
    }

    private <E extends Enum<E> & NameDisplayable> ArrayList<ItemFilterViewBinding> populateFilters(Class<E> enumType, E[] items) { return populateFilters(enumType, items, items); }

    private <E extends Enum<E> & NameDisplayable> ArrayList<ItemFilterViewBinding> populateFilters(Class<E> enumType) {
        // Get an array of instances of the Enum type
        final E[] items = enumType.getEnumConstants();

        // If this isn't an enum type, return our (currently empty) list
        // This should never happens
        if (items == null) { return new ArrayList<>(); }
        return populateFilters(enumType, items);
    }

    private <E extends Enum<E> & NameDisplayable> ArrayList<ItemFilterViewBinding> populateFeaturedFilters(Class<E> enumType, E[] items) {
        // Get an array of instances of the Enum type
        final E[] allItems = enumType.getEnumConstants();

        // If this isn't an enum type, return our (currently empty) list
        // This should never happens
        if (items == null) { return new ArrayList<>(); }
        return populateFilters(enumType, allItems, items);
    }

    // This function updates the character profile for all of the bindings at once
    private void updateSortFilterBindings() {
        for (List<ItemFilterViewBinding> bindings : classToBindingsMap.values()) {
            for (ItemFilterViewBinding binding : bindings) {
                binding.setProfile(characterProfile);
                binding.executePendingBindings();
            }
        }
        for (YesNoFilterViewBinding binding : yesNoBindings) {
            binding.setProfile(characterProfile);
            binding.executePendingBindings();
        }
        sortFilterBinding.levelFilterRange.setProfile(characterProfile);
        sortFilterBinding.levelFilterRange.executePendingBindings();
    }

    private void updateRangeView(Class<? extends QuantityType> quantityType, RangeFilterLayoutBinding rangeBinding) {

        // Get the appropriate data
        final Sextet<Class<? extends Quantity>, Class<? extends Unit>, Unit, Unit, Integer, Integer> data = characterProfile.getQuantityRangeInfo(quantityType);

        // Set the min and max text
        final EditText minET = rangeBinding.rangeMinEntry;
        minET.setText(String.format(Locale.US, "%d", data.getValue4()));
        final EditText maxET = rangeBinding.rangeMaxEntry;
        maxET.setText(String.format(Locale.US, "%d", data.getValue5()));

        // Set the min and max units
        final Spinner minUnitSpinner = rangeBinding.rangeMinSpinner;
        final Spinner maxUnitSpinner = rangeBinding.rangeMaxSpinner;
        final UnitTypeSpinnerAdapter unitAdapter = (UnitTypeSpinnerAdapter) minUnitSpinner.getAdapter();
        final List units = Arrays.asList(unitAdapter.getData());
        final Unit minUnit = data.getValue2();
        minUnitSpinner.setSelection(units.indexOf(minUnit), false);
        final Unit maxUnit = data.getValue3();
        maxUnitSpinner.setSelection(units.indexOf(maxUnit), false);

        // Set the visibility appropriately
        rangeBinding.getRoot().setVisibility(characterProfile.getSpanningTypeVisible(quantityType));

    }

    private <E extends QuantityType> void setupRangeView(RangeFilterLayoutBinding rangeBinding, E e) {

        // Get the range filter info
        final Class<? extends QuantityType> quantityType = e.getClass();
        final Triplet<Class<? extends Unit>,Integer,Integer> info = rangeViewInfo.get(quantityType);
        final Class<? extends Unit> unitType = info.getValue0();
        rangeBinding.getRoot().setTag(quantityType);
        final String rangeText = getResources().getString(info.getValue1());
        final int maxLength = getResources().getInteger(info.getValue2());

        // Set the range text
        final TextView rangeTV = rangeBinding.rangeTextView;
        rangeTV.setText(rangeText);

        // Get the unit plural names
        final Unit[] units = unitType.getEnumConstants();
        final String[] unitPluralNames = new String[units.length];
        for (int i = 0; i < units.length; ++i) {
            unitPluralNames[i] = DisplayUtils.getPluralName(this, units[i]);
        }

        // Set up the min spinner
        final int textSize = 14;
        final Spinner minUnitSpinner = rangeBinding.rangeMinSpinner;
        final UnitTypeSpinnerAdapter minUnitAdapter = new UnitTypeSpinnerAdapter(this, unitType, textSize);
        minUnitSpinner.setAdapter(minUnitAdapter);
        //minUnitSpinner.setTag(R.integer.key_0, 0); // Min or max
        //minUnitSpinner.setTag(R.integer.key_1, unitType); // Unit type
        //minUnitSpinner.setTag(R.integer.key_2, quantityType); // Quantity type

        // Set up the max spinner
        final Spinner maxUnitSpinner = rangeBinding.rangeMaxSpinner;
        final UnitTypeSpinnerAdapter maxUnitAdapter = new UnitTypeSpinnerAdapter(this, unitType, textSize);
        maxUnitSpinner.setAdapter(maxUnitAdapter);
        //maxUnitSpinner.setTag(R.integer.key_0, 1); // Min or max
        //maxUnitSpinner.setTag(R.integer.key_1, unitType); // Unit type
        //maxUnitSpinner.setTag(R.integer.key_2, quantityType); // Quantity type

        // Set what happens when the spinners are changed
        final TriConsumer<CharacterProfile, Class<? extends QuantityType>, Unit> minSetter = CharacterProfile::setMinUnit;
        final TriConsumer<CharacterProfile, Class<? extends QuantityType>, Unit> maxSetter = CharacterProfile::setMaxUnit;
        final UnitSpinnerListener minUnitListener = new UnitSpinnerListener(unitType, quantityType, minSetter);
        final UnitSpinnerListener maxUnitListener = new UnitSpinnerListener(unitType, quantityType, maxSetter);

//        final AdapterView.OnItemSelectedListener unitListener = new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//
//                // Null checks
//                if (characterProfile == null || adapterView == null || adapterView.getAdapter() == null) { return; }
//
//                try {
//                    final int tag = (int) adapterView.getTag(R.integer.key_0);
//                    final Class<? extends Unit> unitType = (Class<? extends Unit>) adapterView.getTag(R.integer.key_1);
//                    final Class<? extends QuantityType> quantityType = (Class<? extends QuantityType>) adapterView.getTag(R.integer.key_2);
//                    final Unit unit = unitType.cast(adapterView.getItemAtPosition(i));
//                    switch (tag) {
//                        case 0:
//                            characterProfile.setMinUnit(quantityType, unit);
//                            break;
//                        case 1:
//                            characterProfile.setMaxUnit(quantityType, unit);
//                    }
//                    saveCharacterProfile();
//                    filterOnTablet.run();
//                } catch (NullPointerException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {}
//        };
        minUnitSpinner.setOnItemSelectedListener(minUnitListener);
        maxUnitSpinner.setOnItemSelectedListener(maxUnitListener);

        // Set up the min and max text views
        final EditText minET = rangeBinding.rangeMinEntry;
        minET.setTag(quantityType);
        minET.setFilters( new InputFilter[] { new InputFilter.LengthFilter(maxLength) } );
        minET.setOnFocusChangeListener( (v, hasFocus) -> {
            if (!hasFocus) {
                final Class<? extends QuantityType> type = (Class<? extends QuantityType>) minET.getTag();
                int min;
                try {
                    min = Integer.parseInt(minET.getText().toString());
                } catch (NumberFormatException nfe) {
                    min = CharacterProfile.getDefaultMinValue(type);
                    minET.setText(String.format(Locale.US, "%d", min));
                    final Unit unit = CharacterProfile.getDefaultMinUnit(type);
                    final UnitTypeSpinnerAdapter adapter = (UnitTypeSpinnerAdapter) minUnitSpinner.getAdapter();
                    final List spinnerObjects = Arrays.asList(adapter.getData());
                    minUnitSpinner.setSelection(spinnerObjects.indexOf(unit));
                    characterProfile.setMinUnit(quantityType, unit);
                }
                characterProfile.setMinValue(quantityType, min);
                saveCharacterProfile();
                filterOnTablet.run();
            }
        });
        final EditText maxET = rangeBinding.rangeMaxEntry;
        maxET.setTag(quantityType);
        maxET.setFilters( new InputFilter[] { new InputFilter.LengthFilter(maxLength) } );
        maxET.setOnFocusChangeListener( (v, hasFocus) -> {
            if (!hasFocus) {
                final Class<? extends QuantityType> type = (Class<? extends QuantityType>) maxET.getTag();
                int max;
                try {
                    max = Integer.parseInt(maxET.getText().toString());
                } catch (NumberFormatException nfe) {
                    max = CharacterProfile.getDefaultMaxValue(type);
                    maxET.setText(String.format(Locale.US, "%d", max));
                    final Unit unit = CharacterProfile.getDefaultMaxUnit(type);
                    final UnitTypeSpinnerAdapter adapter = (UnitTypeSpinnerAdapter) maxUnitSpinner.getAdapter();
                    final List spinnerObjects = Arrays.asList(adapter.getData());
                    maxUnitSpinner.setSelection(spinnerObjects.indexOf(unit));
                    characterProfile.setMaxUnit(quantityType, unit);
                }
                characterProfile.setMaxValue(quantityType, max);
                saveCharacterProfile();
                filterOnTablet.run();
            }
        });

        // Set up the restore defaults button
        final Button restoreDefaultsButton = rangeBinding.restoreDefaultsButton;
        restoreDefaultsButton.setTag(quantityType);
        restoreDefaultsButton.setOnClickListener((v) -> {
            final Class<? extends QuantityType> type = (Class<? extends QuantityType>) v.getTag();
            final Unit minUnit = CharacterProfile.getDefaultMinUnit(type);
            final Unit maxUnit = CharacterProfile.getDefaultMaxUnit(type);
            final int minValue = CharacterProfile.getDefaultMinValue(type);
            final int maxValue = CharacterProfile.getDefaultMaxValue(type);
            minET.setText(String.format(Locale.US, "%d", minValue));
            maxET.setText(String.format(Locale.US, "%d", maxValue));
            final UnitTypeSpinnerAdapter adapter = (UnitTypeSpinnerAdapter) minUnitSpinner.getAdapter();
            final List spinnerObjects = Arrays.asList(adapter.getData());
            minUnitSpinner.setSelection(spinnerObjects.indexOf(minUnit));
            maxUnitSpinner.setSelection(spinnerObjects.indexOf(maxUnit));
            characterProfile.setRangeToDefaults(type);
            saveCharacterProfile();
            filterOnTablet.run();
        });

    }

    private void setupYesNoBinding(YesNoFilterViewBinding binding, int titleResourceID, BiFunction<CharacterProfile,Boolean,Boolean> getter, BiConsumer<CharacterProfile,Boolean> toggler) {
        binding.setProfile(characterProfile);
        binding.setTitle(getResources().getString(titleResourceID));
        binding.setStatusGetter(getter);
        binding.executePendingBindings();
        final ToggleButton yesButton = binding.yesOption.optionFilterButton;
        yesButton.setOnClickListener( (v) -> { toggler.accept(characterProfile, true); saveCharacterProfile(); filterOnTablet.run(); });
        final ToggleButton noButton = binding.noOption.optionFilterButton;
        noButton.setOnClickListener( (v) -> { toggler.accept(characterProfile, false); saveCharacterProfile(); filterOnTablet.run(); });
        yesNoBindings.add(binding);
    }

    private void setupRitualConcentrationFilters() {

        // Get the binding
        final RitualConcentrationLayoutBinding ritualConcentrationBinding = sortFilterBinding.ritualConcentrationFilterBlock;

        // Set the title size
        final SortFilterHeaderView headerView = ritualConcentrationBinding.ritualConcentrationFilterHeader;
        final int textSize = onTablet ? 35 : 28;
        headerView.setTitleSize(textSize);

        // Set up the bindings
        setupYesNoBinding(ritualConcentrationBinding.ritualFilter, R.string.ritual_filter_title, CharacterProfile::getRitualFilter, CharacterProfile::toggleRitualFilter);
        setupYesNoBinding(ritualConcentrationBinding.concentrationFilter, R.string.concentration_filter_title, CharacterProfile::getConcentrationFilter, CharacterProfile::toggleConcentrationFilter);

        // Expandability
        expandingViews.put(headerView, ritualConcentrationBinding.ritualConcentrationFlexbox);

    }

    private void setupFilterOptions() {

        // Get the filter options binding
        final FilterOptionsLayoutBinding filterOptionsBinding = sortFilterBinding.filterOptions;

        final BiConsumer<CharacterProfile,Boolean> searchFilterFunction = (cp, b) -> {
            cp.setApplyFiltersToSearch(b);
            final CharSequence query = (searchView != null) ? searchView.getQuery() : "";
            if (query.length() > 0) { filterOnTablet.run(); }
        };

        final BiConsumer<CharacterProfile,Boolean> listFilterFunction = (cp, b) -> {
            cp.setApplyFiltersToSpellLists(b);
            if (cp.getStatusFilter() != StatusFilterField.ALL) { filterOnTablet.run(); }
        };


        // Set up the bindings
        final Map<FilterOptionBinding, BiConsumer<CharacterProfile,Boolean>> bindingsAndFunctions = new HashMap<FilterOptionBinding, BiConsumer<CharacterProfile,Boolean>>() {{
            put(filterOptionsBinding.filterListsLayout, listFilterFunction);
            put(filterOptionsBinding.filterSearchLayout, searchFilterFunction);
            put(filterOptionsBinding.useExpandedLayout, CharacterProfile::setUseTCEExpandedLists);
        }};

        for (Map.Entry<FilterOptionBinding, BiConsumer<CharacterProfile,Boolean>> entry : bindingsAndFunctions.entrySet()) {
            final FilterOptionBinding binding = entry.getKey();
            final BiConsumer<CharacterProfile, Boolean> function = entry.getValue();
            binding.optionChooser.setOnCheckedChangeListener((chooser, isChecked) -> {
                function.accept(characterProfile, isChecked);
                saveCharacterProfile();
                filterOnTablet.run();
            });
            binding.optionInfoButton.setOnClickListener((v) -> {
                openOptionInfoDialog(binding);
            });
        }

        // Expandable header setup
        expandingViews.put(filterOptionsBinding.filterOptionsHeader, filterOptionsBinding.filterOptionsContent);

    }

    private void setupComponentsFilters() {

        // Get the components view binding
        final ComponentsFilterLayoutBinding componentsBinding = sortFilterBinding.componentsFilterBlock;

        // Set up the bindings
        final List<YesNoFilterViewBinding> bindings = Arrays.asList(componentsBinding.verbalFilter, componentsBinding.somaticFilter, componentsBinding.materialFilter);
        final int[] titleIDs = new int[]{ R.string.verbal_filter_title, R.string.somatic_filter_title, R.string.material_filter_title };
        final List<BiConsumer<CharacterProfile,Boolean>> togglers = Arrays.asList(CharacterProfile::toggleVerbalComponentFilter, CharacterProfile::toggleSomaticComponentFilter, CharacterProfile::toggleMaterialComponentFilter);
        final List<BiFunction<CharacterProfile,Boolean,Boolean>> getters = Arrays.asList(CharacterProfile::getVerbalComponentFilter, CharacterProfile::getSomaticComponentFilter, CharacterProfile::getMaterialComponentFilter);
        for (int i = 0; i < titleIDs.length; ++i) {
            setupYesNoBinding(bindings.get(i), titleIDs[i], getters.get(i), togglers.get(i));
        }

        // Expandability
        expandingViews.put(componentsBinding.componentsFilterHeader, componentsBinding.componentsFlexbox);

    }

    private void setupSortFilterView() {

        // Set up the sorting UI elements
        setupSortElements();

        // Set up the filter option elements
        setupFilterOptions();

        // Set up the level filter elements
        setupLevelFilter();

        // Populate the ritual and concentration views
        setupRitualConcentrationFilters();

        // Populate the component filters
        setupComponentsFilters();

        // Populate the filter bindings
        classToBindingsMap.put(Sourcebook.class, populateFeaturedFilters(Sourcebook.class, LocalizationUtils.supportedCoreSourcebooks()));
        classToBindingsMap.put(CasterClass.class, populateFilters(CasterClass.class, LocalizationUtils.supportedClasses()));
        classToBindingsMap.put(School.class, populateFilters(School.class));
        classToBindingsMap.put(CastingTime.CastingTimeType.class, populateFilters(CastingTime.CastingTimeType.class));
        classToBindingsMap.put(Duration.DurationType.class, populateFilters(Duration.DurationType.class));
        classToBindingsMap.put(Range.RangeType.class, populateFilters(Range.RangeType.class));

        // Set up the expanding views
        setupExpandingViews();

    }

    private void setupExpandingViews() {
        for (HashMap.Entry<View,View> entry : expandingViews.entrySet()) {
            ViewAnimations.setExpandableHeader(entry.getKey(), entry.getValue());
        }
    }

    private void setupLevelFilter() {

        final LevelFilterLayoutBinding levelBinding = sortFilterBinding.levelFilterRange;
        expandingViews.put(levelBinding.levelFilterHeader, levelBinding.levelFilterContent);

        // When a number is selected on the min (max) spinner, set the current character profile's min (max) level
        final EditText minLevelET = levelBinding.minLevelEntry;
        minLevelET.setOnFocusChangeListener( (v, hasFocus) -> {
            if (!hasFocus) {
                final TextView tv = (TextView) v;
                int level;
                try {
                    level = Integer.parseInt(tv.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    tv.setText(String.format(Locale.US, "%d", Spellbook.MIN_SPELL_LEVEL));
                    return;
                }
                characterProfile.setMinSpellLevel(level);
                saveCharacterProfile();
                filterOnTablet.run();
            }
        });


        final EditText maxLevelET = levelBinding.maxLevelEntry;
        maxLevelET.setOnFocusChangeListener( (v, hasFocus) -> {
            if (!hasFocus) {
                final TextView tv = (TextView) v;
                int level;
                try {
                    level = Integer.parseInt(tv.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    tv.setText(String.format(Locale.US, "%d", Spellbook.MAX_SPELL_LEVEL));
                    return;
                }
                characterProfile.setMaxSpellLevel(level);
                saveCharacterProfile();
                filterOnTablet.run();
            }
        });

        // When the restore full range button is pressed, set the min and max levels for the profile to the full min and max
        final Button restoreFullButton = levelBinding.fullRangeButton;
        restoreFullButton.setOnClickListener((v) -> {
            sortFilterBinding.levelFilterRange.minLevelEntry.setText(String.format(Locale.US, "%d", Spellbook.MIN_SPELL_LEVEL));
            sortFilterBinding.levelFilterRange.maxLevelEntry.setText(String.format(Locale.US, "%d", Spellbook.MAX_SPELL_LEVEL));
            characterProfile.setMinSpellLevel(Spellbook.MIN_SPELL_LEVEL);
            characterProfile.setMaxSpellLevel(Spellbook.MAX_SPELL_LEVEL);
            saveCharacterProfile();
            filterOnTablet.run();
        });

    }

    private void updateWindowVisibilities() {

        // Clear the focus from an EditText, if that's where it is
        // since they have an OnFocusChangedListener
        // We want to do this BEFORE we sort/filter so that any changes can be made to the CharacterProfile
        if (!filterVisible) {
            final View view = getCurrentFocus();
            if (view != null) {
                hideSoftKeyboard(view, this);
            }
        }

        // Sort and filter, if necessary
        if (!onTablet && !filterVisible) {
            sort();
            filter();
        }

        // The current window visibilities
        int spellVisibility = filterVisible ? View.GONE : View.VISIBLE;
        final int filterVisibility = filterVisible ? View.VISIBLE : View.GONE;
        if (onTablet && spellWindowBinding.getSpell() == null) {
            spellVisibility = View.GONE;
        }

        // Update window visibilities appropriately
        final View spellView = onTablet ? spellWindowCL : spellsCL;
        spellView.setVisibility(spellVisibility);
        filterSV.setVisibility(filterVisibility);

        // Collapse the SearchView if it's open, and set the search icon visibility appropriately
        if (filterVisible && (searchView != null) && !searchView.isIconified()) {
            searchViewIcon.collapseActionView();
        }
        if (!onTablet && searchViewIcon != null) {
            searchViewIcon.setVisible(spellVisibility == View.VISIBLE);
        }

        // Update the filter icon on the action bar
        // If the filters are open, we show a list or data icon (depending on the platform) instead ("return to the data")
        if (filterMenuIcon != null) {
            final int filterIcon = onTablet ? R.drawable.ic_data : R.drawable.ic_list;
            final int icon = filterVisible ? filterIcon : R.drawable.ic_filter;
            filterMenuIcon.setIcon(icon);
        }

        // Save the character profile
        saveCharacterProfile();

//        if (spellWindowBinding.getSpell() != null) {
//            System.out.println("binding spell is " + spellWindowBinding.getSpell().getName());
//        } else {
//            System.out.println("binding spell is null");
//        }
    }

    private void toggleWindowVisibilities() {
        filterVisible = !filterVisible;
        updateWindowVisibilities();
    }

    private String stringFromID(int stringID) { return getResources().getString(stringID); }

    private float dimensionFromID(int dimensionID) { return getResources().getDimension(dimensionID); }

    private void openPlayStoreForRating() {
        final Uri uri = Uri.parse("market://details?id=" + getPackageName());
        final Intent goToPlayStore = new Intent(Intent.ACTION_VIEW, uri);
        goToPlayStore.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToPlayStore);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void openSpellCreationWindow() {
        final Intent intent = new Intent(MainActivity.this, SpellCreationActivity.class);
        startActivityForResult(intent, RequestCodes.SPELL_CREATION_REQUEST);
        overridePendingTransition(R.anim.identity, android.R.anim.slide_in_left);
    }

    private File createFileDirectory(String directoryName) {
        File directory = new File(getApplicationContext().getFilesDir(), directoryName);
        if ( !(directory.exists() && directory.isDirectory()) ) {
            final boolean success = directory.mkdir();
            if (!success) {
                Log.v(TAG, "Error creating directory: " + directory); // Add something real here eventually
            }
        }
        return directory;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            if (! (view instanceof EditText)) {
                view.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    class UnitSpinnerListener<Q extends QuantityType, U extends Unit> implements AdapterView.OnItemSelectedListener {

        private final Class<U> unitType;
        private final Class<Q> quantityType;
        private TriConsumer<CharacterProfile, Class<? extends QuantityType>, Unit> setter;

        UnitSpinnerListener(Class<U> unitType, Class<Q> quantityType, TriConsumer<CharacterProfile, Class<? extends QuantityType>, Unit> setter) {
            this.unitType = unitType;
            this.quantityType = quantityType;
            this.setter = setter;
        }

        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            // Get the character profile
            final CharacterProfile profile = MainActivity.this.characterProfile;

            // Null checks
            if (profile == null || adapterView == null || adapterView.getAdapter() == null) { return; }

            // Set the appropriate unit in the character profile
            final U unit = unitType.cast(adapterView.getItemAtPosition(i));
            setter.accept(profile, quantityType, unit);
            MainActivity.this.saveCharacterProfile();
            filterOnTablet.run();

        }

        public void onNothingSelected(AdapterView<?> adapterView) {}
    }

    private void showUpdateDialog(boolean checkIfNecessary) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String key = "first_time_" + GlobalInfo.VERSION_CODE;
        final boolean toShow = !checkIfNecessary || (!prefs.contains(key) && charactersList().size() > 0);
        if (toShow) {
            final Runnable onDismissAction = () -> {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(key, true).apply();
            };
            SpellbookUtils.showMessageDialog(this, updateDialogTitleID, updateDialogDescriptionID, false, onDismissAction);
        } else if (checkIfNecessary) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key, true).apply();
        }
    }


}
