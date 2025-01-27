package dnd.jon.spellbook;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.function.BiFunction;

class NamedSpinnerAdapter<T extends Enum<T>> extends ArrayAdapter<T> {

    // Static member values
    private static final int layoutID = R.layout.spinner_item;
    private static final int labelID = R.id.spinner_row_text_view;
    private static String[] objects = null;

    // Member values
    private final Context context;
    private final Class<T> type;
    private final BiFunction<Context,T,String> namingFunction;
    private final int textSize;

    NamedSpinnerAdapter(Context context, Class<T> type, BiFunction<Context,T,String> namingFunction, int textSize) {
        super(context, layoutID, type.getEnumConstants());
        this.context = context;
        this.type = type;
        this.namingFunction = namingFunction;
        this.textSize = textSize;
    }

    NamedSpinnerAdapter(Context context, Class<T> type, BiFunction<Context,T,String> namingFunction) { this(context, type, namingFunction,12); }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getSpinnerRow(position, parent);
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        notifyDataSetChanged();
        return getSpinnerRow(position, parent);
    }

    private View getSpinnerRow(int position, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(layoutID, parent, false);
        TextView label = row.findViewById(labelID);
        label.setText(namingFunction.apply(context, getItem(position)));
        if (textSize > 0) { label.setTextSize(textSize); }
        label.setGravity(Gravity.CENTER);
        return row;
    }

    int itemIndex(T item) {
        final String itemName = namingFunction.apply(context, item);
        final int index = Arrays.asList(objects).indexOf(itemName);
        return (index == -1) ? index : 0;
    }

    String[] getNames() {
        if (objects == null) {
            objects = DisplayUtils.getDisplayNames(context, type, namingFunction);
        }
        return (objects == null) ? new String[0] : Arrays.copyOf(objects, objects.length);
    }

    T[] getData() { return type.getEnumConstants(); }


}
