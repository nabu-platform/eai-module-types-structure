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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.structure.StructureGenerator;

public class GenerateJSONContextMenu implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(Entry entry) {
		if (!entry.isLeaf() && !entry.isNode()) {
			Menu menu = new Menu("Generate Model");
			
			MenuItem item = new MenuItem("From JSON");
			
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					Set<Property<?>> properties = new LinkedHashSet<Property<?>>();
					properties.add(new SimpleProperty<String>("Name", String.class, true));
					SimpleProperty<String> content = new SimpleProperty<String>("JSON", String.class, true);
					content.setLarge(true);
					properties.add(content);
					final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
					EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Generate structure from JSON", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							String name = updater.getValue("Name");
							String json = updater.getValue("JSON");
							if (name != null && json != null) {
								JSONBinding binding = new JSONBinding(new StructureGenerator(), Charset.defaultCharset());
								binding.setAddDynamicElementDefinitions(true);
								binding.setAllowDynamicElements(true);
								binding.setParseNumbers(true);
								binding.setIgnoreRootIfArrayWrapper(true);
								try {
									ComplexContent unmarshal = binding.unmarshal(new ByteArrayInputStream(json.getBytes()), new Window[0]);
									StructureManager manager = new StructureManager();
									RepositoryEntry repositoryEntry = ((RepositoryEntry) entry).createNode(name, manager, true);
									manager.saveContent(repositoryEntry, unmarshal.getType());
									MainController.getInstance().getRepositoryBrowser().refresh();
								}
								catch (Exception e) {
									MainController.getInstance().notify(e);
								}
							}
						}
					});
				}
			});
			
			menu.getItems().add(item);
			return menu;
		}
		return null;
	}

}
