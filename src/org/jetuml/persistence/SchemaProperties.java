package org.jetuml.persistence;

import java.util.List;

public final class SchemaProperties
{
	public static final String JSONSCHEMA_VERSION = "https://json-schema.org/draft/2019-09/schema";
	
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
				new SchemaProperty("type", SchemaType.STRING),
				new SchemaProperty("id",  SchemaType.INTEGER));
	}
	
	public static List<SchemaProperty> edgeBaseProperties()
	{
		return List.of(
				new SchemaProperty("start", SchemaType.INTEGER),
				new SchemaProperty("end", SchemaType.INTEGER),
				new SchemaProperty("type", SchemaType.STRING));
	}
	
	public enum SchemaType
	{
		STRING("string"), INTEGER("integer"), NUMBER("number"), BOOLEAN("boolean"), ARRAY("array"), OBJECT("object");
		
		private final String aName;
		
		SchemaType(String pName){ aName = pName;}
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
		
		public String name()
		{
			return aName;
		}
		
		public SchemaType type()
		{
			return aType;
		}
	}
}
