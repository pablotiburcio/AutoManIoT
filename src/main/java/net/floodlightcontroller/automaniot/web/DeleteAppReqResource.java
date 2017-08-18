package net.floodlightcontroller.automaniot.web;


import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.automaniot.IAppReqPusherService;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;

public class DeleteAppReqResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(DeleteAppReqResource.class);
    
    
    
    @Get("json")
    public String DeleteAppReqEntries() {
        IAppReqPusherService appReqService =
                (IAppReqPusherService)getContext().getAttributes().
                    get(IAppReqPusherService.class.getCanonicalName());
        
        
        
        String param = (String) getRequestAttributes().get("app");

        if (log.isDebugEnabled())
            log.debug("Listing requisites from app {}" + param);
        
        if (param.toLowerCase().equals("all")) {
        	
        	appReqService.deleteAllAppReq();
            return "{\"status\":\"Deleted all Apps Requisites.\"}";
        } else {
            try {
                 appReqService.deleteAppReq(param);
                 return  "{\"status\":\"Deleted {} AppReq.\"}" + param;
                
            } catch (NumberFormatException e){
                setStatus(Status.CLIENT_ERROR_BAD_REQUEST, ControllerSwitchesResource.DPID_ERROR);
            }
        }
        return null;
    }
}
