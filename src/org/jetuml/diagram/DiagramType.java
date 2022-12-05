/*******************************************************************************
 * JetUML - A desktop application for fast UML diagramming.
 *
 * Copyright (C) 2020 by McGill University.
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
package org.jetuml.diagram;

import static org.jetuml.application.ApplicationResources.RESOURCES;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetuml.diagram.builder.ClassDiagramBuilder;
import org.jetuml.diagram.builder.DiagramBuilder;
import org.jetuml.diagram.builder.ObjectDiagramBuilder;
import org.jetuml.diagram.builder.SequenceDiagramBuilder;
import org.jetuml.diagram.builder.StateDiagramBuilder;
import org.jetuml.diagram.builder.UseCaseDiagramBuilder;
import org.jetuml.rendering.ClassDiagramRenderer;
import org.jetuml.rendering.DiagramRenderer;
import org.jetuml.rendering.ObjectDiagramRenderer;
import org.jetuml.rendering.SequenceDiagramRenderer;
import org.jetuml.rendering.StateDiagramRenderer;
import org.jetuml.rendering.UseCaseDiagramRenderer;

/**
 * The different types of UML diagrams supported by 
 * this application.
 */
public enum DiagramType
{
	CLASS(
			"ClassDiagram",
			".class",
			ClassDiagramBuilder::new, 
			ClassDiagramRenderer::new,
			new DiagramElement [] { 
					Prototypes.CLASS, 
					Prototypes.INTERFACE, 
					Prototypes.PACKAGE, 
					Prototypes.PACKAGE_DESCRIPTION, 
					Prototypes.NOTE,
					Prototypes.DEPENDENCY, 
					Prototypes.GENERALIZATION, 
					Prototypes.REALIZATION,
					Prototypes.ASSOCIATION,
					Prototypes.AGGREGATION,
					Prototypes.COMPOSITION,
					Prototypes.NOTE_CONNECTOR,
					Prototypes.POINT,
					}),
	
	SEQUENCE(
			"SequenceDiagram",
			".sequence",
			SequenceDiagramBuilder::new, 
			SequenceDiagramRenderer::new, 
			new DiagramElement[]{
					Prototypes.IMPLICIT_PARAMETER,
					Prototypes.NOTE,
					Prototypes.METHOD_CALL,
					Prototypes.METHOD_RETURN,
					Prototypes.NOTE_CONNECTOR,
					Prototypes.CALL,
					Prototypes.CONSTRUCTOR,
					Prototypes.POINT,
					}), 
	
	STATE(
			"StateDiagram",
			".state",
			StateDiagramBuilder::new, 
			StateDiagramRenderer::new, 
			new DiagramElement[]{
					Prototypes.STATE,
					Prototypes.START_STATE,
					Prototypes.END_STATE,
					Prototypes.NOTE,
					Prototypes.TRANSITION,
					Prototypes.NOTE_CONNECTOR,
					Prototypes.POINT,
					}),
	
	OBJECT(
			"ObjectDiagram",
			".object",
			ObjectDiagramBuilder::new, 
			ObjectDiagramRenderer::new, 
			new DiagramElement[] {
					Prototypes.OBJECT,
					Prototypes.FIELD,
					Prototypes.NOTE,
					Prototypes.REFERENCE,
					Prototypes.COLLABORATION,
					Prototypes.NOTE_CONNECTOR,
					Prototypes.POINT,
					}), 
	
	USECASE(
			"UseCaseDiagram",
			".usecase",
			UseCaseDiagramBuilder::new, 
			UseCaseDiagramRenderer::new, 
			new DiagramElement[]{
					Prototypes.ACTOR, 
					Prototypes.USE_CASE, 
					Prototypes.NOTE, 
					Prototypes.USE_CASE_ASSOCIATION, 
					Prototypes.USE_CASE_EXTENDS, 
					Prototypes.USE_CASE_INCLUDES, 
					Prototypes.USE_CASE_GENERALIZATION, 
					Prototypes.NOTE_CONNECTOR,
					Prototypes.POINT,
					}); 
	
	/* aName is an internal name used for referring to objects of a certain diagram
	 * type in externalized representations, such as persisted versions of the diagram
	 * or property strings. It should this not be externalized. */
	private final String aName;
	private final String aFileExtension; // The suffix that indicates the type of files
	private final Function<Diagram, DiagramBuilder> aBuilderSupplier;
	private final Function<Diagram, DiagramRenderer> aRendererFactory;
	private final DiagramElement[] aPrototypes;
	
	
	DiagramType(String pName, String pFileExtension, Function<Diagram, DiagramBuilder> pBuilderSupplier, 
			Function<Diagram, DiagramRenderer> pRendererFactory, DiagramElement[] pPrototypes)
	{
		assert pName != null;
		aName = pName;
		aFileExtension = pFileExtension;
		aBuilderSupplier = pBuilderSupplier;
		aRendererFactory = pRendererFactory;
		aPrototypes = pPrototypes;
	}
	
	/**
	 * @param pName The name of the diagram type, to match the getName() field. Can be null.
	 * @return The DiagramType with name pName.
	 * @throws IllegalArgumentException if pName is not a valid diagram type name.
	 */
	public static DiagramType fromName(String pName)
	{
		for( DiagramType type : DiagramType.values() )
		{
			if( type.getName().equals(pName) )
			{
				return type;
			}
		}
		throw new IllegalArgumentException(pName + " is not a valid " + DiagramType.class.getSimpleName() + " name");
	}
	
	/**
	 * @return The file extension for this type of diagram.
	 */
	public String getFileExtension()
	{
		return aFileExtension;
	}
	
	/**
	 * @return A short description of the diagram type.
	 */
	public String getFileNameDescription()
	{
		return RESOURCES.getString( aName.toLowerCase() + ".file.name");
	}
	
	/**
	 * Gets the diagram elements that can be created 
	 * using the Prototype pattern.
	 * The list returned is a copy of the prototypes: 
	 * it can be safely modified.
	 * @return A non-null list of prototypes
	 */   
	public List<DiagramElement> getToolPrototypes()
	{
		return Arrays.asList(aPrototypes).stream().filter(x -> x.isToolElement()).collect(Collectors.toList());
	}
	
	/**
	 * Gets all the diagram elements.
	 * The list returned is a copy of the prototypes: 
	 * it can be safely modified.
	 * @return A non-null list of prototypes
	 */   
	public List<DiagramElement> getPrototypes()
	{
		return Arrays.asList(aPrototypes);
	}

	/**
	 * @param pDiagram The diagram for which we want to build a builder.
	 * @return A new instance of a builder for this diagram type.
	 * @pre pDiagram != null
	 */
	public static DiagramBuilder newBuilderInstanceFor(Diagram pDiagram)
	{
		/* This method is not defined on class Diagram to avoid introducing 
		 * a dependency between Diagram and the GUI framework. */
		assert pDiagram != null;
		return pDiagram.getType().aBuilderSupplier.apply(pDiagram);
	}
	
	/**
	 * @param pDiagram The diagram for which we want to build a renderer.
	 * @return A new instance of a renderer for this diagram type.
	 * @pre pDiagram != null
	 */
	public static DiagramRenderer newRendererInstanceFor(Diagram pDiagram)
	{
		/* This method is not defined on class Diagram to avoid introducing 
		 * a dependency between Diagram and the GUI framework. */
		assert pDiagram != null;
		return pDiagram.getType().aRendererFactory.apply(pDiagram);
	}
	
	/**
	 * @return The name of the diagram type.
	 */
	public String getName()
	{
		return aName;
	}
}
