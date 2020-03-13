package be.nabu.eai.module.types.structure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.RepositoryServiceInterfaceResolver;
import be.nabu.eai.repository.RepositoryServiceResolver;
import be.nabu.eai.repository.RepositorySimpleTypeWrapper;
import be.nabu.eai.repository.RepositoryTypeResolver;
import be.nabu.eai.repository.api.ArtifactManager;
import be.nabu.eai.repository.api.BrokenReferenceArtifactManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.artifacts.ArtifactResolverFactory;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.converter.MultipleConverter;
import be.nabu.libs.converter.base.ConverterImpl;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.pojo.converters.StringToDefinedService;
import be.nabu.libs.services.pojo.converters.StringToDefinedServiceInterface;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.MultipleDefinedTypeResolver;
import be.nabu.libs.types.MultipleSimpleTypeWrapper;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableType;
import be.nabu.libs.types.api.ModifiableTypeInstance;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.converters.StringToDefinedType;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.definition.xml.XMLDefinitionUnmarshaller;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.structure.SuperTypeProperty;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class StructureManager implements ArtifactManager<DefinedStructure>, BrokenReferenceArtifactManager<DefinedStructure> {

	@Override
	public DefinedStructure load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		DefinedStructure structure = (DefinedStructure) parse(entry, "structure.xml", messages);
		structure.setId(entry.getId());
		return structure;
	}

	public static XMLDefinitionUnmarshaller getLocalizedUnmarshaller(Entry entry) {
		XMLDefinitionUnmarshaller unmarshaller = new XMLDefinitionUnmarshaller();
		// if we are not in the "main" repository (which has already injected resolvers), add resolvers for the actual repository
		// these will be used for example to find the interfaces related to this repository
		if (!entry.getRepository().equals(EAIResourceRepository.getInstance())) {
			ConverterImpl converter = new ConverterImpl();
			converter.addProvider(new StringToDefinedType(new RepositoryTypeResolver(entry.getRepository())));
			converter.addProvider(new StringToDefinedServiceInterface(new RepositoryServiceInterfaceResolver(entry.getRepository())));
			converter.addProvider(new StringToDefinedService(new RepositoryServiceResolver(entry.getRepository())));
			MultipleDefinedTypeResolver typeResolver = new MultipleDefinedTypeResolver(Arrays.asList(
				new RepositoryTypeResolver(entry.getRepository()),
				DefinedTypeResolverFactory.getInstance().getResolver()
			));
			unmarshaller.setConverter(new MultipleConverter(Arrays.asList(converter, ConverterFactory.getInstance().getConverter())));
			MultipleSimpleTypeWrapper simpleTypeWrapper = new MultipleSimpleTypeWrapper(Arrays.asList(
				new RepositorySimpleTypeWrapper(entry.getRepository()),
				SimpleTypeWrapperFactory.getInstance().getWrapper()
			));
			unmarshaller.setSimpleTypeWrapper(simpleTypeWrapper);
			unmarshaller.setTypeResolver(typeResolver);
		}
		return unmarshaller;
	}
	
	public static Structure parse(ResourceEntry entry, String name) throws FileNotFoundException, IOException, ParseException {
		return parse(entry, name, null);
	}
	
	public static Structure parse(ResourceEntry entry, String name, List<Validation<?>> validations) throws FileNotFoundException, IOException, ParseException {
		Resource resource = entry.getContainer().getChild(name);
		if (resource == null) {
			throw new FileNotFoundException("Can not find: " + name);
		}
		XMLDefinitionUnmarshaller unmarshaller = getLocalizedUnmarshaller(entry);
		unmarshaller.setIgnoreUnknown(validations != null);
		ReadableContainer<ByteBuffer> readable = new ResourceReadableContainer((ReadableResource) resource);
		try {
			unmarshaller.setIdToUnmarshal(entry.getId());
			// evil cast!
			Structure structure = (Structure) unmarshaller.unmarshal(IOUtils.toInputStream(readable));
			if (validations != null) {
				for (String ignoredReference : unmarshaller.getIgnoredReferences()) {
					validations.add(new ValidationMessage(Severity.ERROR, "Could not find reference '" + ignoredReference + "', it has been removed"));
				}
			}
			return structure;
		}
		finally {
			readable.close();
		}
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, DefinedStructure artifact) throws IOException {
		return this.saveContent(entry, artifact);
	}
	
	public List<Validation<?>> saveContent(ResourceEntry entry, ComplexType artifact) throws IOException {
		List<Validation<?>> messages = format(entry, artifact, "structure.xml");
		if (entry instanceof ModifiableNodeEntry) {
			((ModifiableNodeEntry) entry).updateNode(getComplexReferences(artifact));
		}
		return messages;
	}

	public static List<Validation<?>> format(ResourceEntry entry, ComplexType artifact, String name) throws IOException {
		Resource resource = entry.getContainer().getChild(name);
		if (resource == null) {
			resource = ((ManageableContainer<?>) entry.getContainer()).create(name, "application/xml");
		}
		WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) resource);
		try {
			XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
			marshaller.marshal(IOUtils.toOutputStream(writable), artifact);
			return new ArrayList<Validation<?>>();
		}
		finally {
			writable.close();
		}
	}

	@Override
	public Class<DefinedStructure> getArtifactClass() {
		return DefinedStructure.class;
	}

	@Override
	public List<String> getReferences(DefinedStructure artifact) throws IOException {
		return getComplexReferences(artifact);
	}
	
	public static List<String> getComplexReferences(ComplexType type) {
		return getComplexReferences(type, false);
	}
	
	/**
	 * The boolean includeRecursive allows you to get a full picture of all references, this includes those made by a supertype and/or those made by something you reference
	 */
	public static List<String> getComplexReferences(ComplexType type, boolean includeRecursive) {
		List<String> references = new ArrayList<String>();
		getReferences(type, references, includeRecursive);
		return references;
	}
	
	private static void getReferences(ComplexType type, List<String> references, boolean includeRecursive) {
		if (type.getSuperType() != null && type.getSuperType() instanceof Artifact) {
			String id = ((Artifact) type.getSuperType()).getId();
			if (!references.contains(id)) {
				references.add(id);
			}
		}
		// if we want recursive resolving (to get a full picture of all references, include the supertype
		if (includeRecursive && type.getSuperType() instanceof ComplexType) {
			getReferences((ComplexType) type.getSuperType(), references, includeRecursive);
		}
		// only local children, don't loop over supertype children (this is handled by the above if relevant)
		for (Element<?> child : type) {
			// if it is a reference, don't recurse unless specifically asked
			if (child.getType() instanceof Artifact) {
				Artifact artifact = (Artifact) child.getType();
				if (!references.contains(artifact.getId())) {
					references.add(artifact.getId());
					// we only want to recurse if the artifact itself was not already added and scanned, this to prevent circular reference problems
					if (includeRecursive && child.getType() instanceof ComplexType) {
						getReferences((ComplexType) child.getType(), references, includeRecursive);						
					}
				}
			}
			else if (child.getType() instanceof ComplexType) {
				getReferences((ComplexType) child.getType(), references, includeRecursive);
			}
		}
	}
	
	public static List<Validation<?>> updateReferences(ComplexType type, String from, String to) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		if (type.getSuperType() != null && type.getSuperType() instanceof Artifact) {
			String id = ((Artifact) type.getSuperType()).getId();
			if (from.equals(id)) {
				if (!(type instanceof ModifiableType)) {
					messages.add(new ValidationMessage(Severity.ERROR, "Could not update supertype from '" + from + "' to '" + to + "'"));
				}
				else {
					Artifact newSuperType = ArtifactResolverFactory.getInstance().getResolver().resolve(to);
					if (!(newSuperType instanceof Type)) {
						messages.add(new ValidationMessage(Severity.ERROR, "Not a type: " + to));	
					}
					else {
						((ModifiableType) type).setProperty(new ValueImpl<Type>(SuperTypeProperty.getInstance(), (Type) newSuperType));
					}
				}
			}
		}
		for (Element<?> child : type) {
			if (child.getType() instanceof Artifact) {
				Artifact artifact = (Artifact) child.getType();
				if (from.equals(artifact.getId())) {
					if (!(child instanceof ModifiableTypeInstance)) {
						messages.add(new ValidationMessage(Severity.ERROR, "Could not update referenced type from '" + from + "' to '" + to + "'"));	
					}
					else {
						Artifact newType = ArtifactResolverFactory.getInstance().getResolver().resolve(to);
						if (!(newType instanceof Type)) {
							messages.add(new ValidationMessage(Severity.ERROR, "Not a type: " + to));	
						}
						else {
							((ModifiableTypeInstance) child).setType((Type) newType);
						}
					}
				}
			}
			else if (child.getType() instanceof ComplexType) {
				messages.addAll(updateReferences((ComplexType) child.getType(), from, to));
			}
		}
		return messages;
	}

	@Override
	public List<Validation<?>> updateReference(DefinedStructure artifact, String from, String to) throws IOException {
		return updateReferences(artifact, from, to);
	}

	@Override
	public List<Validation<?>> updateBrokenReference(ResourceContainer<?> container, String from, String to) throws IOException {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		Resource child = container.getChild("structure.xml");
		// we don't know the name of the configuration file but we do know that it is an xml
		// rewrite any and all xmls
		// this might be troublesome for some particular extensions but they need to override the behavior then
		if (child != null) {
			EAIRepositoryUtils.updateBrokenReference(child, from, to, Charset.forName("UTF-8"));
		}
		return messages;
	}

}
