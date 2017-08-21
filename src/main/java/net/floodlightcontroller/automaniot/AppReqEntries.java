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

import net.floodlightcontroller.staticentry.StaticEntryPusher;


public class AppReqEntries {
	protected static Logger log = LoggerFactory.getLogger(AppReqEntries.class);
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

			if (n == StaticEntryPusher.Columns.COLUMN_NAME)
				return jp.getText();
		}
		return null;
	}
	
	/**
	 * Turns a JSON formatted App Requisites string into a storage entry
	 * Expects a string in JSON along the lines of:
	 *        {
	 *            "name":         "appReqHealthcare",
	 *            "topic":        "healthcare",        
	 *            "ip_src":       "192.168...",
	 *            "ip_dst":       "192.168...",
	 *            "src_port":	  "80",
	 *            "dst_port":     "80",
	 *            "min":          "80",
	 *            "max":          "80",
	 *            "time_out":     "80",
	 *        }
	 * @param fmJson The JSON formatted AppReq entry
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
	
	public static Map<String, Object> appReqToStorageEntry(AppReq appReq){
		Map<String, Object> entry = new HashMap<String, Object>();
		entry.put(AppReqPusher.Columns.COLUMN_NAME, appReq.getName());
		entry.put(AppReqPusher.Columns.COLUMN_TOPIC, appReq.getTopic());
		entry.put(AppReqPusher.Columns.COLUMN_SOURCE_IP, appReq.getSrcIP().toString());
		entry.put(AppReqPusher.Columns.COLUMN_DESTINATION_IP, appReq.getDstIP().toString());
		entry.put(AppReqPusher.Columns.COLUMN_SOURCE_ID, appReq.getSrcId().toString());
		entry.put(AppReqPusher.Columns.COLUMN_DESTINATION_ID, appReq.getDstId().toString());
		entry.put(AppReqPusher.Columns.COLUMN_SOURCE_PORT, appReq.getSrcPort().toString());
		entry.put(AppReqPusher.Columns.COLUMN_DESTINATION_PORT,  appReq.getDstPort().toString());
		entry.put(AppReqPusher.Columns.COLUMN_MIN, Integer.toString(appReq.getMin()));
		entry.put(AppReqPusher.Columns.COLUMN_MAX, Integer.toString(appReq.getMax()));
		entry.put(AppReqPusher.Columns.COLUMN_ADAPTATION_RATE_TYPE, Integer.toString(appReq.getAdaptionRateType()));
		entry.put(AppReqPusher.Columns.COLUMN_TIME_OUT, Integer.toString(appReq.getTimeout()));

		
		return entry;

	}
	
	
}
