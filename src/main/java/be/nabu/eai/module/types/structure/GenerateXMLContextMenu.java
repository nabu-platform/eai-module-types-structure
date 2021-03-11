package be.nabu.eai.module.types.structure;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;

import org.w3c.dom.Document;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.types.TypeRegistryImpl;
import be.nabu.libs.types.structure.Structure;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class GenerateXMLContextMenu implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(Entry entry) {
		if (!entry.isLeaf() && !entry.isNode()) {
			Menu menu = new Menu("Generate Model");
			
			MenuItem item = new MenuItem("From XML");
			
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					Set<Property<?>> properties = new LinkedHashSet<Property<?>>();
					properties.add(new SimpleProperty<String>("Name", String.class, true));
					SimpleProperty<String> content = new SimpleProperty<String>("XML", String.class, true);
					content.setLarge(true);
					properties.add(content);
					final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
					EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Generate structure from XML", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							String name = updater.getValue("Name");
							String xml = updater.getValue("XML");
							if (name != null && xml != null) {
								Document document = GenerateXSDMenuEntry.toDocument(new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset())));
								Structure root = new Structure();
								root.setName(EAIRepositoryUtils.stringToField(document.getDocumentElement().getLocalName()));
								root.setNamespace(document.getDocumentElement().getNamespaceURI());
								TypeRegistryImpl registry = new TypeRegistryImpl();
								registry.register(root);
								GenerateXSDMenuEntry.enrich(document.getDocumentElement(), root, registry);
								try {
									StructureManager manager = new StructureManager();
									RepositoryEntry repositoryEntry = ((RepositoryEntry) entry).createNode(name, manager, true);
									manager.saveContent(repositoryEntry, root);
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
