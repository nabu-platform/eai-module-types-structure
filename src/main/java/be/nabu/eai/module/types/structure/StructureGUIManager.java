package be.nabu.eai.module.types.structure;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.ArtifactGUIInstance;
import be.nabu.eai.developer.api.ArtifactGUIManager;
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
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.jfx.control.tree.Updateable;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.jfx.control.tree.drag.TreeDragListener;
import be.nabu.jfx.control.tree.drag.TreeDropListener;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.api.ModifiableTypeInstance;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.types.properties.NameProperty;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.validator.api.ValidationMessage;

public class StructureGUIManager implements ArtifactGUIManager<DefinedStructure> {
	
	private MainController controller;
	
	private EventDispatcher dispatcher = new EventDispatcherImpl();

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
					RepositoryEntry entry = ((RepositoryEntry) target.itemProperty().get()).createNode(name, getArtifactManager(), true);
					DefinedStructure structure = new DefinedStructure();
					structure.setName("root");
					getArtifactManager().save(entry, structure);
					controller.getRepositoryBrowser().refresh();
					Tab tab = controller.newTab(entry.getId(), instance);
					AnchorPane pane = new AnchorPane();
					tab.setContent(pane);
					display(controller, pane, structure);
					instance.setEntry(entry);
					instance.setStructure(structure);
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
		display(controller, pane, new RootElementWithPush(structure, true), entry.isEditable(), false);
		return structure;
	}
	
	public void display(final MainController controller, Pane pane, Structure structure) throws IOException, ParseException {
		display(controller, pane, new RootElementWithPush(structure, true), true, false);
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
			});
		tree.rootProperty().set(new ElementTreeItem(element, null, isEditable, allowNonLocalModification));
		tree.setClipboardHandler(new ElementClipboardHandler(tree));
		
		tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		HBox allButtons = new HBox();
		// buttons
		final HBox buttons = new HBox();
		if (customButtons != null && customButtons.length > 0) {
			allButtons.getChildren().addAll(Arrays.asList(customButtons));
		}
		buttons.getChildren().add(createAddButton(tree, Structure.class));
		buttons.getChildren().add(createAddButton(tree, String.class));
		buttons.getChildren().add(createAddButton(tree, Date.class));
		buttons.getChildren().add(createAddButton(tree, Boolean.class));
		buttons.getChildren().add(createAddButton(tree, Integer.class));
		buttons.getChildren().add(createAddButton(tree, Long.class));
		buttons.getChildren().add(createAddButton(tree, Float.class));
		buttons.getChildren().add(createAddButton(tree, Double.class));
		buttons.getChildren().add(createAddButton(tree, Object.class));
		buttons.getChildren().add(createAddButton(tree, byte[].class));
		allButtons.getChildren().add(buttons);
		
		ScrollPane scrollPane = new ScrollPane();
		VBox vbox = new VBox();
		if (isEditable) {
			vbox.getChildren().add(allButtons);	
		}
		vbox.getChildren().add(tree);
		scrollPane.setContent(vbox);
		pane.getChildren().add(scrollPane);
		
		scrollPane.prefHeightProperty().bind(pane.heightProperty());
		vbox.prefWidthProperty().bind(pane.widthProperty());
		tree.prefWidthProperty().bind(vbox.widthProperty());
		
		tree.getSelectionModel().selectedItemProperty().addListener(new ElementSelectionListener(controller, true));
		
		tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeCell<Element<?>>>() {
			@Override
			public void changed(ObservableValue<? extends TreeCell<Element<?>>> arg0, TreeCell<Element<?>> arg1, TreeCell<Element<?>> arg2) {
				// disable all buttons
				Type type = arg2.getItem().itemProperty().get().getType();
				buttons.disableProperty().set(!(type instanceof ModifiableComplexType) || !arg2.getItem().editableProperty().get());
			}
		});
		
		TreeDragDrop.makeDraggable(tree, new TreeDragListener<Element<?>>() {
			@Override
			public boolean canDrag(TreeCell<Element<?>> cell) {
				// it can not be part of a defined type nor can it be the root
				// also: the source type has to be modifiable because you will be dragging it from there
				return cell.getItem().getParent() != null
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
		// can only make it drag/droppable after it's added because it needs the scene
		TreeDragDrop.makeDroppable(tree, new TreeDropListener<Element<?>>() {
			@Override
			public boolean canDrop(String dataType, TreeCell<Element<?>> target, TreeCell<?> dragged, TransferMode transferMode) {
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
					ModifiableComplexType originalParent = (ModifiableComplexType) draggedElement.getItem().getParent().itemProperty().get().getType();
					// if there are no validation errors when adding, remove the old one
					List<ValidationMessage> messages = newParent.add(draggedElement.getItem().itemProperty().get());
					if (messages.isEmpty()) {
						originalParent.remove(draggedElement.getItem().itemProperty().get());
					}
					else {
						controller.notify(messages.toArray(new ValidationMessage[0]));
					}
					// refresh both, in this specific order! or the parent will be the new one
					dragged.getParent().refresh();
					target.refresh();
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
	
	private Button createAddButton(Tree<Element<?>> tree, Class<?> clazz) {
		Button button = new Button();
		button.setTooltip(new Tooltip(clazz.getSimpleName()));
		button.setGraphic(MainController.loadGraphic(ElementTreeItem.getIcon(getType(clazz))));
		button.addEventHandler(ActionEvent.ACTION, new StructureAddHandler(tree, clazz));
		return button;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<ValidationMessage> addElement(Element<?> element, Type type, String name) {
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
			? new ComplexElementImpl(name, (ComplexType) type, newParent)
			: new SimpleElementImpl(name, (SimpleType<?>) type, newParent)
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
	
}
