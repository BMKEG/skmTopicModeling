package edu.isi.bmkeg.skm.topicmodeling.util;

import org.apache.commons.collections15.Transformer;

public class GraphMLMetadataX<T> extends edu.uci.ics.jung.io.GraphMLMetadata<T> {

	public String name;
	
	public String type;
	
	public GraphMLMetadataX(String description, String default_value,
			String name, String type,
			Transformer<T, String> transformer) {
		super(description, default_value, transformer);
		this.name = name;
		this.type = type;
	}

}
