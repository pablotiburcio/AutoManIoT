package net.floodlightcontroller.automaniot.web;

import java.util.HashMap;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.automaniot.TopicReq;
import net.floodlightcontroller.automaniot.ITopicReqPusherService;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;

public class ListTopicReqResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(ListTopicReqResource.class);
    
    
    
    @Get("json")
    public TopicReqMap ListTopicReqEntries() {
        ITopicReqPusherService topicReqService =
                (ITopicReqPusherService)getContext().getAttributes().
                    get(ITopicReqPusherService.class.getCanonicalName());
        
        
        
        String param = (String) getRequestAttributes().get("topic");

        if (log.isDebugEnabled())
            log.debug("Listing requisites from topic {}" + param);
        
        if (param.toLowerCase().equals("all")) {
        	log.info("Topic List {}" + topicReqService.getAllTopicReq());

            return new TopicReqMap(topicReqService.getAllTopicReq());
        } else {
            try {
            	 Map<String, TopicReq> topicReq = new HashMap<String, TopicReq>();
                 topicReq.put(param, topicReqService.getTopicReq(param));
                 return new TopicReqMap(topicReq);
                
            } catch (NumberFormatException e){
                setStatus(Status.CLIENT_ERROR_BAD_REQUEST, ControllerSwitchesResource.DPID_ERROR);
            }
        }
        return null;
    }
}
