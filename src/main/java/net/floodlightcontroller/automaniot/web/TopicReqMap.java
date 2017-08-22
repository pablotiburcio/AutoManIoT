package net.floodlightcontroller.automaniot.web;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.automaniot.TopicReq;
import net.floodlightcontroller.core.web.serializers.TopicReqMapSerializer;

@JsonSerialize(using=TopicReqMapSerializer.class) 
public class TopicReqMap {

	/*
	 * Contains the following double-mapping:
	 * Map<Switch-DPID-Str, Map<Entry-Name-Str, AppReq>>
	 */
	private Map<String, TopicReq> theMap;
	
	public TopicReqMap (Map<String, TopicReq> theMap) {
		this.theMap = theMap;
	}
	
	public Map<String, TopicReq> getMap() {
		return theMap;
	}
}
