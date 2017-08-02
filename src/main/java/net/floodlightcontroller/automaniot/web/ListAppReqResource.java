package net.floodlightcontroller.automaniot.web;

import java.util.HashMap;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.automaniot.AppReq;
import net.floodlightcontroller.automaniot.IAppReqPusherService;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;

public class ListAppReqResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(ListAppReqResource.class);
    
    
    
    @Get("json")
    public AppReqMap ListAppReqEntries() {
        IAppReqPusherService appReqService =
                (IAppReqPusherService)getContext().getAttributes().
                    get(IAppReqPusherService.class.getCanonicalName());
        
        
        
        String param = (String) getRequestAttributes().get("app");

        if (log.isDebugEnabled())
            log.debug("Listing requisites from app {}" + param);
        
        if (param.toLowerCase().equals("all")) {
        	log.info("Apps List {}" + appReqService.getAllAppReq());

            return new AppReqMap(appReqService.getAllAppReq());
        } else {
            try {
            	 Map<String, AppReq> appReq = new HashMap<String, AppReq>();
                 appReq.put(param, appReqService.getAppReq(param));
                 return new AppReqMap(appReq);
                
            } catch (NumberFormatException e){
                setStatus(Status.CLIENT_ERROR_BAD_REQUEST, ControllerSwitchesResource.DPID_ERROR);
            }
        }
        return null;
    }
}
