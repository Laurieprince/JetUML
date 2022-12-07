package org.jetuml.persistence;

import static org.jetuml.application.ApplicationResources.RESOURCES;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetuml.diagram.DiagramElement;
import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.Properties;
import org.jetuml.diagram.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SchemaValidator
{
	private JSONObject aJsonObject;
	private ValidationContext aValidationContext;
	private DiagramType aDiagramType;
	private static Set<Integer> aNodeIds;
	private Map<String, DiagramElement> aElementsMap = new HashMap<>();
	
	public SchemaValidator(ValidationContext pvalidationContext)
	{
		assert pvalidationContext != null && pvalidationContext.JSONObject() != null;
		aValidationContext = pvalidationContext;
		aJsonObject = pvalidationContext.JSONObject();
	}

	public void validate()
	{
		try 
		{
			if (!validateDiagram()) return;

			validateNodes();
			validateNodeChildren();
			validateEdges();
		}
		catch (JSONException | IllegalArgumentException exception)
		{
			aValidationContext.addError(RESOURCES.getString("error.validator.instance_serialize_object"));
		}
	}

	private boolean validateDiagram()
	{
		if (!aJsonObject.has("diagram"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "diagram"));
			return false;
		}

		String diagramName = aJsonObject.getString("diagram");
		try
		{
			aDiagramType = DiagramType.fromName(diagramName);
			aDiagramType.getPrototypes().stream().forEach(x -> aElementsMap.put(x.getClass().getSimpleName(),x));
		}
		catch(IllegalArgumentException e)
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_diagram_name"), diagramName));
			return false;
		}
		return true;
	}

	private void validateNodes()
	{
		aNodeIds = new HashSet<Integer>();
		if (!aJsonObject.has("nodes"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "nodes"));
			return;
		}

		JSONArray nodes = aJsonObject.getJSONArray("nodes");

		for (int i = 0; i < nodes.length(); i++)
		{
			JSONObject object = nodes.getJSONObject(i);

			if (!validateNodeBaseProperties(object))
				continue;

			int nodeId = object.getInt("id");
			if (!aNodeIds.contains(nodeId)) aNodeIds.add(nodeId);
			else aValidationContext.addError(String.format(RESOURCES.getString("error.validator.duplicate_id"), nodeId));
			validateDiagramElement(object, Node.class);
		}
	}

	private boolean validateNodeBaseProperties(JSONObject pObject)
	{
		if (pObject.has("id") && pObject.has("type") && pObject.has("x") && pObject.has("y"))
		{
			return true;
		}
		
		for(NodeBaseProperties nodeBaseProperty : NodeBaseProperties.values())
		{
			if(!pObject.has(nodeBaseProperty.getLabel()))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), nodeBaseProperty.getLabel()));
			}
		}
		
		return false;
	}

	private void validateNodeChildren()
	{
		JSONArray nodes = aJsonObject.getJSONArray("nodes");
		for (int i = 0; i < nodes.length(); i++)
		{
			JSONObject object = nodes.getJSONObject(i);
			var elementType = object.get("type");
			DiagramElement diagramElement = aElementsMap.get(elementType);
			if (object.has("children"))
			{
				if(Node.class.isAssignableFrom(diagramElement.getClass()))
				{
					JSONArray children = object.getJSONArray("children");
					for (int j = 0; j < children.length(); j++)
					{
						nodeIdExists(children.getInt(j), "children");
					}
				}
				else
				{
					aValidationContext.addError(String.format(RESOURCES.getString("error.validator.element_does_not_allow_children"), elementType));
				}
			}
		}
	}

	private void validateEdges()
	{
		if (!aJsonObject.has("edges"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "edges"));
			return;
		}

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
		if (pObject.has("type") && pObject.has("start") && pObject.has("end"))
		{
			return true;
		}

		for(EdgeBaseProperties edgeBaseProperty : EdgeBaseProperties.values())
		{
			if(!pObject.has(edgeBaseProperty.getLabel()))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), edgeBaseProperty.getLabel()));
			}
		}
		return false;
	}

	private void nodeIdExists(int pNodeId, String pField)
	{
		if (!aNodeIds.contains(pNodeId))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.node_id_missing"), pField , pNodeId));
		}
	}
	
	private void validateDiagramElement(JSONObject pObject, Class<? extends DiagramElement> elementClass)
	{
		String elementType = pObject.getString("type");
		
		Optional<DiagramElement> diagramElement = Optional.of(aElementsMap.get(elementType));
		
		if (!diagramElement.isPresent())
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.undefined_property"), elementType, aDiagramType.getName()));
			return;
		}
		
		if(!elementClass.isAssignableFrom(diagramElement.get().getClass()))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_element_type"), elementType, elementClass.getSimpleName()));
			return;
		}
		
		Properties properties = diagramElement.get().properties();

		for (Property property : properties)
		{
			if (!pObject.has(property.name().external())) 
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.undefined_property"),
						property.name().external(), elementType));
			}
		}
	}
}
