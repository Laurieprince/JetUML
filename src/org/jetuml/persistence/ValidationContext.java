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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetuml.diagram.Diagram;
import org.json.JSONObject;

public class ValidationContext
{
	private Optional<File> aFile;
	private Optional<JSONObject> aJSONObject;
	private Optional<Diagram> aDiagram;
	private List<String> aErrors = new ArrayList<String>();

	ValidationContext(File pFile)
	{
		assert pFile != null;
		aFile = Optional.of(pFile);
	}

	public File file()
	{
		return aFile.get();
	}
	
	public JSONObject JSONObject()
	{
		return aJSONObject.get();
	}
	
	public void setJSONObject(JSONObject pJSONObject)
	{
		aJSONObject = Optional.of(pJSONObject);
	}
	
	public Diagram diagram()
	{
		return aDiagram.get();
	}
	
	public void setDiagram(Diagram pDiagram)
	{
		aDiagram = Optional.of(pDiagram);
	}
	
	public boolean isValid()
	{
		return aErrors.isEmpty();
	}
	
	public void addError(String pError)
	{
		aErrors.add(pError);
	}
	
	public List<String> errors()
	{
		return aErrors;
	}
}
