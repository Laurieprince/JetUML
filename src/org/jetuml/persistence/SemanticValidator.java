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

import org.jetuml.diagram.Diagram;
import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.builder.DiagramBuilder;
import org.jetuml.rendering.DiagramRenderer;

public class SemanticValidator extends AbstractValidator<Diagram>
{
	private Diagram aDiagram;
	private Diagram aValidatedDiagram;
	private DiagramRenderer aValidatedRenderer;
	private DiagramBuilder aValidatedBuilder;

	public SemanticValidator(Diagram pDiagram)
	{
		assert pDiagram != null;
		
		aDiagram = pDiagram;
		aValidatedDiagram = new Diagram(aDiagram.getType());
		aValidatedBuilder = DiagramType.newBuilderInstanceFor(aValidatedDiagram);
		aValidatedRenderer = aValidatedBuilder.renderer();
	}

	public void validate()
	{
		if(!validateNodes()) return;
		if(!validateEdges()) return;
		if(!validateChildren()) return;
		setObject(aDiagram);
	}
	
	private boolean validateNodes()
	{
		var result = true;
		for (Node rootNode : aDiagram.rootNodes())
		{
			if (aValidatedBuilder.canAdd(rootNode, rootNode.position()))
			{
				aValidatedDiagram.addRootNode(rootNode);
			}
			else
			{
				addError(String.format(RESOURCES.getString("error.validator.invalid_node_addition"), rootNode.getClass().getSimpleName(), rootNode.position().toString()));
				result = false;
			}
		}
		return result;
	}
	
	private boolean validateChildren()
	{
		try
		{
			aValidatedRenderer.getBounds();
			for (Node rootNode : aDiagram.rootNodes())
			{
				for(Node childNode : rootNode.getChildren())
				{
					if (!aValidatedBuilder.canAdd(childNode, childNode.position()))
					{
						addError(String.format(RESOURCES.getString("error.validator.invalid_node_addition"), rootNode.toString(), rootNode.position().toString()));
						return false;
					}
				}
			}
			return true;
		}
		catch(Exception e)
		{
			addError(String.format(RESOURCES.getString("error.validator.diagram_rendering"), e.getMessage()));
			return false;
		}
	}

	private boolean validateEdges()
	{
		var result = true;
		for (Edge edge : aDiagram.edges())
		{
			if (aValidatedBuilder.canAdd(edge, edge.getStart(), edge.getEnd()))
			{
				aValidatedDiagram.addEdge(edge);
			} 
			else
			{
				addError(String.format(RESOURCES.getString("error.validator.invalid_edge_addition"), edge.toString()));
				result = false;
			}
		}
		return result;
	}
}
