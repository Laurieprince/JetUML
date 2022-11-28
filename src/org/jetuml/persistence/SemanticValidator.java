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
	private DiagramRenderer aDiagramRenderer;
	private DiagramBuilder aDiagramBuilder;
	private ValidationContext aValidationContext;

	public SemanticValidator(ValidationContext pvalidationContext)
	{
		assert pvalidationContext != null && pvalidationContext.diagram() != null;
		aValidationContext = pvalidationContext;
		aDiagram = aValidationContext.diagram();
		aDiagramBuilder = DiagramType.newBuilderInstanceFor(aDiagram);
		aDiagramRenderer = aDiagramBuilder.renderer();
	}

	public void validate()
	{
		aDiagramRenderer.getBounds(); // trigger rendering pass
		validateNodes();
		validateEdges();
	}

	private void validateNodes()
	{
		for (Node node : aDiagram.allNodes())
		{
			var nodePosition = aDiagramRenderer.getBounds(node).getCenter();
			if (!aDiagramBuilder.canAdd(node, nodePosition))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_node_addition"), node.toString(), nodePosition.toString()));
			}
		}
	}

	private void validateEdges()
	{
		for (Edge edge : aDiagram.edges())
		{
			var startPosition = aDiagramRenderer.getBounds(edge.getStart()).getCenter();
			var endPosition = aDiagramRenderer.getBounds(edge.getEnd()).getCenter();
			if (!aDiagramBuilder.canAdd(edge, startPosition, endPosition))
			{
				aValidationContext.addError(String.format(RESOURCES.getString("error.validator.invalid_edge_addition"), edge.toString(), startPosition.toString(), endPosition.toString()));
			}
		}
	}

}
