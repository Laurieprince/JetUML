package org.jetuml.persistence;

import static org.jetuml.application.ApplicationResources.RESOURCES;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.Properties;
import org.jetuml.diagram.Property;
import org.jetuml.diagram.edges.ConstructorEdge;
import org.jetuml.diagram.nodes.CallNode;
import org.jetuml.diagram.nodes.PointNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SchemaValidator
{
	private static final String PREFIX_NODES = "org.jetuml.diagram.nodes.";
	private static final String PREFIX_EDGES = "org.jetuml.diagram.edges.";

	private JSONObject aJsonObject;
	private ValidationContext aValidationContext;
	private DiagramType aDiagramType;
	private List<String> aPrototypes;
	private static Set<Integer> aNodeIds;

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
			aValidationContext
					.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "diagram"));
			return false;
		}

		String diagramName = aJsonObject.getString("diagram");
		if (!DiagramType.isValidName(diagramName))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.invalid_diagram_name"), diagramName));
			return false;
		}
		else
		{
			aDiagramType = DiagramType.fromName(aJsonObject.getString("diagram"));
			aPrototypes = aDiagramType.getPrototypes().stream().map(x -> x.getClass().getSimpleName()).collect(Collectors.toList());
			// TODO: aPrototypes do not include CallNode, ConstructorEdge and PointNode
			aPrototypes.add(CallNode.class.getSimpleName());
			aPrototypes.add(ConstructorEdge.class.getSimpleName());
			aPrototypes.add(PointNode.class.getSimpleName());
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
			try
			{
				JSONObject object = nodes.getJSONObject(i);

				if (!validateNodeBaseProperties(object))
					continue;

				int nodeId = object.getInt("id");
				if (!aNodeIds.contains(nodeId)) aNodeIds.add(nodeId);
				else aValidationContext.addError(String.format(RESOURCES.getString("error.validator.duplicate_id"), nodeId));

				validateNodeProperties(object);
			} 
			catch (ReflectiveOperationException exception)
			{
				aValidationContext.addError(RESOURCES.getString("error.validator.instance_serialize_object"));
				return;
			}
		}
	}

	private boolean validateNodeBaseProperties(JSONObject pObject)
	{
		if (pObject.has("id") && pObject.has("type") && pObject.has("x") && pObject.has("y"))
		{
			return true;
		}
		if (!pObject.has("id"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "id"));
		}
		if (!pObject.has("type"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "type"));
		}
		if (!pObject.has("x"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "x"));
		}
		if (!pObject.has("y"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "y"));
		}
		return false;
	}

	private void validateNodeProperties(JSONObject pObject) throws ReflectiveOperationException
	{
		String nodeType = pObject.getString("type");
		if (!aPrototypes.contains(nodeType))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.undefined_property"), nodeType, aDiagramType.getName()));
			return;
		}

		Class<?> nodeClass = Class.forName(PREFIX_NODES + nodeType);

		Node node = (Node) nodeClass.getDeclaredConstructor().newInstance();

		for (Property property : node.properties())
		{
			if (!pObject.has(property.name().external()))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.undefined_property"),
						property.name().external(), nodeClass.getName()));
			}
		}
	}

	private void validateNodeChildren()
	{
		JSONArray nodes = aJsonObject.getJSONArray("nodes");
		for (int i = 0; i < nodes.length(); i++)
		{
			JSONObject object = nodes.getJSONObject(i);
			if (object.has("children"))
			{
				// TODO: Validate a node can be a child
				JSONArray children = object.getJSONArray("children");
				for (int j = 0; j < children.length(); j++)
				{
					if (!aNodeIds.contains(children.getInt(j)))
					{
						aValidationContext.addError(String.format(RESOURCES.getString("error.validator.node_id_missing"), "child", children.getInt(j)));
					}
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
			try
			{
				JSONObject object = edges.getJSONObject(i);

				if (!validateEdgeBaseProperties(object)) continue;
				
				validateEdgeEndPoints(object);
				validateEdgeProperties(object);
			}
			catch (ReflectiveOperationException exception)
			{
				aValidationContext.addError(RESOURCES.getString("error.validator.instance_serialize_object"));
				return;
			}
		}
	}

	private boolean validateEdgeBaseProperties(JSONObject pObject)
	{
		if (pObject.has("type") && pObject.has("start") && pObject.has("end"))
		{
			return true;
		}
		if (!pObject.has("type"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "type"));
		}
		if (!pObject.has("start"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "start"));
		}
		if (!pObject.has("end"))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.missing_property"), "end"));
		}
		return false;
	}

	private void validateEdgeEndPoints(JSONObject pObject)
	{
		int startNodeId = pObject.getInt("start");
		if (!aNodeIds.contains(startNodeId))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.node_id_missing"), "start", startNodeId));
		}

		int endNodeId = pObject.getInt("end");
		if (!aNodeIds.contains(endNodeId))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.node_id_missing"), "end", endNodeId));
		}
	}

	private void validateEdgeProperties(JSONObject pObject) throws ReflectiveOperationException
	{
		String edgeType = pObject.getString("type");
		if (!aPrototypes.contains(edgeType))
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.undefined_property"), edgeType, aDiagramType.getName()));
			return;
		}
		
		Class<?> edgeClass = Class.forName(PREFIX_EDGES + edgeType);

		Properties properties = ((Edge) edgeClass.getDeclaredConstructor().newInstance()).properties();

		for (Property property : properties)
		{
			if (!pObject.has(property.name().external())) 
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.undefined_property"),
						property.name().external(), edgeClass.getName()));
			}
		}
	}
}
