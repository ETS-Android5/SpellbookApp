package dnd.jon.spellbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.Spinner;

import org.json.JSONArray;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

class SpellbookUtils {

    static final ArrayList<Character> illegalCharacters = new ArrayList<>(Arrays.asList('\\', '/', '.'));

    static <T> T coalesce(@Nullable T one, @NonNull T two) {
        return one != null ? one : two;
    }


    static boolean yn_to_bool(String yn) throws Exception {
        if (yn.equals("no")) {
            return false;
        } else if (yn.equals("yes")) {
            return true;
        } else {
            throw new Exception("String must be yes or no");
        }
    }

    static String bool_to_yn(boolean yn) {
        if (yn) {
            return "yes";
        } else {
            return "no";
        }
    }

    static int parseFromString(final String s, final int defaultValue) {
        int x;
        try {
            x = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            x = defaultValue;
        }
        return x;
    }

    static <T> T[] jsonToArray(JSONArray jarr, Class<T> elementType, BiFunction<JSONArray,Integer,T> itemGetter) {
        final T[] arr = (T[]) Array.newInstance(elementType, jarr.length());
        for (int i = 0; i < jarr.length(); ++i) {
            arr[i] = itemGetter.apply(jarr, i);
        }
        return arr;
    }

//    static String firstLetterCapitalized(String s) {
//        return s.substring(0,1).toUpperCase() + s.substring(1);
//    }

    static <T extends Enum<T>> void setNamedSpinnerByItem(Spinner spinner, T item) {
        try {
            final NamedSpinnerAdapter<T> adapter = (NamedSpinnerAdapter<T>) spinner.getAdapter();
            spinner.setSelection(adapter.itemIndex(item));
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

   static void clickButtons(Collection<ToggleButton> buttons, Function<ToggleButton,Boolean> filter) {
        if (buttons == null) { return; }
        for (ToggleButton tb : buttons) {
            if (filter.apply(tb)) {
                tb.callOnClick();
            }
        }
    }
}
