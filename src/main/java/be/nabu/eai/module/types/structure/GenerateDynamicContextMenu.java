package be.nabu.eai.module.types.structure;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.eai.api.NamingConvention;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.managers.util.EnumeratedSimpleProperty;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.api.ModifiableElement;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.AliasProperty;
import be.nabu.libs.types.properties.CollectionCrudProviderProperty;
import be.nabu.libs.types.properties.CollectionNameProperty;
import be.nabu.libs.types.properties.FormatProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.PrimaryKeyProperty;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class GenerateDynamicContextMenu implements EntryContextMenuProvider {
	
	private static Map<String, ModelGenerator> modelGenerators;
	
	public static Map<String, ModelGenerator> getModelGenerators() {
		if (modelGenerators == null) {
			synchronized(GenerateDynamicContextMenu.class) {
				if (modelGenerators == null) {
					Map<String, ModelGenerator> modelGenerators = new HashMap<String, ModelGenerator>();
					MethodServiceInterface ifaceModels = MethodServiceInterface.wrap(ModelGenerator.class, "getModels");
					MethodServiceInterface ifaceFields = MethodServiceInterface.wrap(ModelGenerator.class, "getFields");

					Map<String, DefinedService> modelServices = new HashMap<String, DefinedService>();
					Map<String, DefinedService> fieldServices = new HashMap<String, DefinedService>();
					
					for (DefinedService service : EAIResourceRepository.getInstance().getArtifacts(DefinedService.class)) {
						String name = service.getId();
						Node node = EAIResourceRepository.getInstance().getNode(service.getId());
						if (node != null && node.getProperties() != null && node.getProperties().containsKey("name")) {
							name = node.getProperties().get("name");
						}
						if (POJOUtils.isImplementation(service, ifaceModels)) {
							modelServices.put(name, service);
						}
						else if (POJOUtils.isImplementation(service, ifaceFields)) {
							fieldServices.put(name, service);
						}
					}
					// we need to figure out if there is a field lister as well
					for (Map.Entry<String, DefinedService> entry : modelServices.entrySet()) {
						String name = entry.getKey();
						ModelGenerator generator;
						if (fieldServices.containsKey(name)) {
							DefinedService fieldService = fieldServices.get(name);
							generator = POJOUtils.newProxy(ModelGenerator.class, EAIResourceRepository.getInstance(), SystemPrincipal.ROOT, EAIResourceRepository.getInstance().getServiceRunner(), entry.getValue(), fieldService);
						}
						else {
							generator = POJOUtils.newProxy(ModelGenerator.class, EAIResourceRepository.getInstance(), SystemPrincipal.ROOT, EAIResourceRepository.getInstance().getServiceRunner(), entry.getValue());
						}
						modelGenerators.put(name, generator);
					}
					GenerateDynamicContextMenu.modelGenerators = modelGenerators;
				}
			}
		}
		return modelGenerators;
	}
	
	@XmlRootElement
	public static class ModelGenerationInformation {
		// the service used to generate
		private String generatorId;
		// the id of this model given by the generator
		private String generationId;
		public String getGeneratorId() {
			return generatorId;
		}
		public void setGeneratorId(String generatorId) {
			this.generatorId = generatorId;
		}
		public String getGenerationId() {
			return generationId;
		}
		public void setGenerationId(String generationId) {
			this.generationId = generationId;
		}
	}
	
	@Override
	public MenuItem getContext(Entry entry) {
		Map<String, ModelGenerator> modelGenerators = getModelGenerators();
		if (!entry.isLeaf() && !entry.isNode() && !modelGenerators.isEmpty()) {
			Menu menu = new Menu("Generate Model");
			MenuItem item = new MenuItem("From Generator");
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					Set<Property<?>> properties = new LinkedHashSet<Property<?>>();
					EnumeratedSimpleProperty<String> generatorProperty = new EnumeratedSimpleProperty<String>("Generator", String.class, true);
					generatorProperty.addEnumeration(modelGenerators.keySet());
					properties.add(generatorProperty);
					final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
					EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Choose model generator", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							String generatorName = updater.getValue("Generator");
							if (generatorName != null) {
								ModelGenerator modelGenerator = modelGenerators.get(generatorName);
								List<StructureModel> models;
								Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
								try {
									ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
									ServiceUtils.setServiceContext(null, entry.getId());
									models = modelGenerator.getModels();
								}
								finally {
									ServiceRuntime.setGlobalContext(originalContext);
								}
								/**
								 * IMPORTANT:
								 * 
								 * the combobox logic is _very_ fragile, it took a long time to balance the issues that arose to create a sorted filterable combobox
								 * a lot of iterations were attempted until the current solution was reached, it can probably be optimized though, for instance we could retain + add instead of remove all. this was in there for a while but taken out as other issues kept popping up (in retrospect: likely unrelated)
								 */
								if (models != null && !models.isEmpty()) {
									Map<String, StructureModel> modelMap = new HashMap<String, StructureModel>();
									for (StructureModel model : models) {
										modelMap.put(model.getCollectionName(), model);
									}
									
									ComboBox<StructureModel> comboBox = new ComboBox<StructureModel>();
									// after blur, the fromString is called and it must return the correct object or the selection will be unset
									comboBox.setConverter(new StringConverter<StructureModel>() {
										@Override
										public String toString(StructureModel object) {
											if (object == null) {
												return null;
											}
											return object.getCollectionName()
												+ (object.getTitle() == null ? "" : " (" + object.getTitle() + ")");
										}
										@Override
										public StructureModel fromString(String string) {
											if (string == null) {
												return null;
											}
											int index = string.indexOf("(");
											if (index >= 0) {
												string = string.substring(0, index).trim(); 
											}
											return modelMap.get(string);
										}
									});
									List<StructureModel> allValues = new ArrayList(modelMap.values());
									Comparator<StructureModel> comparator = new Comparator<StructureModel>() {
										@Override
										public int compare(StructureModel o1, StructureModel o2) {
											return o1.getCollectionName().compareTo(o2.getCollectionName());
										}
									};
									comboBox.getItems().addAll(allValues);
									Collections.sort(comboBox.getItems(), comparator);
									comboBox.setEditable(true);
									comboBox.getEditor().textProperty().addListener(new ChangeListener<String>() {
										@SuppressWarnings({ "rawtypes", "unchecked" })
										@Override
										public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
											StringConverter converter = comboBox.getConverter();
											// we check that there are SOME items
											// when we clear the collection box to add all the items, we actually wipe the selection and reset it, this triggers a roundtrip to an empty string
											if ((oldValue == null && newValue != null) || (oldValue != null && newValue == null) || (oldValue != null && newValue != null && !oldValue.equals(newValue)) && comboBox.getItems().size() > 0) {
												SingleSelectionModel selectionModel = comboBox.getSelectionModel();
												Object selectedItem = comboBox.getSelectionModel().getSelectedItem();
												// by default we show all
												boolean showAll = true;
												// if there is a value, we may want to filter
												if (newValue != null && !newValue.trim().isEmpty()) {
													// check that it doesn't match with the currently selected value
													if (comboBox.getSelectionModel().getSelectedItem() == null || (comboBox.getSelectionModel().getSelectedItem() != null && !comboBox.getConverter().toString(comboBox.getSelectionModel().getSelectedItem()).equals(newValue))) {
														showAll = false;
														List toShow = new ArrayList();
														for (Object value : allValues) {
															if (converter.toString(value).toLowerCase().indexOf(newValue) >= 0) {
																toShow.add(value);
															}
														}
														if (!comboBox.getItems().equals(toShow)) {
															comboBox.getItems().clear();
															comboBox.getItems().addAll(toShow);
															Collections.sort(comboBox.getItems(), comparator);
//															selectionModel.select(selectedItem);
														}
													}
												}
												// the equals does not work on these lists, so we just check size
												if (showAll && allValues.size() > comboBox.getItems().size()) {
													comboBox.getItems().clear();
													comboBox.getItems().addAll(allValues);
													Collections.sort(comboBox.getItems(), comparator);
													selectionModel.select(selectedItem);
												}
											}
										}
									});
									
//									final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
									VBox pane = new VBox();
									TextField name = new TextField();
									pane.getChildren().add(EAIDeveloperUtils.newHBox("Model", comboBox));
									pane.getChildren().add(EAIDeveloperUtils.newHBox("Name", name));
									pane.setPadding(new Insets(10));
									HBox buttons = new HBox();
									buttons.setAlignment(Pos.CENTER);
									pane.getChildren().add(buttons);
									Button ok = new Button("Ok");
									Button cancel = new Button("Cancel");
									buttons.getChildren().addAll(ok, cancel);
									
									comboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<StructureModel>() {
										@Override
										public void changed(ObservableValue<? extends StructureModel> observable, StructureModel oldValue, StructureModel newValue) {
											name.setPromptText(newValue == null ? "Select a model" : newValue.getName());
										}
									});
									
									Stage buildPopup = EAIDeveloperUtils.buildPopup("Choose model", pane);
									
									cancel.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent event) {
											buildPopup.hide();
										}
									});
									ok.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent event) {
//											String structureName = updater.getValue("Name");
											//String modelName = updater.getValue("Model");
											StructureModel structureModel = comboBox.getSelectionModel().getSelectedItem();
											String structureName = name.getText();
											if (structureModel != null) {
												try {
													if (structureName == null || structureName.trim().isEmpty()) {
														structureName = structureModel.getName();
													}
													StructureManager manager = new StructureManager();
													RepositoryEntry repositoryEntry = ((RepositoryEntry) entry).createNode(structureName, manager, true);
													ModelGenerationInformation information = new ModelGenerationInformation();
													information.setGeneratorId(generatorName);
													information.setGenerationId(structureModel.getId());
													setModelGenerationInformation(repositoryEntry, information);
													DefinedStructure structure = new DefinedStructure();
													structure.setId(entry.getId() + "." + structureName);
													structure.setName(structureName);
													
													Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
													try {
														ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
														ServiceUtils.setServiceContext(null, entry.getId());
														generate(modelGenerator, structure, structureModel);
													}
													finally {
														ServiceRuntime.setGlobalContext(originalContext);
													}
													manager.saveContent(repositoryEntry, structure);
													MainController.getInstance().getRepositoryBrowser().refresh();
												}
												catch (Exception e) {
													MainController.getInstance().notify(e);
												}
												finally {
													buildPopup.hide();
												}
											}
											else {
												buildPopup.hide();
											}
										}
									});
								}
								
							}
						}
					});
				}
			});
			
			menu.getItems().add(item);
			return menu;
		}
		else if (entry.isNode() && !modelGenerators.isEmpty()) {
			ModelGenerationInformation information = getModelGenerationInformation(entry);
			if (information != null) {
				MenuItem item = new MenuItem("Regenerate model");
				item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						try {
							Structure structure = (Structure) entry.getNode().getArtifact();
							ModelGenerator generator = getModelGenerators().get(information.getGeneratorId());
							if (generator != null) {
								List<StructureModel> models = generator.getModels();
								if (models != null) {
									for (StructureModel model : models) {
										if (model != null && model.getId() != null && model.getId().equals(information.getGenerationId())) {
											Map<String, Object> originalContext = ServiceRuntime.getGlobalContext();
											try {
												ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
												ServiceUtils.setServiceContext(null, entry.getId());
												generate(generator, structure, model);
											}
											finally {
												ServiceRuntime.setGlobalContext(originalContext);
											}
											StructureManager manager = new StructureManager();
											manager.saveContent((ResourceEntry) entry, structure);
											MainController.getInstance().getRepositoryBrowser().refresh();
										}
									}
								}
							}
						}
						catch (Exception e) {
							MainController.getInstance().notify(e);
						}
					}
				});
				return item;
			}
		}
		return null;
	}
	
	// TODO: don't remove/regenerate referenced documents, we assume you will update that as needed
	private static void generate(ModelGenerator generator, ModifiableComplexType structure, StructureModelField model) {
		List<StructureModelField> fields = model.getFields();
		// if we don't have fields passed in and we have an id, try to resolve it
		if (fields == null && model instanceof StructureModel && generator != null) {
			fields = generator.getFields(((StructureModel) model).getId()); 
		}
		if (fields != null) {
			structure.setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), model.getCollectionName()));
			structure.setProperty(new ValueImpl<String>(CollectionCrudProviderProperty.getInstance(), model.getCollectionCrudProvider()));
			List<String> usedFields = new ArrayList<String>();
			for (StructureModelField field : fields) {
				usedFields.add(field.getName());
				Element<?> element = structure.get(field.getName());
				// if you have replaced an anonymous type with a defined type, we skip the element, we assume you will keep that up to date
				if (element != null && element.getType() instanceof ComplexType && element.getType() instanceof DefinedType) {
					continue;
				}
				if (field.getType() == "structure") {
					if (element != null && !(element.getType() instanceof ModifiableComplexType)) {
						structure.remove(element);
						element = null;
					}
					if (element == null) {
						Structure child = new Structure();
						child.setName(field.getName());
						element = new ComplexElementImpl(field.getName(), child, structure);
						structure.add(element);
					}
					if (element.getType() instanceof ModifiableComplexType) {
						generate(generator, (ModifiableComplexType) element.getType(), field);
					}
				}
				else {
					DefinedSimpleType<?> type = SimpleTypeWrapperFactory.getInstance().getWrapper().getByName(field.getType() == null ? "string" : field.getType());
					// default to string
					if (type == null) {
						type = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
					}
					// if it is not of the same type, try to change it
					if (element != null && !element.getType().getName().equals(type.getName())) {
						// attempt to modify in place, otherwise it will lose its original position in the document
						// this is not too bad just...annoying
						if (element instanceof ModifiableElement) {
							((ModifiableElement<?>) element).setType(type);
						}
						else {
							structure.remove(element);
							element = null;
						}
					}
					if (element == null) {
						element = new SimpleElementImpl(field.getName(), type, structure);
						structure.add(element);
					}
				}
				element.setProperty(new ValueImpl<Boolean>(PrimaryKeyProperty.getInstance(), field.getPrimaryKey()));
				element.setProperty(new ValueImpl<String>(AliasProperty.getInstance(), field.getAlias()));
				element.setProperty(new ValueImpl<String>(FormatProperty.getInstance(), field.getFormat()));
				// fix some basic properties on the element
				if (field.getMaxOccurs() != null) {
					element.setProperty(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), field.getMaxOccurs()));
				}
				if (field.getMinOccurs() != null) {
					element.setProperty(new ValueImpl<Integer>(MinOccursProperty.getInstance(), field.getMinOccurs()));
				}
			}
			Iterator<Element<?>> iterator = structure.iterator();
			while (iterator.hasNext()) {
				Element<?> next = iterator.next();
				if (!usedFields.contains(next.getName())) {
					iterator.remove();
				}
			}
		}
	}
	
	private static void setModelGenerationInformation(Entry entry, ModelGenerationInformation information) {
		if (entry instanceof RepositoryEntry) {
			ResourceContainer<?> container = ((RepositoryEntry) entry).getContainer();
			WritableResource child = (WritableResource) container.getChild("model-generator.xml");
			try {
				if (child == null) {
					child = (WritableResource) ((ManageableContainer<?>) container).create("model-generator.xml", "application/xml");
				}
				JAXBContext jaxb = JAXBContext.newInstance(ModelGenerationInformation.class);
				try (OutputStream outputStream = IOUtils.toOutputStream(child.getWritable())) {
					jaxb.createMarshaller().marshal(information, outputStream);
				}
			}
			catch (Exception e) {
				MainController.getInstance().notify(e);
			}
		}
	}
	
	private static ModelGenerationInformation getModelGenerationInformation(Entry entry) {
		if (entry instanceof RepositoryEntry) {
			ResourceContainer<?> container = ((RepositoryEntry) entry).getContainer();
			ReadableResource child = (ReadableResource) container.getChild("model-generator.xml");
			try {
				JAXBContext jaxb = JAXBContext.newInstance(ModelGenerationInformation.class);
				try (ReadableContainer<ByteBuffer> readable = child.getReadable()) {
					return (ModelGenerationInformation) jaxb.createUnmarshaller().unmarshal(IOUtils.toInputStream(readable));
				}
			}
			catch (Exception e) {
				MainController.getInstance().notify(e);
			}
		}
		return null;
	}
}
