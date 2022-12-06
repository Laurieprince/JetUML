package org.jetuml.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
import org.json.JSONArray;
import org.json.JSONObject;

public final class SchemaGenerator
{
	private static final String JSONSCHEMA_VERSION = "https://json-schema.org/draft/2019-09/schema";
	
	private SchemaGenerator() {}
	
	/**
	 * Run without arguments.
	 * 
	 * @param pArgs Not used.
	 */
	public static void main(String[] pArgs) throws IOException
	{
		generateDiagramSchemas();
	}
	
	private static void generateDiagramSchemas() throws IOException 
	{
		for( DiagramType diagramType : DiagramType.values() )
		{
			Path OUTPUT_FILE = Paths.get("docs/jsonschema", String.format("jsonschema_%s.json", diagramType.getName()));
			File diagramFile = OUTPUT_FILE.toFile();
			try( PrintWriter out = new PrintWriter(
					new OutputStreamWriter(new FileOutputStream(diagramFile), StandardCharsets.UTF_8)))
			{
				out.println(encode(diagramType, OUTPUT_FILE.toString()).toString(3));
			}
			System.out.println(String.format("The diagram %s was generated successfully.", diagramType.getName()));
		}
	}
	
	
	public static JSONObject encode(DiagramType pDiagramType, String pFilePath)
	{
		JSONObject schemaObject = new JSONObject();
		schemaObject.put("$schema", JSONSCHEMA_VERSION);
		schemaObject.put("$id", pFilePath);
		schemaObject.put("title", pDiagramType.getName());
		schemaObject.put("description", pDiagramType.getName());
		schemaObject.put("type", "object");
		
		schemaObject.put("required", new String[]{"diagram", "nodes", "edges", "version"});
		
		JSONObject diagramObject = new JSONObject();
		diagramObject.put("const", pDiagramType.getName());
		
		JSONObject schemaProperties = new JSONObject();
		schemaProperties.put("diagram", diagramObject);
		
		schemaProperties.put("nodes", encodeDiagramElements(pDiagramType, Node.class));
		schemaProperties.put("edges", encodeDiagramElements(pDiagramType, Edge.class));
		schemaProperties.put("version", encodeField("string"));
		schemaObject.put("properties", schemaProperties);
		
		return schemaObject;
	}
	
	private static JSONObject encodeDiagramElements(DiagramType pDiagramType, Class<? extends DiagramElement> elementClass)
	{
		JSONObject elementObject = new JSONObject();
		
		elementObject.put("type", "array");
		elementObject.put("description", "description");
		
		JSONObject propertiesObject = new JSONObject();
		
		if(elementClass.equals(Node.class)) encodeNodeBaseProperties(propertiesObject);
		else encodeEdgeBaseProperties(propertiesObject);
		
		var diagramPrototypes = pDiagramType.getPrototypes().stream()
				.filter(x -> elementClass.isAssignableFrom(x.getClass()))
				.filter(distinctByKey(x -> x.getClass()))
				.collect(Collectors.toSet());
		
		JSONArray enumArray = new JSONArray();
		JSONArray allOfArray = new JSONArray();
		for(DiagramElement diagramElement: diagramPrototypes)
		{
			enumArray.put(diagramElement.getClass().getSimpleName());
			propertiesToJSONObject(propertiesObject, diagramElement.properties());
			allOfArray.put(encodeIfThenStatement(diagramElement));
		}
		
		JSONObject typeObject = new JSONObject();
		typeObject.put("enum", enumArray);
		propertiesObject.put("type", typeObject);
		
		JSONObject itemsObject = new JSONObject();
		itemsObject.put("type", "object");
		itemsObject.put("properties", propertiesObject);
		itemsObject.put("allOf", allOfArray);
		
		if(elementClass.equals(Node.class)) itemsObject.put("required", Arrays.asList(NodeBaseProperties.values()).stream().map(x -> x.getLabel()).toList());
		else itemsObject.put("required", Arrays.asList(EdgeBaseProperties.values()).stream().map(x -> x.getLabel()).toList());
		
		itemsObject.put("unevaluatedProperties", false);
		
		elementObject.put("items", itemsObject);
		
		return elementObject;
	}
	
	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor)
	{
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null; 
	}
	
	private static void encodeNodeBaseProperties(JSONObject object)
	{
		for(NodeBaseProperties nodeBaseProperty : NodeBaseProperties.values())
		{
			object.put(nodeBaseProperty.getLabel(), encodeField(nodeBaseProperty.getType()));	
		}
	
		JSONObject childrenObject = new JSONObject();
		childrenObject.put("type", "array");
		childrenObject.put("uniqueItems", true);
		childrenObject.put("items", encodeField("integer"));
		object.put("children", childrenObject);
	}
	
	private static void encodeEdgeBaseProperties(JSONObject object)
	{
		for(EdgeBaseProperties edgeBaseProperty : EdgeBaseProperties.values())
		{
			object.put(edgeBaseProperty.getLabel(), encodeField(edgeBaseProperty.getType()));	
		}
	}
	
	private static void propertiesToJSONObject(JSONObject object, Properties pProperties) 
	{
		for( Property property : pProperties )
		{
			Object value = property.get();
			if( value instanceof String ) 
			{
				object.put(property.name().external(), encodeField("string"));
			}
			else if (value instanceof Enum )
			{
				JSONObject enumObject = new JSONObject();
				enumObject.put("enum", value.getClass().getEnumConstants());
				object.put(property.name().external(), enumObject);
			}
			else if( value instanceof Integer)
			{
				object.put(property.name().external(), encodeField("integer"));
			}
			else if( value instanceof Boolean)
			{
				object.put(property.name().external(),encodeField("boolean"));
			}
		}
	}
	
	private static JSONObject encodeField( String pType)
	{
		JSONObject typeObject = new JSONObject();
		typeObject.put("type",  pType);
		
		return typeObject;
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
