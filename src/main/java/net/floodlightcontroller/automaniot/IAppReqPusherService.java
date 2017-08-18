/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.automaniot;

import java.util.Map;

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
     * Gets all list of all flows and groups
     */
    public Map<String, AppReq> getAllAppReq();
    
    /**
     * Gets a list of flows and groups by switch
     */
    public Map<String, AppReq> getAppReq(int reqId);


	public AppReq getAppReq(String reqId);


	void addAppReq(String name, AppReq appReq);

}
