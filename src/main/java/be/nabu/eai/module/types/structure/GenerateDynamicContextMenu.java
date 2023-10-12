package be.nabu.eai.module.types.structure;

import java.io.OutputStream;
import java.util.ArrayList;
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
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
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
import be.nabu.utils.io.api.WritableContainer;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class GenerateDynamicContextMenu implements EntryContextMenuProvider {
	
	private static Map<String, ModelGenerator> modelGenerators;
	
	public static Map<String, ModelGenerator> getModelGenerators() {
		if (modelGenerators == null) {
			synchronized(GenerateDynamicContextMenu.class) {
				if (modelGenerators == null) {
					Map<String, ModelGenerator> modelGenerators = new HashMap<String, ModelGenerator>();
					MethodServiceInterface iface = MethodServiceInterface.wrap(ModelGenerator.class, "getModels");
					for (DefinedService service : EAIResourceRepository.getInstance().getArtifacts(DefinedService.class)) {
						if (POJOUtils.isImplementation(service, iface)) {
							ModelGenerator generator = POJOUtils.newProxy(ModelGenerator.class, EAIResourceRepository.getInstance(), SystemPrincipal.ROOT, EAIResourceRepository.getInstance().getServiceRunner(), service);
							modelGenerators.put(service.getId(), generator);
						}
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
								List<StructureModel> models = modelGenerator.getModels();
								if (models != null && !models.isEmpty()) {
									Map<String, StructureModel> modelMap = new HashMap<String, StructureModel>();
									for (StructureModel model : models) {
										modelMap.put(model.getName(), model);
									}
									
									Set<Property<?>> properties = new LinkedHashSet<Property<?>>();
									EnumeratedSimpleProperty<String> modelProperty = new EnumeratedSimpleProperty<String>("Model", String.class, true);
									modelProperty.addEnumeration(modelMap.keySet());
									properties.add(modelProperty);
									properties.add(new SimpleProperty<String>("Name", String.class, false));
									final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
									EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Choose model", new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent event) {
											String structureName = updater.getValue("Name");
											String modelName = updater.getValue("Model");
											if (modelName != null) {
												try {
													if (structureName == null) {
														structureName = NamingConvention.LOWER_CAMEL_CASE.apply(modelName);
													}
													StructureModel structureModel = modelMap.get(modelName);
													StructureManager manager = new StructureManager();
													RepositoryEntry repositoryEntry = ((RepositoryEntry) entry).createNode(structureName, manager, true);
													ModelGenerationInformation information = new ModelGenerationInformation();
													information.setGeneratorId(generatorName);
													information.setGenerationId(structureModel.getId());
													setModelGenerationInformation(repositoryEntry, information);
													DefinedStructure structure = new DefinedStructure();
													structure.setId(entry.getId() + "." + structureName);
													structure.setName(structureName);
													generate(structure, structureModel);
													manager.saveContent(repositoryEntry, structure);
													MainController.getInstance().getRepositoryBrowser().refresh();
												}
												catch (Exception e) {
													MainController.getInstance().notify(e);
												}
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
							Artifact resolve = entry.getRepository().resolve(information.getGeneratorId());
							if (resolve instanceof Service) {
								ModelGenerator generator = POJOUtils.newProxy(ModelGenerator.class, entry.getRepository(), SystemPrincipal.ROOT, EAIResourceRepository.getInstance().getServiceRunner(), (Service) resolve);
								List<StructureModel> models = generator.getModels();
								if (models != null) {
									for (StructureModel model : models) {
										if (model != null && model.getId() != null && model.getId().equals(information.getGenerationId())) {
											generate(structure, model);
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
	private static void generate(ModifiableComplexType structure, StructureModelField model) {
		if (model.getFields() != null) {
			structure.setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), model.getCollectionName()));
			structure.setProperty(new ValueImpl<String>(CollectionCrudProviderProperty.getInstance(), model.getCollectionCrudProvider()));
			List<String> usedFields = new ArrayList<String>();
			for (StructureModelField field : model.getFields()) {
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
						generate((ModifiableComplexType) element.getType(), field);
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
