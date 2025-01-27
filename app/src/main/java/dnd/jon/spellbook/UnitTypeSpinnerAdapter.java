package dnd.jon.spellbook;

import android.content.Context;

class UnitTypeSpinnerAdapter<T extends Enum<T> & Unit> extends NamedSpinnerAdapter<T> {
    UnitTypeSpinnerAdapter(Context context, Class<T> type, int textSize) { super(context, type, DisplayUtils::getPluralName, textSize); }
    UnitTypeSpinnerAdapter(Context context, Class<T> type) { super(context, type, DisplayUtils::getPluralName); }
}
