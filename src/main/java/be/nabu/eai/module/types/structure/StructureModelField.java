package be.nabu.eai.module.types.structure;

import java.util.List;

public interface StructureModelField {
	public String getAlias();
	public String getCollectionName();
	public String getCollectionCrudProvider();
	public String getFormat();
	// the name of the field
	public String getName();
	// a fully qualified type, e.g. java.lang.String. if you send a base type (e.g. string) we will try to resolve as a simple type
	public String getType();
	// a human readable title for this field
	public String getTitle();
	// a human readable description of the field
	public String getDescription();
	// optionality
	public Integer getMinOccurs();
	public Integer getMaxOccurs();

	// child items
	public List<StructureModelField> getFields();
	
	// whether or not it is the primary key
	public Boolean getPrimaryKey();
}
