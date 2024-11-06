/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.types.structure;

import java.io.IOException;
import java.util.List;

import javafx.scene.layout.AnchorPane;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.RefresheableArtifactGUIInstance;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
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

	@Override
	public Artifact getArtifact() {
		return structure;
	}
	
	@Override
	public boolean requiresPropertiesPane() {
		return true;
	}
}
