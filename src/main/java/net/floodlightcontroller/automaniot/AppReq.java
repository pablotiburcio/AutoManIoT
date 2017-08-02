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


	private IPv4Address srcIP, dstIP;
	private DatapathId srcId, dstId;
	private TransportPort srcPort, dstPort;
	private int min, max, timeout;
	
	
	public AppReq(String name, IPv4Address srcIP, IPv4Address dstIP, DatapathId srcId, DatapathId dstId, TransportPort srcPort, TransportPort dstPort, int min,
			int max, int timeout) {
		super();
		this.name = name;
		this.srcIP = srcIP;
		this.dstIP = dstIP;
		this.srcId = srcId;
		this.dstId = dstId;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.min = min;
		this.max = max;
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
		return this.srcIP.toString() + " " + this.srcPort.toString() + " " + this.dstIP.toString() + " " 
				+ this.dstPort.toString() + " " + this.srcId.toString() + " " + this.dstId.toString() + " " 
				+ this.dstPort.toString() + " " + 
				+ this.min + "/" + this.max + " " + this.timeout; 
	}
	
}
