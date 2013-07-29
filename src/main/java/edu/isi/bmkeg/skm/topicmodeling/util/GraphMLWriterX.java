package edu.isi.bmkeg.skm.topicmodeling.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.io.GraphMLMetadata;
import edu.uci.ics.jung.io.GraphMLWriter;

/**
 * Enhances jung's GraphMLWriter with Edge's description attribute
 * name and attribute type. 
 */
public class GraphMLWriterX<V,E> extends GraphMLWriter<V,E> {

	public GraphMLWriterX() {
		super();
	}

    /**
     * Adds a new edge data specification.
     */
	public void addEdgeData(String id, String description, String default_value,
			String name, String type,
			Transformer<E, String> edge_transformer)
	{
		if (edge_data.equals(Collections.EMPTY_MAP))
			edge_data = new HashMap<String, GraphMLMetadata<E>>();
		edge_data.put(id, new GraphMLMetadataX<E>(description, default_value,
				name, type,
				edge_transformer));
	}
	
    /**
     * Adds a new vertex data specification.
     */
	public void addVertexData(String id, String description, String default_value,
			String name, String type,
			Transformer<V, String> vertex_transformer)
	{
		if (vertex_data.equals(Collections.EMPTY_MAP))
			vertex_data = new HashMap<String, GraphMLMetadata<V>>();
		vertex_data.put(id, new GraphMLMetadataX<V>(description, default_value,
				name, type,
				vertex_transformer));
	}

	protected void writeKeySpecification(String key, String type, 
			GraphMLMetadata<?> ds, BufferedWriter bw) throws IOException
	{
		if (!(ds instanceof GraphMLMetadataX<?>)) {
			super.writeKeySpecification(key, type, ds, bw);			
		} else {
			GraphMLMetadataX<?> dsx = (GraphMLMetadataX<?>) ds;
			bw.write("<key id=\"" + key + "\" for=\"" + type + "\"");
			if (dsx.name != null)
				bw.write(" attr.name=\"" + dsx.name +"\"");
			if (dsx.type != null)
				bw.write(" attr.type=\"" + dsx.type +"\"");
			boolean closed = false;
			// write out description if any
			String desc = ds.description;
			if (desc != null)
			{
				if (!closed)
				{
					bw.write(">\n");
					closed = true;
				}
				bw.write("<desc>" + desc + "</desc>\n");
			}
			// write out default if any
			Object def = ds.default_value;
			if (def != null)
			{
				if (!closed)
				{
					bw.write(">\n");
					closed = true;
				}
				bw.write("<default>" + def.toString() + "</default>\n");
			}
			
			if (!closed)
			    bw.write("/>\n");
			else
			    bw.write("</key>\n");
			
		}
	}

}

