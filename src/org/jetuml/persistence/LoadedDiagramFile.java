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
package org.jetuml.persistence;

import java.util.ArrayList;
import java.util.List;

import org.jetuml.diagram.Diagram;

/**
 * Wrapper for a diagram object that also stores
 * the version of JetUML with which the diagram was
 * serialized, and whether the loaded diagram contains errors.
 */
public class LoadedDiagramFile {
	private Diagram aDiagram;
	private List<String> aErrors = new ArrayList<String>();

	LoadedDiagramFile(){
		aDiagram = null;
	}
	
	/**
	 * @return The diagram wrapped by this object.
	 */
	public Diagram diagram()
	{
		return aDiagram;
	}
	
	public boolean hasError() {
		return !aErrors.isEmpty();
	}
	
	public void addError(String pError) {
		aErrors.add(pError);
	}
	
	public List<String> getErrors() {
		return aErrors;
	}
	
	public void setDiagram(Diagram pDiagram) {
		aDiagram = pDiagram;
	}
}
