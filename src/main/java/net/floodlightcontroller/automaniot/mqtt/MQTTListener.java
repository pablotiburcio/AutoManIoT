package net.floodlightcontroller.automaniot.mqtt;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP; 

public class MQTTListener {
	protected static Logger log = LoggerFactory.getLogger(MQTTListener.class);
	
	public MQTTListener() {
		//MqttClient client = new MqttClient(
		//)
	}
	
	void getMessageType(Ethernet eth){
		MqttWireMessage mwm = getMqttWireMessage(eth);
		mwm.getType();
		
	}
	
		
	MqttWireMessage getMqttWireMessage(Ethernet eth){
		if (isMqttMessage(eth)){
			TCP tcp = (TCP) eth.getPayload().getPayload();
			if (tcp.getPayload().serialize().length>0){
				try {
					return MqttWireMessage.createWireMessage(tcp.getPayload().serialize());
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else { // verificar como capturar pacotes do tipo nao PUBLISH (Subscribe, ack, por exemplo)
				IPv4 ipv4 = (IPv4) eth.getPayload();
				log.info("Payload < 0 mas eh mqtt {} {}", ipv4.getSourceAddress(), ipv4.getDestinationAddress()); 
				
				log.info("TCP {}", tcp.getPayload().getPayload());
				//MqttWireMessage mwm = MqttWireMessage.createWireMessage(tcp.getPayload().serialize());
			}
			
		}
		return null;
		
	}
	
	//Identified by TCP/IP protocol and 1883 port 
	boolean isMqttMessage(Ethernet eth){
		if(eth.getEtherType() == EthType.IPv4){ //Do it to IPv6 mqttMessages
			IPv4 ipv4 = (IPv4) eth.getPayload();
			if (ipv4.getProtocol() == IpProtocol.TCP){
				TCP tcp = (TCP) ipv4.getPayload();
				return	(tcp.getSourcePort().equals(TransportPort.of(1883))
						|| tcp.getDestinationPort().equals(TransportPort.of(1883)));
				}
		}
		return false;
	}
	
	void printSrcDstPort(Ethernet eth){
		if (isMqttMessage(eth)){
			TCP tcp = (TCP) eth.getPayload().getPayload();
			log.info("TCP src port {}, dst {}",tcp.getSourcePort(), tcp.getDestinationPort());
		}
	}
	
	void printSrcDstIp(Ethernet eth){
		if (isMqttMessage(eth)){
			IPv4 ipv4 = (IPv4) eth.getPayload();
			log.info("IP src ip {}, dst ip {}", ipv4.getSourceAddress(), ipv4.getDestinationAddress());
		}
	}
}
