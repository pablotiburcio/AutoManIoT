package net.floodlightcontroller.automaniot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class TopicReqEntries {
	protected static Logger log = LoggerFactory.getLogger(TopicReqEntries.class);
	public static String getEntryNameFromJson(String fmJson) throws IOException{
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;

		try {
			jp = f.createParser(fmJson);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			String n = jp.getCurrentName();
			jp.nextToken();
			if (jp.getText().equals("")) 
				continue;

			if (n == TopicReqPusher.Columns.COLUMN_TOPIC)
				return jp.getText();
		}
		return null;
	}
	
	/**
	 * Turns a JSON formatted TOPIC Requisites string into a storage entry
	 * Expects a string in JSON along the lines of:
	 *        {
	 *            "topic":        "healthcare",        
	 *            "min":          "80",
	 *            "max":          "80",
	 *            "time_out":     "80",
	 *        }
	 * @param fmJson The JSON formatted TopicReq entry
	 * @return The map of the storage entry
	 * @throws IOException If there was an error parsing the JSON
	 */
	public static Map<String, Object> jsonToStorageEntry(String fmJson) throws IOException {
		Map<String, Object> entry = new HashMap<String, Object>();
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;

		try {
			jp = f.createParser(fmJson);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			String n = jp.getCurrentName().toLowerCase().trim();
			jp.nextToken();


			entry.put(n, jp.getText()); /* All others are 'key':'value' pairs */

		} 

		return entry;
	} 
	
	public static Map<String, Object> topicReqToStorageEntry(TopicReq topicReq){
		Map<String, Object> entry = new HashMap<String, Object>();
		entry.put(AppReqPusher.Columns.COLUMN_TOPIC, topicReq.getTopic());
		entry.put(AppReqPusher.Columns.COLUMN_MIN, Integer.toString(topicReq.getMin()));
		entry.put(AppReqPusher.Columns.COLUMN_MAX, Integer.toString(topicReq.getMax()));
		entry.put(AppReqPusher.Columns.COLUMN_TIME_OUT, Integer.toString(topicReq.getTimeout()));

		return entry;

	}
	
	
}
