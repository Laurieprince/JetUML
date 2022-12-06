package org.jetuml.persistence;

public enum NodeBaseProperties {
	
		X("x", "integer"), Y("y", "integer"), ID("id", "integer"), TYPE("type", "string");
		
		private final String aLabel;
		private final String aType;
		
		NodeBaseProperties(String pLabel, String pType)
		{ 
			aLabel = pLabel; 
			aType = pType;
		}
		
		public String getLabel()
		{ return aLabel; }
		
		public String getType()
		{ return aType; }
	
}
