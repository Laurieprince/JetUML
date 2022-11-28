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

public class JSONValidator
{
	private ValidationContext aValidationContext;
	private File aFile;
	
	public JSONValidator(ValidationContext pvalidationContext)
	{
		assert pvalidationContext != null && pvalidationContext.file() != null;
		aValidationContext = pvalidationContext;
		aFile = pvalidationContext.file();
	}
	
	public void validate() throws FileNotFoundException, IOException
	{
		try( BufferedReader in = new BufferedReader(
				new InputStreamReader(new FileInputStream(aFile), StandardCharsets.UTF_8)))
		{
			JSONObject jsonObject = new JSONObject(in.lines().collect(Collectors.joining("\n")));
			aValidationContext.setJSONObject(jsonObject);
		}
		catch( JSONException e )
		{
			aValidationContext.addError(RESOURCES.getString("error.validator.decode_file"));
		}
	}
}
