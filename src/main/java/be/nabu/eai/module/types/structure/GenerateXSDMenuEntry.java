package be.nabu.eai.module.types.structure;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.MainMenuEntry;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeRegistryImpl;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.ModifiableTypeRegistry;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.definition.xsd.XSDDefinitionMarshaller;
import be.nabu.libs.types.properties.FormatProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class GenerateXSDMenuEntry implements MainMenuEntry {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void populate(MenuBar menuBar) {
		Menu findOrCreate = EAIDeveloperUtils.findOrCreate(menuBar, "Types");
		MenuItem item = new MenuItem("Generate XSD from XML");
		findOrCreate.getItems().add(item);
		
		item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				SimpleProperty<String> simpleProperty = new SimpleProperty<String>("XML", String.class, true);
				simpleProperty.setLarge(true);
				Set properties = new LinkedHashSet(Arrays.asList(new Property [] {
					simpleProperty
				}));
				final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
				
				EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Generate XSD from XML", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						String content = updater.getValue("XML");
						if (content != null) {
							Document document = toDocument(new ByteArrayInputStream(content.getBytes(Charset.defaultCharset())));
							Structure root = new Structure();
							root.setName(document.getDocumentElement().getLocalName());
							root.setNamespace(document.getDocumentElement().getNamespaceURI());
							TypeRegistryImpl registry = new TypeRegistryImpl();
							registry.register(root);
							enrich(document.getDocumentElement(), root, registry);

							XSDDefinitionMarshaller marshaller = new XSDDefinitionMarshaller();
							marshaller.setIncludeSchemaLocation(false);
							marshaller.setIsElementQualified(true);
							marshaller.setIsAttributeQualified(false);
							for (String namespace : registry.getNamespaces()) {
								for (be.nabu.libs.types.api.Element<?> element : registry.getElements(namespace)) {
									marshaller.define(element);
								}
								for (SimpleType<?> simpleType : registry.getSimpleTypes(namespace)) {
									marshaller.define(simpleType);
								}
								for (ComplexType complexType : registry.getComplexTypes(namespace)) {
									marshaller.define(complexType);
								}
							}
							Confirm.confirm(ConfirmType.INFORMATION, "XML Schema", stringify(marshaller), null);
						}
					}
				}, false);
			}
		});
	}
	
	public static String stringify(XSDDefinitionMarshaller marshaller) {
		List<Document> documents = new ArrayList<Document>();
		if (marshaller.getSchema() != null) {
			documents.add(marshaller.getSchema());
		}
		documents.addAll(marshaller.getAttachments().values());
		StringBuilder builder = new StringBuilder();
		for (Document child : documents) {
			builder.append(stringify(child, true, true)).append("\n\n");
		}
		return builder.toString();
	}
	
	private static void enrich(Element element, Structure current, ModifiableTypeRegistry registry) {
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node item = attributes.item(i);
			current.add(guessSimpleType("@" + item.getNodeName(), item.getTextContent(), current));
		}
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) item;
				be.nabu.libs.types.api.Element<?> currentElement = current.get(child.getLocalName());
				if (currentElement == null) {
					if (hasElementChildren(child)) {
						Structure childStructure = new Structure();
						childStructure.setName(child.getLocalName());
						childStructure.setNamespace(child.getNamespaceURI());
						enrich(child, childStructure, registry);
						registry.register(childStructure);
						current.add(new ComplexElementImpl(childStructure.getName(), childStructure, current));
					}
					else {
						current.add(guessSimpleType(child, current));
					}
				}
				else {
					// make sure it's a list
					currentElement.setProperty(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0));
					// make sure we get all the fields for complex types
					if (currentElement.getType() instanceof Structure) {
						enrich(child, (Structure) currentElement.getType(), registry);
					}
				}
			}
		}
	}
	
	private static be.nabu.libs.types.api.Element<?> guessSimpleType(Element element, Structure parent) {
		return guessSimpleType(element.getLocalName(), element.getTextContent(), parent);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static be.nabu.libs.types.api.Element<?> guessSimpleType(String name, String textContent, Structure parent) {
		Class<?> clazz;
		List<Value<?>> values = new ArrayList<Value<?>>();
		if (textContent == null || textContent.trim().isEmpty()) {
			clazz = String.class;
		}
		else if (textContent.matches("[0-9]+")) {
			clazz = Long.class;
		}
		else if (textContent.matches("[0-9.]+")) {
			clazz = Double.class;
		}
		else if (textContent.equalsIgnoreCase("true") || textContent.equalsIgnoreCase("false")) {
			clazz = Boolean.class;
		}
		else if (textContent.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
			clazz = Date.class;
			values.add(new ValueImpl<String>(FormatProperty.getInstance(), "date"));
		}
		else if (textContent.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[0-9.]*")) {
			clazz = Date.class;
			values.add(new ValueImpl<String>(FormatProperty.getInstance(), "dateTime"));
		}
		else {
			clazz = String.class;
		}
		return new SimpleElementImpl(name, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(clazz), parent, values.toArray(new Value[values.size()]));
	}
	
	private static boolean hasElementChildren(Element element) {
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				return true;
			}
		}
		return false;
	}
	
	public static Document toDocument(InputStream input) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// no DTD
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// allow no external access, as defined http://docs.oracle.com/javase/7/docs/api/javax/xml/XMLConstants.html#FEATURE_SECURE_PROCESSING an empty string means no protocols are allowed
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			return factory.newDocumentBuilder().parse(input);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String stringify(Node node, boolean omitXMLDeclaration, boolean prettyPrint) {
		try {
	        StringWriter string = new StringWriter();
	        TransformerFactory factory = TransformerFactory.newInstance();
	        Transformer transformer = factory.newTransformer();
	        if (omitXMLDeclaration) {
	        	transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        }
	        if (prettyPrint) {
	        	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        }
	        transformer.transform(new DOMSource(node), new StreamResult(string));
	        return string.toString();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
