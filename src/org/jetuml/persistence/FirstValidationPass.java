package org.jetuml.persistence;


import java.util.HashSet;
import java.util.Set;

import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.Properties;
import org.jetuml.diagram.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FirstValidationPass {
	private static final String PREFIX_NODES = "org.jetuml.diagram.nodes.";
	private static final String PREFIX_EDGES = "org.jetuml.diagram.edges.";
	
	private static final String MISSING_PROPERTY_ERROR = "Required property is missing: \"%s\".";
	private static final String UNDEFINED_PROPERTY_ERROR = "\"%s\" is undefined for %s.";
	
	private static Set<Integer> nodeIds;
	
	private FirstValidationPass() {}
	
	public static LoadedDiagramFile validate(JSONObject pDiagram)
	{
		assert pDiagram != null;
		nodeIds = new HashSet<Integer>();
		try
		{
			LoadedDiagramFile loadedDiagramFile = new LoadedDiagramFile();
			
			if(!validateDiagram(loadedDiagramFile, pDiagram))
			{
				return loadedDiagramFile;
			}
			DiagramType diagramType = DiagramType.fromName(pDiagram.getString("diagram"));
			
			validateNodes(loadedDiagramFile, pDiagram, diagramType);
			validateNodeChildren(loadedDiagramFile, pDiagram, diagramType);
			validateEdges(loadedDiagramFile, pDiagram, diagramType);

			return loadedDiagramFile;
		} 
		catch (JSONException | IllegalArgumentException exception)
		{
			throw new DeserializationException("Cannot decode serialized object", exception);
		}
	}
	
	private static boolean validateDiagram(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject)
	{
		if(!pObject.has("diagram"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "diagram"));
			return false;
		}
		
		String diagramName = pObject.getString("diagram");
		if(!DiagramType.isValidName(diagramName)) 
		{
			pLoadedDiagramFile.addError(String.format("Invalid diagram name: %s", diagramName));
			return false;
		}
		return true;
	}
	
	private static void validateNodes(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject, DiagramType diagramType)
	{
		if(!pObject.has("nodes"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "nodes"));
			return ;
		}
		
		JSONArray nodes = pObject.getJSONArray("nodes");
		
		for( int i = 0; i < nodes.length(); i++ )
		{
			try
			{
				JSONObject object = nodes.getJSONObject(i); 
				
				if(!validateNodeBaseProperties(pLoadedDiagramFile, object)) 	
				{
					continue;
				}
				validateNodeId(pLoadedDiagramFile, object);
				validateNodeProperties(pLoadedDiagramFile, object, diagramType);
			} 
			catch (ReflectiveOperationException exception)
			{
				throw new DeserializationException("Cannot instantiate serialized object", exception);
			}
		}
	}
	
	private static boolean validateNodeBaseProperties(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject)
	{	
		if(pObject.has("id") && pObject.has("type") && pObject.has("x") && pObject.has("y"))
		{
			return true;
		}
		if (!pObject.has("id"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "id"));
		}
		if(!pObject.has("type"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "type"));
		}
		if(!pObject.has("x"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "x"));
		}
		if(!pObject.has("y"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "y"));
		}
		return false;
	}
	
	private static void validateNodeId(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject)
	{
		int nodeId = pObject.getInt("id");
		if(nodeIds.contains(nodeId))
		{
			pLoadedDiagramFile.addError("Duplicate node id " + nodeId);
		}
		else
		{
			nodeIds.add(nodeId);
		}
	}
	
	private static void validateNodeProperties(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject, DiagramType diagramType) throws ReflectiveOperationException
	{
		Class<?> nodeClass = Class.forName(PREFIX_NODES + pObject.getString("type")); 
		
		// TODO: Does not include CallNode and ConstructorEdge
		var pList = diagramType.getPrototypes().stream().map(x -> x.getClass()).toList();
		
		if(!pList.contains(nodeClass))
		{
			pLoadedDiagramFile.addError(String.format(UNDEFINED_PROPERTY_ERROR, nodeClass.getName(), diagramType.getName())); 
			return;
		}
		
		Node node = (Node) nodeClass.getDeclaredConstructor().newInstance();

		for( Property property : node.properties() )
		{
			if (!pObject.has(property.name().external()))
			{
				pLoadedDiagramFile.addError(String.format(UNDEFINED_PROPERTY_ERROR, property.name().external(), nodeClass.getName()));
			}
		}
	}
	
	private static void validateNodeChildren(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject, DiagramType diagramType)
	{
		JSONArray nodes = pObject.getJSONArray("nodes");
		for( int i = 0; i < nodes.length(); i++ )
		{
			JSONObject object = nodes.getJSONObject(i);
			if( object.has("children"))
			{
				// TODO: Validate a node can be a child
				
				JSONArray children = object.getJSONArray("children");
				for( int j = 0; j < children.length(); j++ )
				{
					if(!nodeIds.contains(children.getInt(j)))
					{
						pLoadedDiagramFile.addError(String.format("children node id %d is not present.", children.getInt(j)));
					}
				}
			}
		}
	}
	
	private static void validateEdges(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject, DiagramType diagramType)
	{
		if(!pObject.has("edges"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "edges"));
			return ;
		}
		
		JSONArray edges = pObject.getJSONArray("edges");
		
		for (int i = 0; i < edges.length(); i++)
		{
			try
			{
				JSONObject object = edges.getJSONObject(i);

				if(!validateEdgeBaseProperties(pLoadedDiagramFile, object, diagramType))
				{
					continue;
				}
				validateEdgeEndPoints(pLoadedDiagramFile, object); 
				validateEdgeProperties(pLoadedDiagramFile, object, diagramType);
			}
			catch (ReflectiveOperationException exception)
			{
				throw new DeserializationException("Cannot instantiate serialized object", exception);
			}
		}
	}
	
	private static boolean validateEdgeBaseProperties(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject, DiagramType diagramType)
	{
		if(pObject.has("type") && pObject.has("start") && pObject.has("end")) {
			return true;
		}
		if(!pObject.has("type"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "type"));
		}
		if(!pObject.has("start"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "start"));
		} 
		if(!pObject.has("end"))
		{
			pLoadedDiagramFile.addError(String.format(MISSING_PROPERTY_ERROR, "end"));
		}
		return false;
	}
	
	private static void validateEdgeEndPoints(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject)
	{
		int startNodeId = pObject.getInt("start");
		if(!nodeIds.contains(startNodeId)) 
		{
			pLoadedDiagramFile.addError("'start' node " + startNodeId + " is not present in the nodes");
		}
		
		int endNodeId = pObject.getInt("end");
		if(!nodeIds.contains(endNodeId)) 
		{
			pLoadedDiagramFile.addError("'end' node " + endNodeId + " is not present in the nodes");
		}
	}
	
	private static void validateEdgeProperties(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject, DiagramType diagramType) throws ReflectiveOperationException
	{
		Class<?> edgeClass = Class.forName(PREFIX_EDGES + pObject.getString("type"));

		if(!diagramType.getPrototypes().stream().map(x -> x.getClass()).toList().contains(edgeClass))
		{
			pLoadedDiagramFile.addError(String.format(UNDEFINED_PROPERTY_ERROR, edgeClass.getName(), diagramType.getName()));
			return;
		}
		
		Properties properties = ((Edge) edgeClass.getDeclaredConstructor().newInstance()).properties();
		
		for( Property property : properties)
		{
			if (!pObject.has(property.name().external()))
			{
				pLoadedDiagramFile.addError(String.format(UNDEFINED_PROPERTY_ERROR, property.name().external(), edgeClass.getName()));
			}
		}
	}
}
