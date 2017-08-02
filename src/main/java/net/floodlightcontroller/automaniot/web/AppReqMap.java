package net.floodlightcontroller.automaniot.web;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.automaniot.AppReq;
import net.floodlightcontroller.core.web.serializers.AppReqMapSerializer;

@JsonSerialize(using=AppReqMapSerializer.class) 
public class AppReqMap {

	/*
	 * Contains the following double-mapping:
	 * Map<Switch-DPID-Str, Map<Entry-Name-Str, AppReq>>
	 */
	private Map<String, AppReq> theMap;
	
	public AppReqMap (Map<String, AppReq> theMap) {
		this.theMap = theMap;
	}
	
	public Map<String, AppReq> getMap() {
		return theMap;
	}
}
