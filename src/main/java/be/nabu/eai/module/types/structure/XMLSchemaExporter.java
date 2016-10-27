package be.nabu.eai.module.types.structure;

import java.io.ByteArrayOutputStream;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.repository.api.Entry;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.types.api.ComplexType;
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
						XSDDefinitionMarshaller marshaller = new XSDDefinitionMarshaller();
						Boolean value = ValueUtils.getValue(ElementQualifiedDefaultProperty.getInstance(), complex.getProperties());
						if (value != null && value) {
							marshaller.setIsElementQualified(true);
						}
						value = ValueUtils.getValue(AttributeQualifiedDefaultProperty.getInstance(), complex.getProperties());
						if (value != null && value) {
							marshaller.setIsAttributeQualified(true);
						}
						ByteArrayOutputStream output = new ByteArrayOutputStream();
						marshaller.marshal(output, complex);
						Confirm.confirm(ConfirmType.INFORMATION, "XML Schema: " + entry.getId(), new String(output.toByteArray(), "UTF-8"), null);
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
