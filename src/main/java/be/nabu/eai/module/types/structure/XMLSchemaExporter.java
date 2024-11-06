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

import java.util.LinkedHashSet;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.repository.api.Entry;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.definition.xsd.XSDDefinitionMarshaller;
import be.nabu.libs.types.properties.AttributeQualifiedDefaultProperty;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;

public class XMLSchemaExporter implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(final Entry entry) {
		if (entry.isNode() && ComplexType.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
			MenuItem item = new MenuItem("Export as XML Schema");
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					try {
						ComplexType complex = (ComplexType) entry.getNode().getArtifact();
						final XSDDefinitionMarshaller marshaller = new XSDDefinitionMarshaller();
						Boolean value = ValueUtils.getValue(ElementQualifiedDefaultProperty.getInstance(), complex.getProperties());
						if (value != null && value) {
							marshaller.setIsElementQualified(true);
						}
						value = ValueUtils.getValue(AttributeQualifiedDefaultProperty.getInstance(), complex.getProperties());
						if (value != null && value) {
							marshaller.setIsAttributeQualified(true);
						}
						
						marshaller.define(complex);
						
						SimpleProperty<String> simpleProperty = new SimpleProperty<String>("Root Element Name", String.class, true);
						Set<Property<?>> properties = new LinkedHashSet<Property<?>>();
						properties.add(simpleProperty);
						final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties, new ValueImpl<String>(simpleProperty, entry.getId().replaceAll("^.*\\.([^.]+)$", "$1")));
						EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Export as XML Schema", new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								String name = updater.getValue("Root Element Name");
								if (name != null && !name.isEmpty()) {
									marshaller.define(new ComplexElementImpl(name, complex, null));
								}
								Confirm.confirm(ConfirmType.INFORMATION, "XML Schema", GenerateXSDMenuEntry.stringify(marshaller), null);
							}
						});
						
						
//						ByteArrayOutputStream output = new ByteArrayOutputStream();
//						marshaller.marshal(output, complex);
//						Confirm.confirm(ConfirmType.INFORMATION, "XML Schema: " + entry.getId(), new String(output.toByteArray(), "UTF-8"), null);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
			return item;
		}
		return null;
	}
	
}
