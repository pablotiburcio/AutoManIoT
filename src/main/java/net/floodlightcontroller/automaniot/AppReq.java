package net.floodlightcontroller.automaniot;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

import sun.java2d.x11.X11SurfaceDataProxy.Opaque;

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
	private TransportPort srcTransPort, dstTransPort;
	private OFPort ofSrcPort, ofDstPort;
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
	
	
	public AppReq(String name, String topic, IPv4Address srcIP, IPv4Address dstIP, DatapathId srcId,  DatapathId dstId, OFPort ofSrcPort, OFPort ofDstPort, TransportPort srcTransPort, TransportPort dstTransPort, int min,
			int max, int adaptationRateType, int timeout) {
		super();
		
		this.topic = topic;
		this.srcIP = srcIP;
		this.dstIP = dstIP;
		this.srcId = srcId;
		this.dstId = dstId;
		this.ofSrcPort = ofSrcPort;
		this.ofDstPort = ofDstPort;
		this.srcTransPort = srcTransPort;
		this.dstTransPort = dstTransPort;
		this.min = min;
		this.max = max;
		this.adaptionRateType = adaptationRateType;
		this.timeout = timeout;
		this.name = name+this.hashCode();

	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + adaptionRateType;
		result = prime * result + ((dstIP == null) ? 0 : dstIP.hashCode());
		result = prime * result + ((dstId == null) ? 0 : dstId.hashCode());
		result = prime * result + ((dstTransPort == null) ? 0 : dstTransPort.hashCode());
		result = prime * result + max;
		result = prime * result + min;
		result = prime * result + ((ofDstPort == null) ? 0 : ofDstPort.hashCode());
		result = prime * result + ((ofSrcPort == null) ? 0 : ofSrcPort.hashCode());
		result = prime * result + ((srcIP == null) ? 0 : srcIP.hashCode());
		result = prime * result + ((srcId == null) ? 0 : srcId.hashCode());
		result = prime * result + ((srcTransPort == null) ? 0 : srcTransPort.hashCode());
		result = prime * result + timeout;
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AppReq other = (AppReq) obj;
		if (adaptionRateType != other.adaptionRateType)
			return false;
		if (dstIP == null) {
			if (other.dstIP != null)
				return false;
		} else if (!dstIP.equals(other.dstIP))
			return false;
		if (dstId == null) {
			if (other.dstId != null)
				return false;
		} else if (!dstId.equals(other.dstId))
			return false;
		if (dstTransPort == null) {
			if (other.dstTransPort != null)
				return false;
		} else if (!dstTransPort.equals(other.dstTransPort))
			return false;
		if (max != other.max)
			return false;
		if (min != other.min)
			return false;
		if (ofDstPort == null) {
			if (other.ofDstPort != null)
				return false;
		} else if (!ofDstPort.equals(other.ofDstPort))
			return false;
		if (ofSrcPort == null) {
			if (other.ofSrcPort != null)
				return false;
		} else if (!ofSrcPort.equals(other.ofSrcPort))
			return false;
		if (srcIP == null) {
			if (other.srcIP != null)
				return false;
		} else if (!srcIP.equals(other.srcIP))
			return false;
		if (srcId == null) {
			if (other.srcId != null)
				return false;
		} else if (!srcId.equals(other.srcId))
			return false;
		if (srcTransPort == null) {
			if (other.srcTransPort != null)
				return false;
		} else if (!srcTransPort.equals(other.srcTransPort))
			return false;
		if (timeout != other.timeout)
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
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
	public TransportPort getSrcTransPort() {
		return srcTransPort;
	}
	public void setSrcTransPort(TransportPort srcPort) {
		this.srcTransPort = srcPort;
	}
	public TransportPort getDstTransPort() {
		return dstTransPort;
	}
	public void setDstTransPort(TransportPort dstPort) {
		this.dstTransPort = dstPort;
	}
	
	public OFPort getSrcPort() {
		return ofSrcPort;
	}
	public void setSrcPort(OFPort srcPort) {
		this.ofSrcPort = srcPort;
	}
	public OFPort getDstPort() {
		return ofDstPort;
	}
	public void setDstPort(OFPort dstPort) {
		this.ofDstPort = dstPort;
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
		toReturn = this.name + " ";
		toReturn = toReturn+= this.srcIP == null ? "" : this.srcIP.toString()+" "; 
		toReturn = toReturn+= this.srcTransPort == null ? "" : this.srcTransPort.toString()+" ";
		toReturn = toReturn+= this.dstIP == null ? "" : this.dstIP.toString()+" ";
		toReturn = toReturn+= this.dstTransPort == null ? "" : this.dstTransPort.toString()+ " "; 
		toReturn = toReturn+= this.srcId == null ? "" : this.srcId.toString()+ " "; 
		toReturn = toReturn+= this.dstId == null ? "" : this.dstId.toString()+ " "; 
		toReturn = toReturn+= this.ofSrcPort == null ? "" : this.ofSrcPort.toString()+ " "; 
		toReturn = toReturn+= this.ofDstPort == null ? "" : this.ofDstPort.toString()+ " "; 		
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
	
	public static void main(String[] args) {
		AppReq app1 = new AppReq("name", "topic", IPv4Address.of("10.0.0.1"), IPv4Address.of("10.0.0.2"),
				DatapathId.of(1L), DatapathId.of(2L), OFPort.of(1), OFPort.of(2), 
				TransportPort.of(1), TransportPort.of(2), 5, 10, 2, 10);
		
		AppReq app2 = new AppReq("name", "topic", IPv4Address.of("10.0.0.1"), IPv4Address.of("10.0.0.2"),
				DatapathId.of(1L), DatapathId.of(2L), OFPort.of(1), OFPort.of(2), 
				TransportPort.of(1), TransportPort.of(2), 5, 10, 2, 10);
		
		
		System.out.println("app1="+app1.toString()+ " hashCode="+ app1.hashCode());
		System.out.println("app2="+app2.toString()+ " hashCode="+ app2.hashCode());
	}
	
}
