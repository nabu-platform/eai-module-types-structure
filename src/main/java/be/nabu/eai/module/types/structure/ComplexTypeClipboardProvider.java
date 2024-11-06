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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import be.nabu.eai.developer.api.ClipboardProvider;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.definition.xml.XMLDefinitionMarshaller;
import be.nabu.libs.types.definition.xml.XMLDefinitionUnmarshaller;
import be.nabu.libs.types.structure.Structure;

public class ComplexTypeClipboardProvider implements ClipboardProvider<ComplexType> {

	@Override
	public String getDataType() {
		return "anonymous-complex-type";
	}

	@Override
	public String serialize(ComplexType instance) {
		XMLDefinitionMarshaller marshaller = new XMLDefinitionMarshaller();
		marshaller.setIgnoreUnknownSuperTypes(true);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			marshaller.marshal(output, instance);
			return new String(output.toByteArray(), "UTF-8");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ComplexType deserialize(String content) {
		XMLDefinitionUnmarshaller unmarshaller = new XMLDefinitionUnmarshaller();
		try {
			// don't want a defined structure
			Structure structure = new Structure();
			unmarshaller.unmarshal(new ByteArrayInputStream(content.getBytes("UTF-8")), structure);
			return structure;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<ComplexType> getClipboardClass() {
		return ComplexType.class;
	}

}
