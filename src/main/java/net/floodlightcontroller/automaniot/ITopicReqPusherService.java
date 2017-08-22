package net.floodlightcontroller.automaniot;

import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ITopicReqPusherService extends IFloodlightService {
 
    
    /**
     * Deletes a static flow or group entry
     * @param name The name of the static flow to delete.
     */
    public void deleteTopicReq(String name);
    
    /**
     * Deletes all flows and groups.
     */
    public void deleteAllTopicReq();
    
    /**
     * Gets all list of all flows and groups
     */
    public Map<String, TopicReq> getAllTopicReq();
    
    /**
     * Gets a list of flows and groups by switch
     */
    public Map<String, TopicReq> getTopicReq(int reqId);


	public TopicReq getTopicReq(String reqId);
	
	
	 /**
     * Gets the first occurrence of topicReq from topic name
     */
	public TopicReq getTopicReqFromTopic(String topic);
	
	
	public Set<String> getAllTopics();


	void addTopicReq(String name, TopicReq topicReq);

}
