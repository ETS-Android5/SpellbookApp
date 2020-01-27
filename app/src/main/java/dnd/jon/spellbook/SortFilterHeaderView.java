package dnd.jon.spellbook;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;

public class SortFilterHeaderView extends ConstraintLayout {

    private AppCompatTextView titleView;
    private ToggleButton button;
    private final Runnable runWhenClicked = () -> { button.toggle(); };

    // Constructors
    // First constructor is public so that it can be used via XML
    public SortFilterHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SortFilterHeaderView, 0, 0);
        final String title = Util.coalesce(a.getString(R.styleable.SortFilterHeaderView_title), "");
        a.recycle();

        // Create the title view and the button, and add them as subviews
        titleView = new AppCompatTextView(context);
        button = new ToggleButton(context, R.drawable.ic_add, R.drawable.ic_remove, false);
        titleView.setId(View.generateViewId());
        button.setId(View.generateViewId());
        addView(titleView);
        addView(button);


        // Title view setup
        Typeface typeface = ResourcesCompat.getFont(context, R.font.cloister_black);
        titleView.setTypeface(typeface);
        titleView.setTextSize(context.getResources().getDimension(R.dimen.sort_header_text_size));
        titleView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        titleView.setText(title);

        // Button setup
        button.setClickable(false);
        button.setBackground(null);

        // Setting up the constraints
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);
        constraintSet.connect(titleView.getId(), ConstraintSet.START, this.getId(), ConstraintSet.START, 0);
        constraintSet.connect(titleView.getId(), ConstraintSet.END, this.getId(), ConstraintSet.END, 0);
        constraintSet.connect(titleView.getId(), ConstraintSet.TOP, this.getId(), ConstraintSet.TOP, 0);
        constraintSet.connect(button.getId(), ConstraintSet.END, this.getId(), ConstraintSet.END, 0);
        constraintSet.connect(button.getId(), ConstraintSet.TOP, this.getId(), ConstraintSet.TOP, 0);
        constraintSet.connect(button.getId(), ConstraintSet.BOTTOM, this.getId(), ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo(this);

        // Apply the constraints
        constraintSet.applyTo(this);

    }

    void setTitle(String title) {
        System.out.println("Setting title to " + title);
        titleView.setText(title);
    }

    Runnable onClickRunnable() { return runWhenClicked; }

}
