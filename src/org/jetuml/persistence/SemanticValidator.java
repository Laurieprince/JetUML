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

	public SemanticValidator(ValidationContext pvalidationContext)
	{
		assert pvalidationContext != null && pvalidationContext.diagram() != null;
		
		aValidationContext = pvalidationContext;
		aDiagram = aValidationContext.diagram();
		aValidatedDiagram = new Diagram(aDiagram.getType());
		aDiagramBuilder = DiagramType.newBuilderInstanceFor(aValidatedDiagram);
		aDiagramRenderer = aDiagramBuilder.renderer();
	}

	public void validate()
	{
		if(!validateChildrenNodes()) return;
		if(!aValidationContext.isValid()) return;
		if(!validateNodes()) return;
		if(!validateEdges()) return;
		
		// Check if there is no exception 
		try
		{
			aDiagramRenderer.getBounds();
		}
		catch(Exception e)
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.diagram_rendering"), e.getMessage()));
		}
	}

	private boolean validateChildrenNodes()
	{
		var result = true;
		for(Node node: aDiagram.allNodes())
		{
			if(node.requiresParent() && !node.hasParent())
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.children_node_requires_parent"), node.toString()));
			}
		}
		return result;
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
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_node_addition"), rootNode.toString(), rootNode.position().toString()));
				result = false;
			}
		}
		return result;
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
