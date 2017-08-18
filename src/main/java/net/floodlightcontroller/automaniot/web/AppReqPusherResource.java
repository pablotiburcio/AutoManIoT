package net.floodlightcontroller.automaniot.web;

import java.io.IOException;
import java.util.Map;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.automaniot.AppReqEntries;
import net.floodlightcontroller.automaniot.AppReqPusher;
import net.floodlightcontroller.storage.IStorageSourceService;

public class AppReqPusherResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(AppReqPusherResource.class);
	
	@Post
	public String store(String json) {
		IStorageSourceService storageSource =
				(IStorageSourceService)getContext().getAttributes().
				get(IStorageSourceService.class.getCanonicalName());

		Map<String, Object> rowValues;
		try {
			rowValues = AppReqEntries.jsonToStorageEntry(json);
			String status = null;           
			storageSource.insertRowAsync(AppReqPusher.TABLE_NAME, rowValues);
			status = "Entry pushed!!   "; 
			log.info("Inseriu " + rowValues);
			return status;
		} catch (IOException e) {
			log.error("Error parsing push flow mod request: " + json, e);
			return "{\"status\" : \"Error! Could not parse flow mod, see log for details.\"}";
		}        
	}

}
