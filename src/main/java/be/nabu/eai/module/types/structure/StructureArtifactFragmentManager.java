package be.nabu.eai.module.types.structure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.impl.BaseNodeMetadataArtifactFragmentManager;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.Validation;

public class StructureArtifactFragmentManager extends BaseNodeMetadataArtifactFragmentManager<DefinedStructure> {

	private static final String STRUCTURE_PATH = "structure.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String EXTENSION_HIERARCHY = "extension-hierarchy";
	private static final String ARTIFACT_TYPE = "structure";

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
	public List<Validation<?>> deleteFragment(DefinedStructure artifact, String path) {
		throw new UnsupportedOperationException("Deleting fragments is not supported for structures");
	}

	@Override
	public List<Validation<?>> createFragment(DefinedStructure artifact, String path, String content) {
		throw new UnsupportedOperationException("Creating fragments is not supported for structures");
	}

	@Override
	public String getGuidelines(List<String> fragmentTypes) {
		List<String> filtered = new ArrayList<String>();
		if (fragmentTypes == null || fragmentTypes.isEmpty()) {
			filtered.add("# Artifact: structure\n\nFragments:\n- `metadata`: repository metadata around the artifact\n- `structure`: the canonical structure definition\n");
			filtered.add(super.getGuidelines(Arrays.asList("metadata")));
			filtered.add("## Fragment: structure\n\n"
				+ "Use `structure.xml` to edit the actual structure definition.\n\n"
				+ "Validation performed on update includes:\n"
				+ "- referenced simple types must resolve\n"
				+ "- referenced complex or defined types must resolve\n"
				+ "- field names must match strict naming rules\n"
				+ "- duplicate field names are rejected\n"
				+ "- properties must be known and supported on the specific node\n\n"
				+ "TypeScript shape:\n"
				+ "```typescript\n"
				+ "export type D=string,J=string,G=string,M=number,S=\"PRIVATE\"|\"PROTECTED\"|\"PUBLIC\"|string,Y=\"NONE\"|\"IN\"|\"OUT\"|string,C=\"LIST\"|\"SET\"|\"MAP\"|string,U=\"CANONICAL\"|\"COMPACT\"|string;\n"
				+ "export interface P{name?:string;namespace?:string;alias?:string;label?:string;comment?:string;foreignName?:string;minOccurs?:number;maxOccurs?:M;qualified?:boolean;elementQualifiedDefault?:boolean;attributeQualifiedDefault?:boolean;nillable?:boolean;defaultValue?:string;pattern?:string;format?:string;timezone?:string;language?:string;country?:string;length?:number;minLength?:number;maxLength?:number;minInclusive?:string|number|boolean;maxInclusive?:string|number|boolean;minExclusive?:string|number|boolean;maxExclusive?:string|number|boolean;totalDigits?:number;fractionDigits?:number;epsilon?:number;generated?:boolean;temporary?:boolean;environmentSpecific?:boolean;raw?:boolean;matrix?:boolean;validate?:boolean;primaryKey?:boolean;foreignKey?:string;unique?:boolean;indexed?:boolean;identifiable?:boolean;translatable?:boolean;secret?:boolean;token?:boolean;collectionName?:string;collectionFormat?:C;collectionCrudProvider?:string;scope?:S;synchronized?:never;synchronization?:Y;actualType?:J;uuidFormat?:U;allow?:string;restrict?:string;duplicate?:string;dynamicName?:string;persister?:string;enricher?:string;period?:string}\n"
				+ "export interface GM{name:string}\n"
				+ "export interface SB extends P{type:J|D;enumerations?:string[]}\n"
				+ "export interface F extends SB{t:\"field\"}\n"
				+ "export interface A extends SB{t:\"attribute\"}\n"
				+ "export interface IB extends P{t:\"structure\";superType?:D;children?:N[]}\n"
				+ "export interface SC extends IB{type?:undefined;definition?:undefined;enumerations?:never}\n"
				+ "export interface SS extends IB{type:J;definition?:undefined;enumerations?:string[]}\n"
				+ "export interface SR extends P{t:\"structure\";definition:D;type?:J;superType?:never;children?:never;enumerations?:never}\n"
				+ "export type SD=SC|SS|SR;\n"
				+ "export type N=A|F|SD;\n"
				+ "export interface Doc{root:SC|SS}\n"
				+ "```\n\n"
				+ "Example:\n"
				+ "```xml\n"
				+ "<structure name=\"customer\">\n"
				+ "\t<field name=\"id\" type=\"java.lang.String\" minOccurs=\"1\"/>\n"
				+ "\t<field name=\"email\" type=\"java.lang.String\" pattern=\".+@.+\"/>\n"
				+ "\t<structure name=\"address\">\n"
				+ "\t\t<field name=\"street\" type=\"java.lang.String\"/>\n"
				+ "\t</structure>\n"
				+ "</structure>\n"
				+ "```" );
			return String.join("\n\n", filtered);
		}
		filtered.add("# Artifact: structure\n");
		String metadataGuidance = super.getGuidelines(fragmentTypes);
		if (metadataGuidance != null) {
			filtered.add(metadataGuidance);
		}
		if (fragmentTypes.contains("structure")) {
			filtered.add("## Fragment: structure\n\n"
				+ "Use `structure.xml` to edit the canonical structure definition.\n\n"
				+ "Updates validate references, simple types, field names, duplicate fields and supported properties before saving.");
		}
		return filtered.isEmpty() ? null : String.join("\n\n", filtered);
	}

	@Override
	public Class<DefinedStructure> getArtifactClass() {
		return DefinedStructure.class;
	}

	@Override
	public String getArtifactType(DefinedStructure artifact) {
		return ARTIFACT_TYPE;
	}

}
