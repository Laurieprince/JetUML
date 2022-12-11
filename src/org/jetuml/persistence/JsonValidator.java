package org.jetuml.persistence;

import static org.jetuml.application.ApplicationResources.RESOURCES;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonValidator
{
	private ValidationContext aValidationContext;
	private File aFile;
	
	public JsonValidator(ValidationContext pvalidationContext)
	{
		assert pvalidationContext != null && pvalidationContext.file() != null;
		aValidationContext = pvalidationContext;
		aFile = pvalidationContext.file();
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
			JSONObject jsonObject = new JSONObject(in.lines().collect(Collectors.joining("\n")));
			aValidationContext.setJSONObject(jsonObject);
		}
		catch( IOException | JSONException e )
		{
			aValidationContext.addError(String.format(RESOURCES.getString("error.validator.decode_file"), e.getMessage()));
		}
	}
}
