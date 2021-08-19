package dnd.jon.spellbook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dnd.jon.spellbook.databinding.CharacterRowBinding;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterRowHolder>{

    // Member values
    private List<String> characterNames;
    private final FragmentActivity activity;
    private final CharacterProfileViewModel viewModel;

    // Constructor
    CharacterAdapter(FragmentActivity activity) {
        this.activity = activity;
        this.viewModel = new ViewModelProvider(activity, activity.getDefaultViewModelProviderFactory())
                .get(CharacterProfileViewModel.class);
        viewModel.getCharacterNames().observe(activity, (names) -> {
            this.characterNames = names;
            notifyDataSetChanged();
        });
        this.characterNames = viewModel.getCharacterNames().getValue();
    }

    // ViewHolder methods
    @NonNull
    public CharacterRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final CharacterRowBinding binding = CharacterRowBinding.inflate(inflater, parent, false);
        return new CharacterRowHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterAdapter.CharacterRowHolder holder, int position) {

        // Do nothing if the index is out of bounds
        // This shouldn't happen, but it's better than a crash
        if ( (position >= characterNames.size()) || (position < 0) ) { return; }

        // Get the appropriate spell and bind it to the holder
        final String name = characterNames.get(position);
        holder.bind(name);
    }

    public int getItemCount() { return characterNames.size(); }


    // The RowHolder class
    public class CharacterRowHolder extends ItemViewHolder<String, CharacterRowBinding> {

        public CharacterRowHolder(CharacterRowBinding b) {
            super(b, CharacterRowBinding::setName);
            itemView.setTag(this);
        }

        public void bind(String name) {
            super.bind(name);

            // Set the buttons to have the appropriate effect
            if (name != null) {

                // Set the listener for the options button
                binding.optionsButton.setOnClickListener((v) -> {
                    final PopupMenu popupMenu = new PopupMenu(activity, binding.optionsButton);
                    popupMenu.inflate(R.menu.character_options_menu);
                    popupMenu.setOnMenuItemClickListener((menuItem) -> {
                        final int itemID = menuItem.getItemId();
                        if (itemID == R.id.character_options_rename) {
                            final Bundle args = new Bundle();
                            args.putString(NameChangeDialog.nameKey, binding.getName());
                            final NameChangeDialog dialog = new NameChangeDialog();
                            dialog.setArguments(args);
                            dialog.show(activity.getSupportFragmentManager(), "changeCharacterName");
                        } else if (itemID == R.id.character_options_duplicate) {
                            final Bundle args = new Bundle();
                            args.putParcelable(CreateCharacterDialog.PROFILE_KEY, CharacterProfileUtils.getProfileByName(activity, binding.getName()));
                            final CreateCharacterDialog dialog = new CreateCharacterDialog();
                            dialog.setArguments(args);
                            dialog.show(activity.getSupportFragmentManager(), "duplicateCharacter");
                        } else if (itemID == R.id.character_options_delete) {
                            final Bundle args = new Bundle();
                            args.putString(DeleteCharacterDialog.nameKey, binding.getName());
                            final DeleteCharacterDialog dialog = new DeleteCharacterDialog();
                            dialog.setArguments(args);
                            dialog.show(activity.getSupportFragmentManager(), "confirmDeleteCharacter");
                        } else if (itemID == R.id.character_options_export) {
                            // TODO: implement this
                        }
                        return false;
                    });
                    popupMenu.show();
                });

//                // Set the listener for the delete button
//                binding.deleteButton.setOnClickListener((v) -> {
//                    final Bundle args = new Bundle();
//                    args.putString(DeleteCharacterDialog.nameKey, binding.getName());
//                    final DeleteCharacterDialog dialog = new DeleteCharacterDialog();
//                    dialog.setArguments(args);
//                    dialog.show(main.getSupportFragmentManager(), "confirmDeleteCharacter");
//                });
//
//                // Set the listener for the edit button
//                binding.editButton.setOnClickListener((v) -> {
//                    final Bundle args = new Bundle();
//                    args.putString(NameChangeDialog.nameKey, binding.getName());
//                    final NameChangeDialog dialog = new NameChangeDialog();
//                    dialog.setArguments(args);
//                    dialog.show(main.getSupportFragmentManager(), "changeCharacterName");
//                });

                // Set the listener for the label
                binding.nameLabel.setOnClickListener((v) -> {
                    final String charName = binding.getName();
                    viewModel.setProfileByName(charName);

                    // Show a Toast message after selection
                    Toast.makeText(activity, activity.getString(R.string.character_selected_toast, charName), Toast.LENGTH_SHORT).show();
                });
            }
        }

    }

}
