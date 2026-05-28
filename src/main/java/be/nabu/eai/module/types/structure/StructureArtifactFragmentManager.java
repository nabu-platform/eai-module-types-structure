package be.nabu.eai.module.types.structure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.api.CreatableArtifactFragmentManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.repository.impl.BaseNodeMetadataArtifactFragmentManager;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.Validation;

public class StructureArtifactFragmentManager extends BaseNodeMetadataArtifactFragmentManager<DefinedStructure> implements CreatableArtifactFragmentManager<DefinedStructure> {

	private static final String STRUCTURE_PATH = "structure.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String EXTENSION_HIERARCHY = "extension-hierarchy";
	private static final String ARTIFACT_TYPE = "structure";

	@Override
	public Entry createArtifact(Entry parent, String name) {
		try {
			RepositoryEntry entry = ((RepositoryEntry) parent).createNode(name, new StructureManager(), true);
			DefinedStructure structure = new DefinedStructure();
			structure.setName(name == null ? "root" : name);
			new StructureManager().save(entry, structure);
			return entry;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<ArtifactFragment> listFragments(DefinedStructure artifact) {
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>(getSharedFragments(artifact));
		fragments.add(new ArtifactFragment() {
			@Override
			public boolean isEditable() {
				return true;
			}

			@Override
			public boolean isRemovable() {
				return false;
			}

			@Override
			public String getPath() {
				return STRUCTURE_PATH;
			}

			@Override
			public String getContent() {
				XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
				marshaller.setIgnoreUnknownSuperTypes(true);
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				try {
					marshaller.marshal(output, artifact);
					return new String(output.toByteArray(), "UTF-8");
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String getContentType() {
				return CONTENT_TYPE;
			}

			@Override
			public String getArtifactId() {
				return artifact.getId();
			}

			@Override
			public String getFragmentType() {
				return "structure";
			}

			@Override
			public Map<String, String> getProperties() {
				Map<String, String> properties = new LinkedHashMap<String, String>();
				String extensionHierarchy = getExtensionHierarchy(artifact);
				if (!extensionHierarchy.isEmpty()) {
					properties.put(EXTENSION_HIERARCHY, extensionHierarchy);
				}
				return properties;
			}

			@Override
			public Long getLastModified() {
				return getFragmentLastModified(artifact.getId(), STRUCTURE_PATH);
			}
		});
		return fragments;
	}

	private String getExtensionHierarchy(DefinedStructure artifact) {
		List<String> hierarchy = new ArrayList<String>();
		Type parent = artifact.getSuperType();
		while (parent != null) {
			if (parent instanceof DefinedStructure) {
				String id = ((DefinedStructure) parent).getId();
				if (id != null && !id.isEmpty()) {
					hierarchy.add(id);
				}
			}
			parent = parent.getSuperType();
		}
		return String.join(",", hierarchy);
	}

	@Override
	public List<Validation<?>> updateFragment(DefinedStructure artifact, String path, String oldContent, String newContent) {
		if (!STRUCTURE_PATH.equals(path)) {
			return super.updateFragment(artifact, path, oldContent, newContent);
		}
		List<Validation<?>> validations = new ArrayList<Validation<?>>();
		be.nabu.eai.repository.api.ResourceEntry entry = (be.nabu.eai.repository.api.ResourceEntry) be.nabu.eai.repository.EAIResourceRepository.getInstance().getEntry(artifact.getId());
		try {
			DefinedStructure updated = StructureManager.parseUpdatedStructure(entry, newContent, artifact, new DefinedStructure(), validations);
			updated.setId(artifact.getId());
			if (!hasErrors(validations)) {
				validations.addAll(new StructureManager().save(entry, updated));
			}
		}
		catch (Exception e) {
			validations.add(new be.nabu.libs.validator.api.ValidationMessage(be.nabu.libs.validator.api.ValidationMessage.Severity.ERROR, e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
		}
		return validations;
	}

	private boolean hasErrors(List<Validation<?>> validations) {
		for (Validation<?> validation : validations) {
			if (validation != null && validation.getSeverity() == be.nabu.libs.validator.api.ValidationMessage.Severity.ERROR) {
				return true;
			}
		}
		return false;
	}



	@Override
	public Class<DefinedStructure> getArtifactClass() {
		return DefinedStructure.class;
	}

	@Override
	public String getArtifactType() {
		return ARTIFACT_TYPE;
	}

	@Override
	public String getArtifactCategory() {
		return "type";
	}

}
