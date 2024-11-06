/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
