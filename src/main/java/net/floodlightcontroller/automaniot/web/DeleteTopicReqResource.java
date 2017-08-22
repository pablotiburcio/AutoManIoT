package net.floodlightcontroller.automaniot.web;


import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.automaniot.ITopicReqPusherService;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;

public class DeleteTopicReqResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(DeleteTopicReqResource.class);
    
    
    
    @Get("json")
    public String DeleteTopicReqEntries() {
        ITopicReqPusherService topicReqService =
                (ITopicReqPusherService)getContext().getAttributes().
                    get(ITopicReqPusherService.class.getCanonicalName());
        
        
        
        String param = (String) getRequestAttributes().get("topic");

        if (log.isDebugEnabled())
            log.debug("Listing requisites from topics {}" + param);
        
        if (param.toLowerCase().equals("all")) {
        	
        	topicReqService.deleteAllTopicReq();
            return "{\"status\":\"Deleted all Topics Requisites.\"}";
        } else {
            try {
                 topicReqService.deleteTopicReq(param);
                 return  "{\"status\":\"Deleted {} TopicReq.\"}" + param;
                
            } catch (NumberFormatException e){
                setStatus(Status.CLIENT_ERROR_BAD_REQUEST, ControllerSwitchesResource.DPID_ERROR);
            }
        }
        return null;
    }
}
