package be.nabu.eai.module.types.structure;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.ArtifactGUIInstance;
import be.nabu.eai.developer.api.ArtifactGUIManager;
import be.nabu.eai.developer.api.ConfigurableGUIManager;
import be.nabu.eai.developer.collection.EAICollectionUtils;
import be.nabu.eai.developer.components.RepositoryBrowser;
import be.nabu.eai.developer.impl.CustomTooltip;
import be.nabu.eai.developer.managers.util.ElementMarshallable;
import be.nabu.eai.developer.managers.util.RootElementWithPush;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.ElementClipboardHandler;
import be.nabu.eai.developer.util.ElementSelectionListener;
import be.nabu.eai.developer.util.ElementTreeItem;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.impl.PropertyUpdatedEventImpl;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.MovableTreeItem;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.Tree.CellDescriptor;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.jfx.control.tree.TreeUtils;
import be.nabu.jfx.control.tree.Updateable;
import be.nabu.jfx.control.tree.MovableTreeItem.Direction;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.jfx.control.tree.drag.TreeDragListener;
import be.nabu.jfx.control.tree.drag.TreeDropListener;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.api.ModifiableElement;
import be.nabu.libs.types.api.ModifiableTypeInstance;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.StringMapCollectionHandlerProvider;
import be.nabu.libs.types.base.TypeBaseUtils;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.types.properties.CollectionHandlerProviderProperty;
import be.nabu.libs.types.properties.NameProperty;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.SimpleStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class StructureGUIManager implements ArtifactGUIManager<DefinedStructure>, ConfigurableGUIManager<DefinedStructure> {

	static {
		URL resource = StructureGUIManager.class.getClassLoader().getResource("structure.css");
		if (resource != null) {
			MainController.registerStyleSheet(resource.toExternalForm());
		}
	}
	
	private MainController controller;
	private boolean allowComplexChildren = true;
	
	private EventDispatcher dispatcher = new EventDispatcherImpl();

	private java.util.Map<String, String> configuration;
	public static final String ACTUAL_ID = "actualId";
	
	private String actualId;
	private ElementSelectionListener elementSelectionListener;

	public String isActualId() {
		return actualId;
	}

	public void setActualId(String actualId) {
		this.actualId = actualId;
	}
	
	@Override
	public ArtifactManager<DefinedStructure> getArtifactManager() {
		return new StructureManager();
	}

	// even though it belongs in "types", put it in the main tree
//	@Override
//	public String getCategory() {
//		return "Types";
//	}
	
	@Override
	public String getArtifactName() {
		return "Structure";
	}

	@Override
	public ImageView getGraphic() {
		return MainController.loadGraphic("types/structure.gif");
	}
	
	@Override
	public void setConfiguration(java.util.Map<String, String> configuration) {
		this.configuration = configuration;
	}
	public String getId(DefinedStructure structure) {
		return configuration == null || configuration.get(ACTUAL_ID) == null ? (actualId == null ? (structure == null ? null : structure.getId()) : actualId) : configuration.get(ACTUAL_ID);
	}

	@Override
	public ArtifactGUIInstance create(final MainController controller, final TreeItem<Entry> target) throws IOException {
		this.controller = controller;
		List<Property<?>> properties = new ArrayList<Property<?>>();
		properties.add(new SimpleProperty<String>("Name", String.class, true));
		final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(properties));
		final StructureGUIInstance instance = new StructureGUIInstance(this);
		EAIDeveloperUtils.buildPopup(controller, updater, "Create Structure", new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				try {
					String name = updater.getValue("Name");
					create(controller, ((RepositoryEntry) target.itemProperty().get()), instance, name);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				catch (ParseException e) {
					throw new RuntimeException(e);
				}
			}

		});
		return instance;
	}
	
	public StructureGUIInstance create(final MainController controller, final RepositoryEntry target, String name) throws IOException, ParseException {
		final StructureGUIInstance instance = new StructureGUIInstance(this);
		create(controller, target, instance, name);
		return instance;
	}
	
	private void create(final MainController controller, final RepositoryEntry target, final StructureGUIInstance instance, String name) throws IOException, ParseException {
		RepositoryEntry entry = target.createNode(name, getArtifactManager(), true);
		DefinedStructure structure = new DefinedStructure();
		structure.setName(name == null ? "root" : name);
		getArtifactManager().save(entry, structure);
		controller.getRepositoryBrowser().refresh();
		
		// reload
		MainController.getInstance().getAsynchronousRemoteServer().reload(target.getId());
		MainController.getInstance().getCollaborationClient().created(entry.getId(), "Created");
		
		setActualId(entry.getId());
		Tab tab = controller.newTab(entry.getId(), instance);
		AnchorPane pane = new AnchorPane();
		tab.setContent(pane);
		display(controller, pane, structure);
		instance.setEntry(entry);
		instance.setStructure(structure);
	}

	@Override
	public ArtifactGUIInstance view(MainController controller, TreeItem<Entry> target) throws IOException, ParseException {
		this.controller = controller;
		StructureGUIInstance instance = new StructureGUIInstance(this, target.itemProperty().get(), null);
		Tab tab = controller.newTab(target.itemProperty().get().getId(), instance);
		AnchorPane pane = new AnchorPane();
		tab.setContent(pane);
		instance.setStructure(display(controller, pane, target.itemProperty().get()));
		return instance;
	}
	
	DefinedStructure display(final MainController controller, Pane pane, Entry entry) throws IOException, ParseException {
		DefinedStructure structure = (DefinedStructure) entry.getNode().getArtifact();
		setActualId(structure.getId());
		display(controller, pane, new RootElementWithPush(structure, true), entry.isEditable(), false);
		return structure;
	}
	
	public void display(final MainController controller, Pane pane, Structure structure) throws IOException, ParseException {
		// if someone externally sets an external id, it wins
		if (structure instanceof DefinedStructure && actualId == null) {
			setActualId(((DefinedStructure) structure).getId());
		}
		display(controller, pane, new RootElementWithPush(structure, true), true, false);
	}
	private Node createMoveButton(Tree<?> serviceTree, Direction direction, BooleanBinding notLocked, String tooltip) {
		Button button = new Button();
//		button.setTooltip(new Tooltip(direction.name()));
		new CustomTooltip(tooltip).install(button);
		button.setGraphic(MainController.loadFixedSizeGraphic("move/" + direction.name().toLowerCase() + ".png", 12));
		button.disableProperty().bind(notLocked);
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				TreeCell<?> cell = serviceTree.getSelectionModel().getSelectedItem();
				if (cell != null) {
					TreeItem<?> item = cell.getItem();
					((MovableTreeItem<?>) item).move(direction);
				}
			}
		});
		return button;
	}
	
	public static class StructureExtract {
		private String name;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	public Tree<Element<?>> display(final MainController controller, Pane pane, Element<?> element, boolean isEditable, boolean allowNonLocalModification, Button...customButtons) throws IOException, ParseException {
		this.controller = controller;
		final Tree<Element<?>> tree = new Tree<Element<?>>(new ElementMarshallable(),
			new Updateable<Element<?>>() {
				@Override
				public Element<?> update(TreeCell<Element<?>> cell, String name) {
					String oldValue = cell.getItem().getName();
					if (ElementTreeItem.rename(controller, cell.getItem(), name)) {
						dispatcher.fire(new PropertyUpdatedEventImpl(cell.getItem(), NameProperty.getInstance(), name, oldValue), this);
					}
					return cell.getItem().itemProperty().get();
				}
			}, newCellDescriptor());
		
		tree.getStyleClass().add("tree");
		tree.rootProperty().set(new ElementTreeItem(element, null, isEditable, allowNonLocalModification));
		tree.getTreeCell(tree.rootProperty().get()).expandedProperty().set(true);
		tree.setClipboardHandler(new ElementClipboardHandler(tree));
		
		String lockId = getId(null);
		BooleanProperty locked = lockId == null ? new SimpleBooleanProperty(true) : controller.hasLock(lockId);
		BooleanBinding notLocked = locked.not();

		ElementTreeItem.setListeners(tree, locked);
		
		tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		HBox allButtons = new HBox();
		allButtons.getStyleClass().add("structure-all-buttons");
		// buttons
		final HBox buttons = new HBox();
		if (customButtons != null && customButtons.length > 0) {
			allButtons.getChildren().addAll(Arrays.asList(customButtons));
		}
		if (allowComplexChildren) {
			buttons.getChildren().add(createAddButton(tree, Structure.class, "A structure is a complex type that contains other types"));
		}
		buttons.getChildren().add(createAddButton(tree, String.class, "A string is a series of characters that make up textual content"));
		buttons.getChildren().add(createAddButton(tree, Date.class, "A date is a point in time"));
		buttons.getChildren().add(createAddButton(tree, Boolean.class, "Either true or false"));
		buttons.getChildren().add(createAddButton(tree, UUID.class, "A globally unique id"));
//		buttons.getChildren().add(createAddButton(tree, Integer.class, "An integer number with 32 bit precision"));
		buttons.getChildren().add(createAddButton(tree, Long.class, "An integer number with 64 bit precision"));
//		buttons.getChildren().add(createAddButton(tree, Float.class, "A floating number with 32 bit precision"));
		buttons.getChildren().add(createAddButton(tree, Double.class, "A floating number with 64 bit precision"));
		buttons.getChildren().add(createAddButton(tree, Object.class, "A generic object, any data type can be mapped to an object but it provides very little type safety"));
		buttons.getChildren().add(createAddButton(tree, byte[].class, "An array of bytes"));
//		buttons.getChildren().add(createAddMapButton(tree));
		allButtons.getChildren().add(buttons);
		
		allButtons.disableProperty().bind(notLocked);
		
		allButtons.setPadding(new Insets(10, 5, 0, 5));
		allButtons.setAlignment(Pos.BOTTOM_LEFT);
		
		ScrollPane scrollPane = new ScrollPane();
		VBox vbox = new VBox();
		
		vbox.getChildren().add(createSearchable(tree));
		
		if (isEditable) {
			vbox.getChildren().add(allButtons);	
		}
		vbox.getChildren().add(scrollPane);
		// the tree _must_ be contained by a container that can contain other things so we can attach stuff to the tree, if we set it directly in the scrollpane, this won't work
		VBox treeContainer = new VBox();
		treeContainer.getChildren().add(tree);
		scrollPane.setContent(treeContainer);
		pane.getChildren().add(vbox);
		VBox.setVgrow(scrollPane, Priority.ALWAYS);
		
		HBox moveButtons = new HBox();
		moveButtons.getStyleClass().add("structure-move-buttons");
		moveButtons.getChildren().add(createMoveButton(tree, Direction.LEFT, notLocked, "Move the selected field to the parent"));
		moveButtons.getChildren().add(createMoveButton(tree, Direction.RIGHT, notLocked, "Move the selected field into the sibling right above this one"));
		moveButtons.getChildren().add(createMoveButton(tree, Direction.UP, notLocked, "Move the selected field up"));
		moveButtons.getChildren().add(createMoveButton(tree, Direction.DOWN, notLocked, "Move the selected field down"));
		moveButtons.setAlignment(Pos.TOP_CENTER);
		moveButtons.setPadding(new Insets(0, 5, 10, 5));
		moveButtons.disableProperty().bind(notLocked);
		if (isEditable) {
			vbox.getChildren().add(moveButtons);
		}
		
		scrollPane.prefHeightProperty().bind(pane.heightProperty());
		if (pane instanceof AnchorPane) {
			AnchorPane.setBottomAnchor(vbox, 0d);
			AnchorPane.setTopAnchor(vbox, 0d);
			AnchorPane.setLeftAnchor(vbox, 0d);
			AnchorPane.setRightAnchor(vbox, 0d);
		}
		vbox.prefWidthProperty().bind(pane.widthProperty());
		// minus scrollbar
		tree.prefWidthProperty().bind(vbox.widthProperty().subtract(25));
		
		elementSelectionListener = new ElementSelectionListener(controller, true);
		elementSelectionListener.setActualId(lockId);
		tree.getSelectionModel().selectedItemProperty().addListener(elementSelectionListener);
		
		tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeCell<Element<?>>>() {
			@Override
			public void changed(ObservableValue<? extends TreeCell<Element<?>>> arg0, TreeCell<Element<?>> arg1, TreeCell<Element<?>> arg2) {
				if (arg2 != null) {
					boolean editable = false;
					// disable all buttons
					Type type = arg2.getItem().itemProperty().get().getType();
					if (type instanceof ModifiableComplexType) {
						editable = arg2.getItem().editableProperty().get();
					}
					// if the type itself is not modifiable, check the parent
					else {
						TreeCell<Element<?>> parent = arg2.getParent();
						if (parent != null && parent.getItem().itemProperty().get().getType() instanceof ModifiableComplexType && parent.getItem().editableProperty().get()) {
							editable = true;
						}
					}
					buttons.disableProperty().set(!editable);
//					buttons.disableProperty().set(!(type instanceof ModifiableComplexType) || !arg2.getItem().editableProperty().get());
				}
			}
		});
		
		TreeDragDrop.makeDraggable(tree, new TreeDragListener<Element<?>>() {
			@Override
			public boolean canDrag(TreeCell<Element<?>> cell) {
				// if we specifically block moves, you can't drag it
				if (cell.getItem() instanceof ElementTreeItem && !ElementTreeItem.canRename(cell.getItem())) {
					return false;
				}
				// it can not be part of a defined type nor can it be the root
				// also: the source type has to be modifiable because you will be dragging it from there
				return locked.get() && cell.getItem().getParent() != null
					&& (cell.getItem().getParent().itemProperty().get().getType() instanceof ModifiableComplexType)
					&& cell.getItem().getParent().editableProperty().get()
					&& cell.getItem().editableProperty().get();
			}
			@Override
			public void drag(TreeCell<Element<?>> cell) {
				// do nothing
			}
			@Override
			public String getDataType(TreeCell<Element<?>> cell) {
//				return cell.getItem().itemProperty().get().getType().getNamespace() + ":" + cell.getItem().itemProperty().get().getType().getName();
				return ElementTreeItem.DATA_TYPE_DEFINED;
			}
			@Override
			public TransferMode getTransferMode() {
				return TransferMode.MOVE;
			}
			@Override
			public void stopDrag(TreeCell<Element<?>> arg0, boolean successful) {
				// do nothing
			}
		});
		EAIDeveloperUtils.addElementExpansionHandler(tree);
		
		// the above "expansion handler" also injects code to unset the right click menu
		// we assume chronological triggering in order of registration, so we register this one after that
		// not clean...but hey
		tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeCell<Element<?>>>() {
			@Override
			public void changed(ObservableValue<? extends TreeCell<Element<?>>> arg0, TreeCell<Element<?>> arg1, TreeCell<Element<?>> selected) {
				Entry resolve = controller.getRepository().getEntry(lockId);
				if (selected != null && resolve != null && resolve.getParent() instanceof RepositoryEntry) {
					Element<?> element = selected.getItem().itemProperty().get();
					boolean set = false;
					if (tree.getContextMenuProvider() != null) {
						ContextMenu menu = tree.getContextMenuProvider().getMenu(Arrays.asList(selected));
						if (menu != null) {
							tree.setContextMenu(menu);
							set = true;
						}
					}
					Menu menu = new Menu();
					if (!set && element.getType() instanceof Structure && !(element.getType() instanceof DefinedStructure)) {
						MenuItem item = new MenuItem("Extract to new type");
						item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								StructureExtract extract = new StructureExtract();
								EAIDeveloperUtils.buildPopup("Extract to new type", "Extract to new type", extract, new EventHandler<ActionEvent>() {
									@Override
									public void handle(ActionEvent event) {
										if (extract.getName() != null && !extract.getName().trim().isEmpty()) {
											StructureManager manager = new StructureManager();
											String name = extract.getName().trim();
											String normalized = EAICollectionUtils.normalize(name);
											try {
												RepositoryEntry repositoryEntry = ((RepositoryEntry) resolve.getParent()).createNode(normalized, manager, true);
												if (!normalized.equals(name)) {
													repositoryEntry.getNode().setName(name);
													repositoryEntry.saveNode();
												}
												// make sure we set the name on the element itself!
												((Structure) element.getType()).setName(element.getName());
												manager.saveContent(repositoryEntry, (ComplexType) element.getType());
												MainController.getInstance().getRepositoryBrowser().refresh();
												EAIDeveloperUtils.created(repositoryEntry.getId());
												// if we can modify it, redraw it
												if (element instanceof ModifiableElement) {
													((ModifiableElement<?>) element).setType((Type) repositoryEntry.getNode().getArtifact());
													MainController.getInstance().setChanged();
													MainController.getInstance().redraw(resolve.getId());
												}
											}
											catch (Exception e) {
												MainController.getInstance().notify(e);
											}
										}
									}
								}, false).show();
							}
						});
						menu.getItems().add(item);
					}
					if (element instanceof ModifiableElement && element.getType() instanceof Structure && !(element.getType() instanceof DefinedStructure)) {
						// if it is a simple complex type
						if (element.getType() instanceof SimpleStructure) {
							MenuItem item = new MenuItem("Convert to complex type");
							// TODO
//							menu.getItems().add(item);
						}
						else {
							MenuItem item = new MenuItem("Convert to simple type");
							item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent event) {
									SimpleStructure<?> simple = new SimpleStructure(SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class));
									for (Element<?> child : (Structure) element.getType()) {
										Element<?> clone = TypeBaseUtils.clone(child, simple);
										if (!clone.getName().startsWith("@")) {
											clone.setProperty(new ValueImpl<String>(NameProperty.getInstance(), "@" + clone.getName()));
										}
										simple.add(clone);
									}
									((ModifiableElement<?>) element).setType(simple);
									MainController.getInstance().setChanged();
									MainController.getInstance().redraw(resolve.getId());
								}
							});
							menu.getItems().add(item);
						}
					}
					if (!menu.getItems().isEmpty()) {
						ContextMenu contextMenu = new ContextMenu(menu.getItems().toArray(new MenuItem[0]));
						tree.setContextMenu(contextMenu);
					}
				}
			}
		});
		
		tree.addEventHandler(DragEvent.DRAG_OVER, new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard dragboard = event.getDragboard();
				if (dragboard != null) {
					Object content = dragboard.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedType.class)));
					// this will be the path in the tree
					if (content != null) {
						Artifact artifact = controller.getRepository().resolve((String) content);
						if (artifact instanceof Type) {
							event.acceptTransferModes(TransferMode.MOVE);
							event.consume();
						}
					}
				}
			}
		});
		tree.addEventHandler(DragEvent.DRAG_DROPPED, new EventHandler<DragEvent>() {
			@SuppressWarnings("unchecked")
			@Override
			public void handle(DragEvent event) {
				Dragboard dragboard = event.getDragboard();
				if (dragboard != null) {
					Object content = dragboard.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedType.class)));
					if (content != null) {
						Artifact artifact = controller.getRepository().resolve((String) content);
						if (artifact instanceof Type) {
							TreeCell<Element<?>> target = (TreeCell<Element<?>>) ((Node) event.getTarget()).getUserData();
							if (target != null) {
								controller.notify(
									addElement(target.getItem().itemProperty().get(), (Type) artifact, ElementTreeItem.UNNAMED + ElementTreeItem.getLastCounter((ComplexType) target.getItem().itemProperty().get().getType()))
									.toArray(new ValidationMessage[0]));
								target.refresh();
								MainController.getInstance().setChanged();
								MainController.getInstance().closeDragSource();
								event.consume();
							}
						}
					}
				}
			}
		});
		// can only make it drag/droppable after it's added because it needs the scene
		TreeDragDrop.makeDroppable(tree, new TreeDropListener<Element<?>>() {
			@Override
			public boolean canDrop(String dataType, TreeCell<Element<?>> target, TreeCell<?> dragged, TransferMode transferMode) {
				if (!locked.get()) {
					return false;
				}
				// this drop listener is only interested in move events which mean it originates from its own tree
				// or copy events (originates from the repository)
				if (transferMode != TransferMode.MOVE && transferMode != TransferMode.COPY) {
					return false;
				}
				if (!dragged.getTree().equals(target.getTree()) && !MainController.isRepositoryTree(dragged.getTree())) {
					return false;
				}
				if (!target.getItem().editableProperty().get()) {
					return false;
				}
				// if it's the root and the root is modifiable, we can drop it there
				else if (target.getItem().getParent() == null || (!target.getItem().leafProperty().get() && !target.getItem().equals(dragged.getItem().getParent()))) {
					return target.getItem().itemProperty().get().getType() instanceof ModifiableComplexType;
				}
				return false;
			}
			@SuppressWarnings({ "unchecked" })
			@Override
			public void drop(String dataType, TreeCell<Element<?>> target, TreeCell<?> dragged, TransferMode transferMode) {
				// if the cell to drop is from this tree, we need to actually move it
				if (dragged.getTree().equals(tree)) {
					ModifiableComplexType newParent;
					// we need to wrap an extension around it
					if (target.getItem().itemProperty().get().getType() instanceof DefinedType && target.getItem().getParent() != null) {
						Structure structure = new Structure();
						structure.setSuperType(target.getItem().itemProperty().get().getType());
						((ModifiableTypeInstance) target.getItem().itemProperty().get()).setType(structure);
						newParent = structure;
					}
					else {
						newParent = (ModifiableComplexType) target.getItem().itemProperty().get().getType();
					}
					TreeCell<Element<?>> draggedElement = (TreeCell<Element<?>>) dragged;
					String fromPath = TreeUtils.getPath(draggedElement.getItem());
					// if there are no validation errors when adding, remove the old one
					ModifiableComplexType originalParent = (ModifiableComplexType) draggedElement.getItem().itemProperty().get().getParent();
					List<ValidationMessage> messages = newParent.add(draggedElement.getItem().itemProperty().get());
					if (messages.isEmpty()) {
						// remove by the actual parent, not the tree parent (e.g. for pipeline extensions)
						originalParent.remove(draggedElement.getItem().itemProperty().get());
						// make sure someone else didn't update the parent (e.g. pipeline extension)
						if (draggedElement.getItem().itemProperty().get() instanceof ModifiableElement && draggedElement.getItem().itemProperty().get().getParent().equals(originalParent)) {
							// update the parent
							((ModifiableElement<?>) draggedElement.getItem().itemProperty().get()).setParent(newParent);
						}
					}
					else {
						controller.notify(messages.toArray(new ValidationMessage[0]));
					}
					// refresh both, in this specific order! or the parent will be the new one
					dragged.getParent().refresh();
					target.refresh();

					if (messages.isEmpty()) {
						String toPath = null;
						for (TreeItem<Element<?>> item : target.getItem().getChildren()) {
							if (item.getName().equals(draggedElement.getItem().itemProperty().get().getName())) {
								toPath = TreeUtils.getPath(item);
							}
						}
						if (toPath != null) {
							ElementTreeItem.renameVariable(controller, fromPath, toPath);
						}
						else {
							MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Could not refactor " + draggedElement.getItem().itemProperty().get().getName()));
						}
					}
							
					MainController.getInstance().setChanged();
//					((ElementTreeItem) target.getItem()).refresh();
//					((ElementTreeItem) dragged.getParent().getItem()).refresh();
				}
				// if it is from the repository tree, we need to add it
				else if (MainController.isRepositoryTree(dragged.getTree())) {
					TreeCell<Entry> draggedElement = (TreeCell<Entry>) dragged;
					try {
						if (draggedElement.getItem().itemProperty().get().getNode().getArtifact() instanceof DefinedType) {
							DefinedType definedType = (DefinedType) draggedElement.getItem().itemProperty().get().getNode().getArtifact();
							controller.notify(
								addElement(target.getItem().itemProperty().get(), definedType, ElementTreeItem.UNNAMED + ElementTreeItem.getLastCounter((ComplexType) target.getItem().itemProperty().get().getType()))
								.toArray(new ValidationMessage[0]));
							target.refresh();
							MainController.getInstance().setChanged();
							MainController.getInstance().closeDragSource();
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					catch (ParseException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
		return tree;
	}

	public static VBox createSearchable(final Tree<Element<?>> tree) {
		VBox search = new VBox();
		TextField searchText = new TextField();
		searchText.setPromptText("Search for field");
		search.getChildren().add(searchText);
		search.setPadding(new Insets(10, 5, 0, 5));
		
		searchText.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				find(tree.getRootCell(), newValue == null ? null : newValue.trim().toLowerCase());
			}
			private void find(TreeCell<Element<?>> cell, String value) {
				cell.getCellValue().getNode().getStyleClass().removeAll("search-unmatched", "search-matched");
				if (value != null && !value.isEmpty()) {
					String name = cell.getItem().getName();
					if (name != null && name.toLowerCase().contains(value)) {
						cell.getCellValue().getNode().getStyleClass().add("search-matched");		
					}
					else {
						cell.getCellValue().getNode().getStyleClass().add("search-unmatched");
					}
				}
				for (TreeCell<Element<?>> child : cell.getChildren()) {
					find(child, value);
				}
			}
		});
		return search;
	}

	public static CellDescriptor newCellDescriptor() {
		return new CellDescriptor() {
			@Override
			public void describe(Label label, String description) {
				Node loadGraphic = MainController.getInfoIcon();
				CustomTooltip customTooltip = new CustomTooltip(description);
				customTooltip.install(loadGraphic);
				customTooltip.setMaxWidth(400d);
				label.setGraphic(loadGraphic);
				label.setContentDisplay(ContentDisplay.RIGHT);
			}
		};
	}

	private Button createAddButton(Tree<Element<?>> tree, Class<?> clazz, String tooltip) {
		Button button = new Button();
//		button.setTooltip(new Tooltip(clazz.getSimpleName()));
		if (tooltip != null) {
			new CustomTooltip(tooltip).install(button);
		}
		button.setGraphic(MainController.loadGraphic(ElementTreeItem.getIcon(getType(clazz))));
		button.addEventHandler(ActionEvent.ACTION, new StructureAddHandler(tree, clazz));
		return button;
	}
	
	@SuppressWarnings("unused")
	private Button createAddMapButton(Tree<Element<?>> tree) {
		Button button = new Button();
		button.setTooltip(new Tooltip("Map"));
		button.setGraphic(MainController.loadGraphic("types/map.gif"));
		button.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void handle(ActionEvent arg0) {
				TreeCell<Element<?>> selectedItem = tree.getSelectionModel().getSelectedItem();
				if (selectedItem != null && selectedItem.getItem().editableProperty().get()) {
					if (selectedItem.getItem().itemProperty().get().getType() instanceof ComplexType) {
						ComplexType target = (ComplexType) selectedItem.getItem().itemProperty().get().getType();
						controller.notify(addElement(selectedItem.getItem().itemProperty().get(), 
								BeanResolver.getInstance().resolve(java.util.Map.class), 
								ElementTreeItem.UNNAMED + ElementTreeItem.getLastCounter(target),
								new ValueImpl(CollectionHandlerProviderProperty.getInstance(), new StringMapCollectionHandlerProvider())));
					}
					selectedItem.expandedProperty().set(true);
					selectedItem.refresh();
					MainController.getInstance().setChanged();
				}
			}
		});
		return button;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<ValidationMessage> addElement(Element<?> element, Type type, String name, Value<?>...values) {
		ModifiableComplexType newParent;
		// if the target is a defined type, we need to wrap an extension around it
		if (element.getType() instanceof DefinedType && element.getParent() != null) {
			Structure structure = new Structure();
			structure.setSuperType(element.getType());
			((ModifiableTypeInstance) element).setType(structure);
			newParent = structure;
		}
		else {
			newParent = (ModifiableComplexType) element.getType();
		}
		List<ValidationMessage> messages = newParent.add(type instanceof ComplexType 
			? new ComplexElementImpl(name, (ComplexType) type, newParent, values)
			: new SimpleElementImpl(name, (SimpleType<?>) type, newParent, values)
		);
		return messages;
	}

	private class StructureAddHandler implements EventHandler<Event> {

		private Tree<Element<?>> tree;
		private Class<?> type;
		
		public StructureAddHandler(Tree<Element<?>> tree, Class<?> type) {
			this.tree = tree;
			this.type = type;
		}

		@Override
		public void handle(Event arg0) {
			TreeCell<Element<?>> selectedItem = tree.getSelectionModel().getSelectedItem();
			// if nothing is selected, add it to the root
			if (selectedItem == null) {
				selectedItem = tree.getRootCell();
			}
			if (!(selectedItem.getItem().itemProperty().get().getType() instanceof ModifiableComplexType)) {
				selectedItem = selectedItem.getParent();
			}
			if (selectedItem != null && selectedItem.getItem().editableProperty().get()) {
				// add an element in it
				if (selectedItem.getItem().itemProperty().get().getType() instanceof ComplexType) {
					ComplexType target = (ComplexType) selectedItem.getItem().itemProperty().get().getType();
					controller.notify(addElement(selectedItem.getItem().itemProperty().get(), getType(type), ElementTreeItem.UNNAMED + ElementTreeItem.getLastCounter(target)));
				}
				selectedItem.expandedProperty().set(true);
				selectedItem.refresh();
				// update it in maincontroller
				MainController.getInstance().setChanged();
//				((ElementTreeItem) selectedItem.getItem()).refresh();
				// add an element next to it
				// TODO
			}
		}		
	}
	
	public static Type getType(Class<?> clazz) {
		Type type = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(clazz);
		if (type == null) {
			try {
				type = ComplexType.class.isAssignableFrom(clazz)
					? (ComplexType) clazz.newInstance()
					: BeanResolver.getInstance().resolve(clazz);
			}
			catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
			catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		return type;
	}
	
	@Override
	public Class<DefinedStructure> getArtifactClass() {
		return getArtifactManager().getArtifactClass();
	}

	public EventDispatcher getDispatcher() {
		return dispatcher;
	}

	public boolean isAllowComplexChildren() {
		return allowComplexChildren;
	}

	public void setAllowComplexChildren(boolean allowComplexChildren) {
		this.allowComplexChildren = allowComplexChildren;
	}

	public ElementSelectionListener getElementSelectionListener() {
		return elementSelectionListener;
	}

}
