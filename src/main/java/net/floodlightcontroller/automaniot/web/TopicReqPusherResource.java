package net.floodlightcontroller.automaniot.web;

import java.io.IOException;
import java.util.Map;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.automaniot.TopicReqEntries;
import net.floodlightcontroller.automaniot.TopicReqPusher;
import net.floodlightcontroller.storage.IStorageSourceService;

public class TopicReqPusherResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(TopicReqPusherResource.class);
	
	@Post
	public String store(String json) {
		IStorageSourceService storageSource =
				(IStorageSourceService)getContext().getAttributes().
				get(IStorageSourceService.class.getCanonicalName());

		Map<String, Object> rowValues;
		try {
			rowValues = TopicReqEntries.jsonToStorageEntry(json);
			String status = null;           
			storageSource.insertRowAsync(TopicReqPusher.TABLE_NAME, rowValues);
			status = "Entry pushed!!   "; 
			log.info("Inseriu " + rowValues);
			return status;
		} catch (IOException e) {
			log.error("Error parsing push flow mod request: " + json, e);
			return "{\"status\" : \"Error! Could not parse flow mod, see log for details.\"}";
		}        
	}

}
