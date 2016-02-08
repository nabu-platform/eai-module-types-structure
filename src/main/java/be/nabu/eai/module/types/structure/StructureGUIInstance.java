package be.nabu.eai.module.types.structure;

import java.io.IOException;
import java.util.List;

import javafx.scene.layout.AnchorPane;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.RefresheableArtifactGUIInstance;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.Validation;

public class StructureGUIInstance implements RefresheableArtifactGUIInstance {

	private DefinedStructure structure;
	private Entry entry;
	private boolean changed;
	private StructureGUIManager manager;
	
	public StructureGUIInstance(StructureGUIManager manager) {
		// delayed
		this.manager = manager;
	}
	
	public StructureGUIInstance(StructureGUIManager manager, Entry entry, DefinedStructure structure) {
		this.manager = manager;
		this.entry = entry;
		this.structure = structure;
	}
	
	@Override
	public String getId() {
		return entry.getId();
	}

	@Override
	public List<Validation<?>> save() throws IOException {
		return new StructureManager().save((RepositoryEntry) entry, structure);
	}

	@Override
	public boolean isReady() {
		return entry != null && structure != null;
	}
	
	@Override
	public boolean hasChanged() {
		return changed;
	}

	public DefinedStructure getStructure() {
		return structure;
	}

	public void setStructure(DefinedStructure structure) {
		this.structure = structure;
	}

	public Entry getEntry() {
		return entry;
	}

	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	@Override
	public boolean isEditable() {
		return entry.isEditable();
	}

	@Override
	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	@Override
	public void refresh(AnchorPane pane) {
		entry.refresh(true);
		try {
			this.structure = manager.display(MainController.getInstance(), pane, entry);
		}
		catch (Exception e) {
			throw new RuntimeException("Could not refresh: " + getId(), e);
		}
	}
}
