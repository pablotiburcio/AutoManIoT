package net.floodlightcontroller.automaniot;

import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IAppReqPusherService extends IFloodlightService {
 
    
    /**
     * Deletes a static flow or group entry
     * @param name The name of the static flow to delete.
     */
    public void deleteAppReq(String name);
    
    /**
     * Deletes all flows and groups.
     */
    public void deleteAllAppReq();
    
    /**
     * Updates all flows and groups.
     */
    public void updateAppReq(String name, AppReq appreq);
    
    /**
     * Gets all list of all flows and groups
     */
    public Map<String, AppReq> getAllAppReq();
    
    /**
     * Gets a list of flows and groups by switch
     */
    public Map<String, AppReq> getAppReq(int reqId);


	public AppReq getAppReq(String reqId);
	
	
	public Set<String> getAllTopics();

	public boolean containsValue(AppReq appReq);
	
	public boolean containsKey(String key);

	void addAppReq(String name, AppReq appReq);
	
}
