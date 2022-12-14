package org.jetuml.persistence;

import static org.jetuml.application.ApplicationResources.RESOURCES;

import org.jetuml.diagram.Diagram;
import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.builder.DiagramBuilder;
import org.jetuml.rendering.DiagramRenderer;

public class SemanticValidator
{
	private Diagram aDiagram;
	private Diagram aValidatedDiagram;
	private DiagramRenderer aDiagramRenderer;
	private DiagramBuilder aDiagramBuilder;
	private ValidationContext aValidationContext;

	public SemanticValidator(ValidationContext pValidationContext)
	{
		assert pValidationContext != null && pValidationContext.diagram() != null;
		
		aValidationContext = pValidationContext;
		aDiagram = aValidationContext.diagram();
		aValidatedDiagram = new Diagram(aDiagram.getType());
		aDiagramBuilder = DiagramType.newBuilderInstanceFor(aValidatedDiagram);
		aDiagramRenderer = aDiagramBuilder.renderer();
	}

	public void validate()
	{
		if(!validateNodes()) return;
		if(!validateEdges()) return;
		if(!validateChildren()) return;
	}
	
	private boolean validateNodes()
	{
		var result = true;
		for (Node rootNode : aDiagram.rootNodes())
		{
			if (aDiagramBuilder.canAdd(rootNode, rootNode.position()))
			{
				aValidatedDiagram.addRootNode(rootNode);
			}
			else
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_node_addition"), rootNode.getClass().getSimpleName(), rootNode.position().toString()));
				result = false;
			}
		}
		return result;
	}
	
	private boolean validateChildren()
	{
		try
		{
			aDiagramRenderer.getBounds();
			for (Node rootNode : aDiagram.rootNodes())
			{
				for(Node childNode : rootNode.getChildren())
				{
					if (!aDiagramBuilder.canAdd(childNode, childNode.position()))
					{
						aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_node_addition"), rootNode.toString(), rootNode.position().toString()));
						return false;
					}
				}
			}
			return true;
		}
		catch(Exception e)
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.diagram_rendering"), e.getMessage()));
			return false;
		}
	}

	private boolean validateEdges()
	{
		var result = true;
		for (Edge edge : aDiagram.edges())
		{
			if (aDiagramBuilder.canAdd(edge, edge.getStart(), edge.getEnd()))
			{
				aValidatedDiagram.addEdge(edge);
			} 
			else
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_edge_addition"), edge.toString()));
				result = false;
			}
		}
		return result;
	}
}
