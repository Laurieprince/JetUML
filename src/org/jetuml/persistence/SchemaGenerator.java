package org.jetuml.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetuml.diagram.DiagramElement;
import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.Properties;
import org.jetuml.diagram.Property;
import org.jetuml.persistence.SchemaProperties.SchemaArray;
import org.jetuml.persistence.SchemaProperties.SchemaEnum;
import org.jetuml.persistence.SchemaProperties.SchemaObject;
import org.jetuml.persistence.SchemaProperties.SchemaProperty;
import org.jetuml.persistence.SchemaProperties.SchemaType;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SchemaGenerator
{
	public static final String JSONSCHEMA_VERSION = "https://json-schema.org/draft/2019-09/schema";
	
	private SchemaGenerator() {}
	
	/**
	 * Run without arguments.
	 * 
	 * @param pArgs Not used.
	 */
	public static void main(String[] pArgs) throws IOException
	{
		generateJsonSchemas();
	}
	
	private static void generateJsonSchemas() throws IOException 
	{
		for( DiagramType diagramType : DiagramType.values() )
		{
			Path OUTPUT_FILE = Paths.get("docs/jsonschema", String.format("jsonschema_%s.json", diagramType.getName()));
			try( PrintWriter out = new PrintWriter(
					new OutputStreamWriter(new FileOutputStream(OUTPUT_FILE.toFile()), StandardCharsets.UTF_8)))
			{
				out.println(encode(diagramType, OUTPUT_FILE.toString()).toString(3));
				System.out.println(String.format("The diagram %s was generated successfully.", diagramType.getName()));
			} 
			catch(IOException e)
			{
				System.out.println(e);
			}
		}
	}
	
	public static JSONObject encode(DiagramType pDiagramType, String pFilePath)
	{
		SchemaObject schemaObject = new SchemaObject(JSONSCHEMA_VERSION, pFilePath, pDiagramType.getName());
		schemaObject.addRequired(new String[]{"diagram", "nodes", "edges", "version"});
		
		JSONObject diagramObject = new JSONObject();
		diagramObject.put("const", pDiagramType.getName());

		schemaObject.addProperty("diagram", diagramObject);		
		schemaObject.addProperty("nodes", encodeDiagramElements(pDiagramType, Node.class));
		schemaObject.addProperty("edges", encodeDiagramElements(pDiagramType, Edge.class));
		schemaObject.addProperty("version", SchemaType.STRING.get());

		return schemaObject;
	}
	
	private static JSONObject encodeDiagramElements(DiagramType pDiagramType, Class<? extends DiagramElement> elementClass)
	{
		SchemaObject schemaObject = new SchemaObject();
		
		if(elementClass.equals(Node.class)) encodeNodeBaseProperties(schemaObject);
		else encodeEdgeBaseProperties(schemaObject);
		
		var diagramPrototypes = pDiagramType.getPrototypes().stream()
				.filter(x -> elementClass.isAssignableFrom(x.getClass()))
				.filter(distinctByKey(x -> x.getClass()))
				.collect(Collectors.toSet());
		
		SchemaEnum schemaEnum = new SchemaEnum();
		for(DiagramElement diagramElement: diagramPrototypes)
		{
			schemaEnum.addEnum(diagramElement.getClass().getSimpleName());
			propertiesToJsonObject(schemaObject, diagramElement.properties());
			schemaObject.addAllOf(encodeIfThenStatement(diagramElement));
		}
		
		schemaObject.addProperty("type", schemaEnum);
		
		SchemaArray schemaArray = new SchemaArray();
		schemaArray.setItems(schemaObject);
		
		return schemaArray;
	}
	
	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor)
	{
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null; 
	}
	
	private static void encodeNodeBaseProperties(SchemaObject object)
	{
		for(SchemaProperty nodeProperty : SchemaProperties.nodeBaseProperties())
		{
			object.addProperty(nodeProperty.name(), nodeProperty.type().get());	
			object.addRequired(nodeProperty.name());
		}
	
		SchemaArray childrenArray = new SchemaArray();
		childrenArray.setItems( SchemaType.INTEGER.get());
		object.addProperty("children", childrenArray);
	}
	
	private static void encodeEdgeBaseProperties(SchemaObject object)
	{
		for(SchemaProperty edgeProperty : SchemaProperties.edgeBaseProperties())
		{
			object.addProperty(edgeProperty.name(), edgeProperty.type().get());	
			object.addRequired(edgeProperty.name());
		}
	}
	
	private static void propertiesToJsonObject(SchemaObject object, Properties pProperties) 
	{
		for( Property property : pProperties )
		{
			Object value = property.get();
			if( value instanceof String )
			{
				object.addProperty(property.name().external(), SchemaType.STRING.get());
			}
			else if (value instanceof Enum )
			{
				object.addProperty(property.name().external(), new SchemaEnum(value.getClass().getEnumConstants()));
			}
			else if( value instanceof Integer)
			{
				object.addProperty(property.name().external(), SchemaType.INTEGER.get());
			}
			else if( value instanceof Boolean)
			{
				object.addProperty(property.name().external(), SchemaType.BOOLEAN.get());
			}
		}
	}

	private static JSONObject encodeIfThenStatement( DiagramElement pDiagramElement)
	{
		JSONObject conditionObject = new JSONObject();
		
		JSONObject constObject = new JSONObject();
		constObject.put("const", pDiagramElement.getClass().getSimpleName());
		JSONObject typeObject = new JSONObject();
		typeObject.put("type", constObject);
		JSONObject propertiesObject = new JSONObject();
		propertiesObject.put("properties", typeObject);
		
		JSONArray propertiesArray = new JSONArray();
		for(Property property : pDiagramElement.properties())
		{
			propertiesArray.put(property.name().external());
		}
		
		JSONObject requiredObject = new JSONObject();
		requiredObject.put("required", propertiesArray);
		
		conditionObject.put("if", propertiesObject );
		conditionObject.put("then", requiredObject);
		
		return conditionObject;
	}	
}
