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
		var validationContext = validate(diagramFile);
		
		if(validationContext.isValid())
		{
			System.out.println("Diagram is Valid.");
		}
		else
		{
			System.out.println(validationContext.errors().toString());
		}
	}
	
	public static ValidationContext validate(File pFile)
	{
		ValidationContext validationContext = new ValidationContext(pFile);
		
		JsonValidator jsonValidator = new JsonValidator(validationContext);
		jsonValidator.validate();
		if(!validationContext.isValid()) return validationContext;
		
		SchemaValidator schemaValidator = new SchemaValidator(validationContext);
		schemaValidator.validate();
		if(!validationContext.isValid()) return validationContext;
		
		var diagram = JsonDecoder.decode(validationContext.JSONObject());
		validationContext.setDiagram(diagram);
		
		SemanticValidator semanticValidator = new SemanticValidator(validationContext);
		semanticValidator.validate();
		
		return validationContext;
	}
}
