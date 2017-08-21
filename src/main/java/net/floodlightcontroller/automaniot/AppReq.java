package net.floodlightcontroller.automaniot;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.TransportPort;

public class AppReq {

	private String name;
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	//zero to bypass the values
	//""- empty to bypass the values
	private IPv4Address srcIP, dstIP;
	private DatapathId srcId, dstId;
	private TransportPort srcPort, dstPort;
	//min: Minimum value of a specific requirement
	//max: Maximum value of a specific requirement
	//timeout: timeout to evaluate the requirement
	private int min, max, timeout;
	
	//Type of adaption rate of the requirements
	//type 1: continuous - evaluates every timeout period
	//type 2: lazy - operates when a important parameter (specific to requirements) change
	//type 3: opportunistic - seize the opportunity (for example. high bw utilization in a specific topic?!) research how to do it
	private int adaptionRateType;
	private String topic;
	
	
	public AppReq(String name, String topic, IPv4Address srcIP, IPv4Address dstIP, DatapathId srcId, DatapathId dstId, TransportPort srcPort, TransportPort dstPort, int min,
			int max, int adaptationRateType, int timeout) {
		super();
		this.name = name;
		this.topic = topic;
		this.srcIP = srcIP;
		this.dstIP = dstIP;
		this.srcId = srcId;
		this.dstId = dstId;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.min = min;
		this.max = max;
		this.adaptionRateType = adaptationRateType;
		this.timeout = timeout;
	}
	
	public DatapathId getSrcId() {
		return srcId;
	}

	public void setSrcId(DatapathId srcId) {
		this.srcId = srcId;
	}

	public DatapathId getDstId() {
		return dstId;
	}

	public void setDstId(DatapathId dstId) {
		this.dstId = dstId;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	public IPv4Address getSrcIP() {
		return srcIP;
	}
	public void setSrcIP(IPv4Address srcIP) {
		this.srcIP = srcIP;
	}
	public IPv4Address getDstIP() {
		return dstIP;
	}
	public void setDstIP(IPv4Address dstIP) {
		this.dstIP = dstIP;
	}
	public TransportPort getSrcPort() {
		return srcPort;
	}
	public void setSrcPort(TransportPort srcPort) {
		this.srcPort = srcPort;
	}
	public TransportPort getDstPort() {
		return dstPort;
	}
	public void setDstPort(TransportPort dstPort) {
		this.dstPort = dstPort;
	}
	
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	
	@Override
	public String toString(){
		String toReturn;

		toReturn = this.srcIP == null ? "" : this.srcIP.toString()+" "; 
		toReturn = toReturn+= this.srcPort == null ? "" : this.srcPort.toString()+" ";
		toReturn = toReturn+= this.dstIP == null ? "" : this.dstIP.toString()+" ";
		toReturn = toReturn+= this.dstPort == null ? "" : this.dstPort.toString()+ " "; 
		toReturn = toReturn+= this.srcId == null ? "" : this.srcId.toString()+ " "; 
		toReturn = toReturn+= this.dstId == null ? "" : this.dstId.toString()+ " "; 
		toReturn = toReturn+= this.dstPort == null ? "" : this.dstPort.toString()+ " "; 
		toReturn = toReturn+= this.min+" ";
		toReturn = toReturn+= this.max+" "; 
		toReturn = toReturn+= this.adaptionRateType+" ";
		toReturn = toReturn+= this.timeout+" "; 
		return toReturn;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getAdaptionRateType() {
		return adaptionRateType;
	}

	public void setAdaptionRateType(int adaptionRateType) {
		this.adaptionRateType = adaptionRateType;
	}
	
}
