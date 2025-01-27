package dnd.jon.spellbook;

import androidx.databinding.BindingAdapter;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.widget.TextView;

public class BindingAdapterUtils {

    @BindingAdapter({"promptText", "otherText"})
    public static void promptFormat(TextView tv, String promptText, String otherText) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(promptText + ": " + otherText);
        ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, promptText.length()+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(ssb);
    }

    @BindingAdapter({"context", "level", "schoolName", "ritual"})
    public static void schoolLevelText(TextView tv, Context context, int level, String schoolName, boolean ritual) {

        // Handle cantrips
        if (level == 0) {
            String text = context.getString(R.string.school_cantrip, schoolName);
            text = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
            tv.setText(text);
            return;
        }

        // Handle higher-level spells
        String ordinal = level + DisplayUtils.ordinalString(context, level);
        String text = context.getString(R.string.ordinal_school, ordinal, schoolName.toLowerCase());
        if (ritual) {
            text = text + " (ritual)";
        }
        tv.setText(text);
    }

    @BindingAdapter("set")
    public static void setToggleButton(ToggleButton button, boolean set) {
        button.set(set);
    }

}
