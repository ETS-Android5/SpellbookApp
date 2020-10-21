package dnd.jon.spellbook;

import android.content.Context;

class NameDisplayableSpinnerAdapter<T extends Enum<T> & NameDisplayable> extends NamedSpinnerAdapter<T> {
    NameDisplayableSpinnerAdapter(Context context, Class<T> type, int textSize) { super(context, type, DisplayNameUtils::getDisplayName, textSize); }
    NameDisplayableSpinnerAdapter(Context context, Class<T> type) { super(context, type, DisplayNameUtils::getDisplayName); }
}
