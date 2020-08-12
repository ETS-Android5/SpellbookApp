package dnd.jon.spellbook;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.io.File;

@Database(entities = {Spell.class, Source.class, CharacterProfile.class, SpellListEntry.class, SourceListEntry.class}, version = 1, exportSchema = true)
@TypeConverters({SpellbookTypeConverters.class})
public abstract class SpellbookRoomDatabase extends RoomDatabase {

    private static SpellbookRoomDatabase INSTANCE;
    private static final String DB_NAME = "spellbook";
    private static final String DB_ASSET_FILE = "spellbook.db";
    private static final String DB_DIR = "databases";
    private static final File dbPath = new File(DB_DIR, DB_NAME);

    public abstract SpellDao spellDao();
    public abstract SourceDao sourceDao();
    public abstract CharacterDao characterDao();
    public abstract SpellListDao spellListDao();
    public abstract SourceListDao sourceListDao();

    public static SpellbookRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SpellRoomDatabase.class) {
                if (INSTANCE == null) {

                    createDatabaseFileIfNeeded(context); // Create the database if we need to

                    // Then build it from the file
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), SpellbookRoomDatabase.class, DB_NAME).allowMainThreadQueries().createFromFile(dbPath).build();
                }
            }
        }
        return INSTANCE;
    }

    // If the database file doesn't already exist
    private static void createDatabaseFileIfNeeded(Context context) {
        if ( !(dbPath.exists() && dbPath.isFile()) ) {
            System.out.println("Copying spellbook DB from assets to " + dbPath.getPath());
            AndroidUtils.copyAssetToData(context, DB_ASSET_FILE, dbPath.getPath());
        }
    }

}
