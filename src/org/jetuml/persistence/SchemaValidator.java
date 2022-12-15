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
			if (!validateDiagramRequiredProperties()) return;
			if (!validateDiagramType()) return;
			validateRootNodes();
			validateChildrenNodes();
			validateEdges();
			
			if(!aValidationContext.isValid())
			{
				Version version = Version.parse(aJsonObject.getString("version"));
				if(!version.compatibleWith(JetUML.VERSION))
				{
					aValidationContext.addError(String.format(RESOURCES.getString("error.validator.version"),  version.toString(), JetUML.VERSION.toString()));
				}
			}
		}
		catch (JSONException | IllegalArgumentException exception)
		{
			aValidationContext.addError(RESOURCES.getString("error.validator.instance_serialize_object"));
		}
	}
	
	private boolean validateDiagramRequiredProperties()
	{
		for(SchemaProperty property : SchemaProperties.diagramProperties())
		{
			if(!aJsonObject.has(property.name()))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), property));
				return false;
			}
		}
		return true;
	}

	private boolean validateDiagramType()
	{
		String diagramName = aJsonObject.getString("diagram");
		try
		{
			aDiagramType = DiagramType.fromName(diagramName);
			aDiagramType.getPrototypes().stream().forEach(x -> aElementsMap.put(x.getClass().getSimpleName(),x));
			return true;
		}
		catch(IllegalArgumentException e)
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_diagram_name"), diagramName));
			return false;
		}
	}

	private void validateRootNodes()
	{
		aNodeIds = new HashSet<Integer>();

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
		
		for(SchemaProperty nodeProperty : SchemaProperties.nodeBaseProperties())
		{
			if(!pObject.has(nodeProperty.name()))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), nodeProperty.name()));
			}
		}
		
		return false;
	}

	// TODO: validate a node can be a child
	// TODO: validate a node only has one parent
	// validates the children nodes are present
	// validates a node allows children
	private void validateChildrenNodes()
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
					Node parentNode = (Node)diagramElement;
					if(parentNode.allowsChildren()) {
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
				else
				{
					aValidationContext.addError(String.format(RESOURCES.getString("error.validator.element_does_not_allow_children"), elementType));
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
		if (pObject.has("type") && pObject.has("start") && pObject.has("end"))
		{
			return true;
		}

		for(SchemaProperty edgeProperty : SchemaProperties.edgeBaseProperties())
		{
			if(!pObject.has(edgeProperty.name()))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), edgeProperty.name()));
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
		
		Optional<DiagramElement> diagramElement = Optional.ofNullable(aElementsMap.get(elementType));
		
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
