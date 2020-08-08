package dnd.jon.spellbook;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import dnd.jon.spellbook.databinding.RightNavHeaderBinding;
import dnd.jon.spellbook.databinding.RightNavSubmenuBinding;

public class CreatedItemsAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<Source> createdSources = new ArrayList<>();
    private List<List<Spell>> createdSpellsBySource = new ArrayList<>();

    CreatedItemsAdapter(Context context) {
        this.context = context;
    }

    void setData(List<Source> createdSources, List<List<Spell>> createdSpellsBySource) {
        this.createdSources = createdSources;
        this.createdSpellsBySource = createdSpellsBySource;
        notifyDataSetChanged();
    }

    @Override public int getGroupCount() { return createdSources.size(); }
    @Override public int getChildrenCount(int groupPosition) { return createdSpellsBySource.get(groupPosition).size(); }
    @Override public Object getGroup(int groupPosition) {return createdSources.get(groupPosition); }
    @Override public Object getChild(int groupPosition, int childPosition) { return createdSpellsBySource.get(groupPosition).get(childPosition); }
    @Override public long getGroupId(int groupPosition) { return groupPosition; }
    @Override public long getChildId(int groupPosition, int childPosition) { return childPosition; }
    @Override public boolean hasStableIds() { return false; }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final RightNavHeaderBinding binding = RightNavHeaderBinding.inflate(inflater);
            convertView = binding.getRoot();
        }
        TextView header = convertView.findViewById(R.id.header);
        header.setText(headerTitle);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final RightNavSubmenuBinding binding = RightNavSubmenuBinding.inflate(inflater);
            convertView = binding.getRoot();
        }
        TextView childTV = convertView.findViewById(R.id.submenu);
        childTV.setText(childText);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }
}
