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

import org.jetuml.diagram.Diagram;
import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.Property;
import org.jetuml.geom.Point;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Converts a JSONObject to a versioned diagram.
 */
public final class JsonDecoder
{
	private static final String PREFIX_NODES = "org.jetuml.diagram.nodes.";
	private static final String PREFIX_EDGES = "org.jetuml.diagram.edges.";
	
	private JsonDecoder() {}
	
	/**
	 * @param pDiagram A JSON object that encodes the diagram.
	 * @return The decoded diagram.
	 * @throws DeserializationException If it's not possible to decode the object into a valid diagram.
	 */
	public static LoadedDiagramFile decode(JSONObject pDiagram)
	{
		assert pDiagram != null;
		try
		{
			LoadedDiagramFile loadedDiagramFile = new LoadedDiagramFile();
			Diagram diagram = new Diagram(DiagramType.fromName(pDiagram.getString("diagram")));
			loadedDiagramFile.setDiagram(diagram);
			decodeNodes(loadedDiagramFile, pDiagram);
			restoreChildren(loadedDiagramFile, pDiagram);
			restoreRootNodes(loadedDiagramFile);
			decodeEdges(loadedDiagramFile, pDiagram);
			loadedDiagramFile.context().attachNodes();

			return loadedDiagramFile;
		} 
		catch (JSONException | IllegalArgumentException exception)
		{
			throw new DeserializationException("Cannot decode serialized object", exception);
		}
	}
	
	/* 
	 * Extracts information about nodes from pObject and creates new objects
	 * to represent them.
	 * throws Deserialization Exception
	 */
	private static void decodeNodes(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject)
	{
		JSONArray nodes = pObject.getJSONArray("nodes");
		var diagramPrototypeClasses = pLoadedDiagramFile.diagram().getType().getPrototypes().stream().map(x -> x.getClass()).toList();
		for( int i = 0; i < nodes.length(); i++ )
		{
			try
			{
				JSONObject object = nodes.getJSONObject(i);

				if (!object.has("type") || !object.has("id"))
				{
					pLoadedDiagramFile.addError("'type' property is not defined for node " + i);
					continue;
				}
				
				Class<?> nodeClass = Class.forName(PREFIX_NODES + object.getString("type")); 
				
				if(!diagramPrototypeClasses.contains(nodeClass))
				{
					pLoadedDiagramFile.addError("node " + nodeClass + " is not defined for " + pLoadedDiagramFile.diagram().getType());
					continue;
				}
				
				Node node = (Node) nodeClass.getDeclaredConstructor().newInstance();
				
				if (!object.has("x") || !object.has("y"))
				{ 
					pLoadedDiagramFile.addError("coordinates are not defined for node " + i);
					continue;
				}

				node.moveTo(new Point(object.getInt("x"), object.getInt("y")));
				for( Property property : node.properties() )
				{
					if (!object.has(property.name().external()))
					{
						pLoadedDiagramFile.addError(property.name().external() + " is not defined for node " + i);
						continue;
					}

					property.set(object.get(property.name().external()));
				}
				
				pLoadedDiagramFile.context().addNode(node, object.getInt("id"));
				
			} 
			catch (ReflectiveOperationException exception)
			{
				throw new DeserializationException("Cannot instantiate serialized object", exception);
			}
		}
	}
	
	/* 
	 * Discovers the root nodes and stores them in the diagram.
	 */
	private static void restoreRootNodes(LoadedDiagramFile pLoadedDiagramFile)
	{
		for( Node node : pLoadedDiagramFile.context() )
		{
			if( !node.hasParent() )
			{
				pLoadedDiagramFile.context().pDiagram().addRootNode(node);
			}
		}
	}
	
	/* 
	 * Restores the parent-child hierarchy within the context's diagram. Assumes
	 * the context has been initialized with all the nodes.
	 */
	private static void restoreChildren(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject)
	{
		JSONArray nodes = pObject.getJSONArray("nodes");
		for( int i = 0; i < nodes.length(); i++ )
		{
			JSONObject object = nodes.getJSONObject(i);
			if( object.has("children"))
			{
				Node node = pLoadedDiagramFile.context().getNode(object.getInt("id"));
				JSONArray children = object.getJSONArray("children");
				for( int j = 0; j < children.length(); j++ )
				{
					node.addChild(pLoadedDiagramFile.context().getNode(children.getInt(j)));
				}
			}
		}
	}
	
	/* 
	 * Extracts information about nodes from pObject and creates new objects
	 * to represent them.
	 * throws Deserialization Exception
	 */
	private static void decodeEdges(LoadedDiagramFile pLoadedDiagramFile, JSONObject pObject)
	{
		JSONArray edges = pObject.getJSONArray("edges");
		var diagramPrototypeClasses = pLoadedDiagramFile.diagram().getType().getPrototypes().stream().map(x -> x.getClass()).toList();
		for (int i = 0; i < edges.length(); i++)
		{
			try
			{
				JSONObject object = edges.getJSONObject(i);

				if (!object.has("type") || !object.has("start") || !object.has("end"))
				{
					pLoadedDiagramFile.addError("type, start and end properties are not defined for this edge");
					continue;
				}

				Class<?> edgeClass = Class.forName(PREFIX_EDGES + object.getString("type"));

				if(!diagramPrototypeClasses.contains(edgeClass))
				{
					pLoadedDiagramFile.addError("edge " + edgeClass + " is not defined for " + pLoadedDiagramFile.diagram().getType());
					continue;
				}
				
				Edge edge = (Edge) edgeClass.getDeclaredConstructor().newInstance();
				
				for( Property property : edge.properties())
				{
					if (!object.has(property.name().external()))
					{
						pLoadedDiagramFile.addError(property.name().external() + " properties are not defined for this edge");
						continue;
					}

					property.set(object.get(property.name().external()));
				}
				
//				if(diagramBuilder.canAdd(edge, startNode, endNode))
				
				edge.connect(pLoadedDiagramFile.context().getNode(object.getInt("start")),
						pLoadedDiagramFile.context().getNode(object.getInt("end")),
						pLoadedDiagramFile.context().pDiagram());
				
				pLoadedDiagramFile.context().pDiagram().addEdge(edge);
			} 
			catch (ReflectiveOperationException exception)
			{
				throw new DeserializationException("Cannot instantiate serialized object", exception);
			}
		}
	}
}
