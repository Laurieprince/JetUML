/*******************************************************************************
 * JetUML - A desktop application for fast UML diagramming.
 *
 * Copyright (C) 2020, 2021 by McGill University.
 *     
 * See: https://github.com/prmr/JetUML
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *******************************************************************************/
package org.jetuml.persistence;

import static org.jetuml.application.ApplicationResources.RESOURCES;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetuml.JetUML;
import org.jetuml.application.Version;
import org.jetuml.diagram.DiagramElement;
import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.Properties;
import org.jetuml.diagram.Property;
import org.jetuml.persistence.SchemaProperties.SchemaProperty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Validates the Diagram JSONObject.
 */
public class SchemaValidator extends AbstractValidator<JSONObject>
{
	private JSONObject aJsonObject;
	private DiagramType aDiagramType;
	private static Set<Integer> aNodeIds;
	private Map<String, DiagramElement> aElements = new HashMap<>();
	
	public SchemaValidator(JSONObject pJsonObject)
	{
		assert pJsonObject != null;
		aJsonObject = pJsonObject;
	}

	public void validate()
	{
		try 
		{
			if (!validateDiagramRequiredProperties()) return;
			if (!validateDiagramType()) return;
			aDiagramType.getPrototypes().stream().forEach(x -> aElements.put(x.getClass().getSimpleName(),x));
			validateNodes();
			validateChildrenNodes();
			validateEdges();
			
			if(!isValid())
			{
				Version version = Version.parse(aJsonObject.getString("version"));
				if(!version.compatibleWith(JetUML.VERSION))
				{
					addError(String.format(RESOURCES.getString("error.validator.version"),  version.toString(), JetUML.VERSION.toString()));
				}
			}
		}
		catch (JSONException exception)
		{
			addError(RESOURCES.getString("error.validator.instance_serialize_object"));
		}
	}
	
	/**
	 * @return True if the Diagram required properties are present.
	 */
	private boolean validateDiagramRequiredProperties()
	{
		for(SchemaProperty property : SchemaProperties.diagramProperties())
		{
			if(!aJsonObject.has(property.name()))
			{
				addError(String.format(RESOURCES.getString("error.validator.missing_property"), property));
				return false;
			}
		}
		return true;
	}

	/**
	 * @return True if the type 
	 */
	private boolean validateDiagramType()
	{
		String diagramName = aJsonObject.getString("diagram");
		try
		{
			aDiagramType = DiagramType.fromName(diagramName);
			return true;
		}
		catch(IllegalArgumentException exception)
		{
			addError(String.format(RESOURCES.getString("error.validator.invalid_diagram_name"), diagramName));
			return false;
		}
	}

	private void validateNodes()
	{
		aNodeIds = new HashSet<Integer>();

		JSONArray nodes = aJsonObject.getJSONArray("nodes");

		for (int i = 0; i < nodes.length(); i++)
		{
			JSONObject object = nodes.getJSONObject(i);

			if (!validateNodeBaseProperties(object)) continue;
			
			// Validate the node id is unique
			int nodeId = object.getInt("id");
			if (aNodeIds.contains(nodeId))
			{
				addError(String.format(RESOURCES.getString("error.validator.duplicate_id"), nodeId));
			}
			else
			{
				aNodeIds.add(nodeId);
			}
			
			validateDiagramElement(object, Node.class);
		}
	}

	private boolean validateNodeBaseProperties(JSONObject pObject)
	{
		boolean result = true;
		
		for(SchemaProperty nodeProperty : SchemaProperties.nodeBaseProperties())
		{
			if(!pObject.has(nodeProperty.name()))
			{
				addError(String.format(RESOURCES.getString("error.validator.missing_property"), nodeProperty.name()));
				result = false;
			}
		}
		
		return result;
	}


	// validates the children nodes are present
	// validates a node allows children
	private void validateChildrenNodes()
	{
		JSONArray nodes = aJsonObject.getJSONArray("nodes");
		for (int i = 0; i < nodes.length(); i++)
		{
			JSONObject object = nodes.getJSONObject(i);
			var elementType = object.get("type");
			DiagramElement diagramElement = aElements.get(elementType);
			if (object.has("children"))
			{
				if(Node.class.isAssignableFrom(diagramElement.getClass()))
				{
					if(((Node)diagramElement).allowsChildren()) {
						JSONArray children = object.getJSONArray("children");
						for (int j = 0; j < children.length(); j++)
						{
							nodeIdExists(children.getInt(j), "children");
						}
					}
					else
					{
						addError(String.format(RESOURCES.getString("error.validator.element_does_not_allow_children"), elementType));
					}
				}
				else
				{
					addError(String.format(RESOURCES.getString("error.validator.element_does_not_allow_children"), elementType));
				}
			}
		}
	}

	private void validateEdges()
	{
		JSONArray edges = aJsonObject.getJSONArray("edges");

		for (int i = 0; i < edges.length(); i++)
		{
			JSONObject object = edges.getJSONObject(i);

			if (!validateEdgeBaseProperties(object)) continue;
			
			// Validate edge end points
			nodeIdExists(object.getInt("start"), "start");
			nodeIdExists(object.getInt("end"), "end");
			
			validateDiagramElement(object, Edge.class);
		}
	}

	private boolean validateEdgeBaseProperties(JSONObject pObject)
	{
		boolean result = true;

		for(SchemaProperty edgeProperty : SchemaProperties.edgeBaseProperties())
		{
			if(!pObject.has(edgeProperty.name()))
			{
				addError(String.format(RESOURCES.getString("error.validator.missing_property"), edgeProperty.name()));
				result = false;
			}
		}
		return result;
	}

	private void nodeIdExists(int pNodeId, String pField)
	{
		if (!aNodeIds.contains(pNodeId))
		{
			addError(String.format(RESOURCES.getString("error.validator.node_id_missing"), pField , pNodeId));
		}
	}
	
	private void validateDiagramElement(JSONObject pObject, Class<? extends DiagramElement> elementClass)
	{
		String elementType = pObject.getString("type");
		
		Optional<DiagramElement> diagramElement = Optional.ofNullable(aElements.get(elementType));
		
		if (!diagramElement.isPresent())
		{
			addError(String.format(RESOURCES.getString("error.validator.undefined_property"), elementType, aDiagramType.getName()));
			return;
		}
		
		if(!elementClass.isAssignableFrom(diagramElement.get().getClass()))
		{
			addError(String.format(RESOURCES.getString("error.validator.invalid_element_type"), elementType, elementClass.getSimpleName()));
			return;
		}
		
		Properties properties = diagramElement.get().properties();

		for (Property property : properties)
		{
			if (!pObject.has(property.name().external()))
			{
				addError(String.format(RESOURCES.getString("error.validator.undefined_property"), property.name().external(), elementType));
			}
		}
	}
}
