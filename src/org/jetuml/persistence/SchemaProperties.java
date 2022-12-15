package org.jetuml.persistence;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SchemaProperties
{	
	public SchemaProperties(){};
	
	public static List<SchemaProperty> diagramProperties()
	{
		return List.of(
				new SchemaProperty("diagram", SchemaType.STRING),
				new SchemaProperty("nodes", SchemaType.ARRAY),
				new SchemaProperty("edges", SchemaType.ARRAY),
				new SchemaProperty("version", SchemaType.STRING));
	}
	
	public static List<SchemaProperty> nodeBaseProperties()
	{
		return List.of(
				new SchemaProperty("x",  SchemaType.INTEGER),
				new SchemaProperty("y",  SchemaType.INTEGER),
				new SchemaProperty("id",  SchemaType.INTEGER),
				new SchemaProperty("type", SchemaType.STRING));
	}
	
	public static List<SchemaProperty> edgeBaseProperties()
	{
		return List.of(
				new SchemaProperty("start", SchemaType.INTEGER),
				new SchemaProperty("end", SchemaType.INTEGER),
				new SchemaProperty("type", SchemaType.STRING));
	}
	
	public static class SchemaTypeObject extends JSONObject
	{
		public SchemaTypeObject(String pType)
		{
			put("type", pType);
		}
	}
	
	public enum SchemaType 
	{
		STRING(new SchemaTypeObject("string")),
		INTEGER(new SchemaTypeObject("integer")),
		NUMBER(new SchemaTypeObject("number")),
		BOOLEAN(new SchemaTypeObject("boolean")),
		ARRAY(new SchemaArray()),
		OBJECT(new SchemaObject());
	
		private JSONObject aJsonObject;
		
		private SchemaType(JSONObject pJsonObject)
		{
			aJsonObject = pJsonObject;
		}
		
		public JSONObject get() { return aJsonObject; }
	}
	
	public static class SchemaProperty
	{
		private String aName;
		private SchemaType aType;
		
		public SchemaProperty(String pName, SchemaType pType)
		{
			aName = pName;
			aType = pType;
		}
		
		public String name() { return aName; }
		
		public SchemaType type() { return aType; }
	}
	
	public static class SchemaObject extends JSONObject
	{
		private final String type = "object";
		public JSONObject properties = new JSONObject();
		private JSONArray allOf = new JSONArray();
		private JSONArray required = new JSONArray();
		private boolean unevaluatedProperties = false;
		
		public SchemaObject()
		{
			put("type", type);
			put("required", required);
			put("properties", properties);
			put("allOf", allOf);
			put("unevaluatedProperties", unevaluatedProperties);
		}
		
		public SchemaObject(String pSchema, String pId, String pTitle)
		{
			put("$schema", pSchema);
			put("$id", pId);
			put("title", pTitle);
			put("type", type);
			put("required", required);
			put("properties", properties);
			put("unevaluatedProperties", unevaluatedProperties);
		}
		
		public void addAllOf(JSONObject pObject)
		{
			allOf.put(pObject);
		}
		
		public void addProperty(String pName, JSONObject pObject)
		{
			properties.put(pName,  pObject);
		}
		
		public void addRequired(String... pRequired)
		{
			for(String r : pRequired) required.put(r);
		}
	}
	
	public static class SchemaEnum extends JSONObject
	{
		private JSONArray enumArray = new JSONArray();
		
		public SchemaEnum()
		{
			put("enum", enumArray);
		}
		
		public SchemaEnum(Object... pEnums)
		{
			for(Object pEnum : pEnums) addEnum(pEnum);
			put("enum", enumArray);
		}
		
		public void addEnum(Object pEnum)
		{
			enumArray.put(pEnum);
		}
	}
	
	public static class SchemaArray extends JSONObject
	{
		public SchemaArray()
		{
			put("type", "array");
			put("uniqueItems", true);
		}
		
		public void setItems(JSONObject pObject)
		{
			put("items", pObject);
		}
	}
}
