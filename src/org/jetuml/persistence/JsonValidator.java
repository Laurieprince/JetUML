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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonValidator extends AbstractValidator<JSONObject>
{
	private File aFile;
	
	public JsonValidator(File pFile)
	{
		assert pFile != null;
		aFile = pFile;
	}
	
	/**
	 * Reads a JSONObject from a file.
	 * 
	 * @param pFile The file to read the JSONObject from.
	 * @throws IOException if the file cannot be read.
	 * @throws JSONException if the JSONObject cannot be constructed.
	 */
	public void validate()
	{
		try( BufferedReader in = new BufferedReader(
				new InputStreamReader(new FileInputStream(aFile), StandardCharsets.UTF_8)))
		{
			setObject(new JSONObject(in.lines().collect(Collectors.joining("\n"))));
		}
		catch( IOException | JSONException e )
		{
			addError(String.format(RESOURCES.getString("error.validator.decode_file"), e.getMessage()));
		}
	}
}
