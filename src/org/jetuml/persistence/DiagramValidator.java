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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public final class DiagramValidator
{
	/**
	 * Run without arguments.
	 * 
	 * @param pArgs Not used.
	 */
	public static void main(String[] pArgs) throws IOException
	{
		assert pArgs.length == 1;
		
		File diagramFile = Paths.get(pArgs[0]).toFile();
		Validator validator = validate(diagramFile);
		
		if(validator.isValid())
		{
			System.out.println("Diagram is Valid.");
		}
		else
		{
			System.out.println(validator.errors().toString());
		}
	}
	
	public static Validator validate(File pFile)
	{
		JsonValidator jsonValidator = new JsonValidator(pFile);
		jsonValidator.validate();
		if(!jsonValidator.isValid()) return jsonValidator;
		
		SchemaValidator schemaValidator = new SchemaValidator(jsonValidator.object());
		schemaValidator.validate();
		if(!schemaValidator.isValid()) return schemaValidator;
		
		var diagram = JsonDecoder.decode(jsonValidator.object());
		
		SemanticValidator semanticValidator = new SemanticValidator(diagram);
		semanticValidator.validate();
		return semanticValidator;
	}
}
