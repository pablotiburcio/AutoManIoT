package net.floodlightcontroller.automaniot;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;


import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.core.IFloodlightProviderService;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import java.util.Set;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class AutoManIoT implements IOFMessageListener, IFloodlightModule, IStorageSourceListener, IOFSwitchListener, ITopologyListener {
	protected static Logger log = LoggerFactory.getLogger(AutoManIoT.class);

	
	protected IFloodlightProviderService floodlightProvider;
	protected IAppReqPusherService appReqService;
	protected ITopicReqPusherService topicReqService;
	protected IStorageSourceService storageSourceService;
	protected ILinkDiscoveryService linkDiscoveryService;
	protected IOFSwitchService switchService;
	protected IRoutingService routingService;
	protected IDeviceService deviceService;
	protected ITopologyService topologyService;
	protected Set<Long> macAddresses;
	protected IOFIoTRouting iotRouting;
	protected static Logger logger;
	protected Map<String, ScheduledFuture<?>> scheduledFutureMap;
	protected ScheduledThreadPoolExecutor threadPool;
	
	class SimulateNodes implements Runnable {
		int nnodesToSimulate;
		int nnodesSimulated;
		int adaptationRate;
		
		///Nodos para simular e nodos ja simulados no mininet
		//Multiplos de 1000
		public SimulateNodes(int nnodesToSimulate, int nnodesSimulated, int adaptationRate) {
			this.nnodesSimulated = nnodesSimulated;
			this.nnodesToSimulate = nnodesToSimulate;
			this.adaptationRate = adaptationRate;
		}
		
		public void run() {
			//Registra AppReqs para simulacao maior que  >1000 nodos
			//Simula os nodos a partir do registro de cada um deles (a partir de 1000 nodos), pois acima de 1000 nodos, mininet consome muita memoria >8Gb
			IPv4 ipv4 = new IPv4();

			//ip 10.0.0.0 = 167772160
			int intIp = 167772161; //10.0.0.1
			ipv4.setDestinationAddress(167772161);


			//log.info("ip={}", ipv4.getSourceAddress());

			String[] appType = {"Temperature", "Luximeter", "AirMonitoring", "NoiseMonitoring", "TrafficCongestion", "Humidity", "CarPresence"};


			TCP tcp = new TCP();
			tcp.setDestinationPort(1883);

			int toSimulate = nnodesToSimulate - nnodesSimulated;
			int eachThousand = toSimulate/1000;
			for (int j=0; j<eachThousand; j++) {
				intIp = 167772161;
				tcp.setSourcePort(5050+j); 
				for (int i=0; i<1000; i++) {
					int app = new Random().nextInt(7);
					//log.info("i={}", i);

					ipv4.setSourceAddress(intIp++);
					AppReq ar = new AppReq(appType[app]
							, appType[app], ipv4.getSourceAddress(), ipv4.getDestinationAddress(),
							DatapathId.of(5L), DatapathId.of(6L),
							OFPort.of(1), OFPort.of(1), tcp.getSourcePort(), tcp.getDestinationPort(), 1, 50, adaptationRate, 60);
					appReqService.addAppReq(AppReqPusher.TABLE_NAME, ar);
					log.info("appReq {} adicionada em SimulateNodes {}", i+(1000*j), ar.toString());

				}
			}
		}
	}
	
	class ContinuousDelayMonitor implements Runnable {
		private AppReq appReq;
		ContinuousDelayMonitor(AppReq ar){
			appReq = ar;
		}
		public void run(){

			//log.info("Monitoring app:{} in continuous mode, at each {}s", appReq.getName(), appReq.getTimeout());
			//log.info("Monitoring app:{}", appReq.toString());
			//TODO: Corrigir: latencia mais alta do que a definida no mininet (alem de estar variando bastante). Sugestao pacote de sinalizacao.
			//log.info("links: {}", linkDiscoveryService.getLinks());

			//TODO: Corrigir: problema: todos hosts tem que dar um ping na rede para o floodlight cadastrar seu IP
			//Isso altera as regras aplicadas aos roteadores (verificar)

			log.info("Monitoring app:{} at continuous time {}s", appReq.getName(), appReq.getTimeout());

			iotRouting.applyLowerLatencyPath(appReq);

		}
	}
	
	class LazyDelayMonitor implements Runnable, ITopologyListener {
		private AppReq appReq;
		LazyDelayMonitor(AppReq ar){
			appReq = ar;
		}
		public void run(){
			
			log.info("Monitoring app:{} at topology change ", appReq.getName());
			
			iotRouting.applyLowerLatencyPath(appReq);
			//topologyService.getAllLinks();
			//linkDiscoveryService.getLinkInfo(l);			
		}
		
		@Override
		public void topologyChanged(List<LDUpdate> linkUpdates) {
			iotRouting.applyLowerLatencyPath(appReq);
			
		}
	}

	
	//Listener to add or update in reqTable
	public void appReqAddListener(String name){
		if (appReqService.getAppReq(name) != null) {
			AppReq appReq = appReqService.getAppReq(name);
			//If timeout = 0, do not monitor
			if (appReq.getTimeout()==0) {
					return ;
			}
				
			if (appReq.getAdaptionRateType()==1){ //continuous
				//Execute Delay Monitor at each X sec from timeout in ReqTable
				scheduledFutureMap.put(name, threadPool.scheduleAtFixedRate(new ContinuousDelayMonitor(appReq), 0, appReq.getTimeout(), TimeUnit.SECONDS));
				//		    	try {
				//					scheduledFutureMap.get(name).get();
				//				} catch (InterruptedException | ExecutionException e) {
				//					// TODO Auto-generated catch block
				//					log.info("thread error:");
				//					e.printStackTrace();
				//				}

			//TODO: Decidir qual melhor metodo: Lazy could be:
			// -receive new message -> verify -> apply new route -> send packet or 
			// -receive new message -> verify -> apply new route -> send packet -> apply continuous monitoring
			} else if (appReq.getAdaptionRateType()==2){ //lazy
				//first: was already done in IoTRouting
				
				//second: apply continuous monitoring after timeout
				//scheduledFutureMap.put(name, threadPool.scheduleAtFixedRate(new ContinuousDelayMonitor(appReq), appReq.getTimeout(), appReq.getTimeout(), TimeUnit.SECONDS));
				LazyDelayMonitor lazyDelayMonitor = new LazyDelayMonitor(appReq);
				topologyService.addListener(lazyDelayMonitor);
				//scheduledFutureMap.put(name, threadPool.scheduleAtFixedRate(lazyDelayMonitor, 0, appReq.getTimeout(), TimeUnit.SECONDS));
				scheduledFutureMap.put(name, threadPool.schedule(lazyDelayMonitor, 0, TimeUnit.SECONDS));
				
			}
		}
	}
	
	//Listener to del in reqTable
	public void appReqDeleteListener(String name){
	    //TODO fix it: appReqService.getAppReq was deleted before enter here
		//if (appReqService.getAppReq(name) != null) {

	    	//log.info("scheduledFutureMap {}", scheduledFutureMap.get(name));
	    	//Remove scheduled thread from app and if its ok, remove it from scheduledFutureMap 
	    if (scheduledFutureMap.get(name) != null)	
	    	if (scheduledFutureMap.get(name).cancel(false))
	    		scheduledFutureMap.remove(name);
	    	
	    
	}
	
	@Override
	public String getName() {
		return AutoManIoT.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		//log.info("type of message pre, type = {}, name = {}",type,name);
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		if (name.equals("forwarding")){
			return true;
		}
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(ILinkDiscoveryService.class);
		l.add(ITopologyService.class);
		l.add(IOFSwitchService.class);
		//l.add(IOFIoTRouting.class);

		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		linkDiscoveryService = context.getServiceImpl(ILinkDiscoveryService.class);
		topologyService = context.getServiceImpl(ITopologyService.class);
		routingService = context.getServiceImpl(IRoutingService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		deviceService = context.getServiceImpl(IDeviceService.class);
		appReqService = context.getServiceImpl(IAppReqPusherService.class);
		topicReqService = context.getServiceImpl(ITopicReqPusherService.class);
		iotRouting = context.getServiceImpl(IOFIoTRouting.class);
		macAddresses = new ConcurrentSkipListSet<Long>();
		logger = LoggerFactory.getLogger(AutoManIoT.class);
		storageSourceService = context.getServiceImpl(IStorageSourceService.class);
    	threadPool = new ScheduledThreadPoolExecutor(1);
    	scheduledFutureMap = new HashMap<String, ScheduledFuture<?>>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
	    floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	    storageSourceService.addListener(AppReqPusher.TABLE_NAME, this);
		switchService.addOFSwitchListener(this);
		topologyService.addListener(this);

	    //linkDiscoveryService.

	    log.info("Starting AutoManIoT...");
	    
	    int adaptationRate = 1;
	    
	    //SimulateNodes sn = new SimulateNodes(3000, 1000, adaptationRate);
		//scheduledFutureMap.put("simulationNodes", threadPool.schedule(sn, 100, TimeUnit.SECONDS));

	    TopicReq tr = new TopicReq("Temperature", 1, adaptationRate, 10, 100, 60);
		topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);
		tr = new TopicReq("Luximeter", 1, adaptationRate, 10, 100, 60);
		topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);
		tr = new TopicReq("AirMonitoring", 1, adaptationRate, 10, 100, 60);
		topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);
		tr = new TopicReq("NoiseMonitoring	", 1, adaptationRate, 10, 100, 60);
		topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);
		tr = new TopicReq("TrafficCongestion", 1, adaptationRate, 10, 100, 60);
		topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);
		tr = new TopicReq("Humidity", 1, adaptationRate, 10, 100, 60);
		topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);
		tr = new TopicReq("CarPresence", 1, adaptationRate, 10, 100, 60);
		topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);
		
		//tr = new TopicReq("healthcare", 1, 1, 10, 100, 10);
		//topicReqService.addTopicReq(TopicReqPusher.TABLE_NAME, tr);

	    
		
	}
	
	
	

	

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		
		OFPacketIn packetIn = (OFPacketIn) msg;


		//log.info("Mensagem Recebida em automaniot{}", msg);
		//log.info("Porta de origem {}", packetIn.getInPort());
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		//log.info("Eth src {}, dst {}", eth.getSourceMACAddress(), eth.getDestinationMACAddress());
		//log.info("Eth type {}", eth.getEtherType());
		
		//IOFSwitch ofSwitch = switchService.getSwitch(sw.getId());
		
		
		/*if(eth.getEtherType() == EthType.IPv4){
			IPv4 ipv4 = (IPv4) eth.getPayload();
			log.info("IP src ip {}, dst ip {}", ipv4.getSourceAddress(), ipv4.getDestinationAddress());
			if (ipv4.getProtocol() == IpProtocol.TCP){
				TCP tcp = (TCP) ipv4.getPayload();
				log.info("TCP src port {}, dst {}",tcp.getSourcePort(), tcp.getDestinationPort());
				try {
					if (ipv4.getDestinationAddress().equals(IPv4Address.of("10.0.0.1")) 
							 && (tcp.getDestinationPort().equals(TransportPort.of(1883))) 
							 && (tcp.getPayload().serialize().length>0)){
						
						MqttWireMessage mqttWireMessage = MqttWireMessage.createWireMessage(tcp.getPayload().serialize());
						
						//if (mqttWireMessage.getType() == MqttWireMessage.MESSAGE_TYPE_PUBLISH){
						if (mqttWireMessage instanceof MqttPublish){
							MqttPublish mqttpublish = (MqttPublish) mqttWireMessage;
							log.info("MqttPublish Message {}", mqttpublish.getMessage());
							log.info("MqttPublish Topic  {}",mqttpublish.getTopicName());
	
						}
					} else if (ipv4.getSourceAddress().equals(IPv4Address.of("10.0.0.1")) 
							 && (tcp.getSourcePort().equals(TransportPort.of(1883))) 
							 && (tcp.getPayload().serialize().length>0)) {
						MqttWireMessage mqttWireMessage = MqttWireMessage.createWireMessage(tcp.getPayload().serialize());
							log.info("--------------------------------");
							log.info("MqttSub TOPICCCCC {}", mqttWireMessage.toString());
							log.info("---------------------------------");
						//if (mqttWireMessage.getType() == MqttWireMessage.MESSAGE_TYPE_PUBLISH){
						if (mqttWireMessage instanceof MqttSubscribe){
							MqttSubscribe mqttsubscribe = (MqttSubscribe) mqttWireMessage;
							log.info("--------------------------------");
							log.info("MqttSub TOPICCCCC {}", mqttsubscribe.toString());
							log.info("---------------------------------");
							//log.info("TCP  topic  {}",mqttsubscribe.);
						}
					}
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.info("Cause",   e.getCause());
				}


			}
		
		
		} else if (eth.getEtherType() == EthType.ARP){
			ARP arp = (ARP) eth.getPayload();
			log.info("ARP protocol type {}",arp.getProtocolType());
			
		}*/
		
		/*MQTTListener ml = new MQTTListener();
		MqttWireMessage mwm = ml.getMqttWireMessage(eth);
		
		if (mwm!=null){
			ml.printSrcDstIp(eth);
			ml.printSrcDstPort(eth);
			log.info("Mqtt Type {}", mwm.getType());	
			
			switch (mwm.getType()){
				case 3 : 
					MqttPublish mPublish = (MqttPublish) mwm;
					log.info("Mqtt Topic Publish {}", mPublish.getTopicName());
					break;
				case 8 : 
					MqttSubscribe mSubscribe = (MqttSubscribe) mwm;
					log.info("Mqtt Topic Subscribe {}", mSubscribe.toString());
					break;
				case 10 : 
					MqttUnsubscribe mUnsubscribe = (MqttUnsubscribe) mwm;
					log.info("Mqtt Topic Unsubscribe {}", mUnsubscribe.toString());
					log.info("Mqtt Topic Unsubscribe mwwm {}", mwm.toString());
					break;
			}
		}*/

		/*if (MqttUtils.isMqttMessage(eth)){
			
			IPv4 ipv4 = (IPv4) eth.getPayload();
			log.info("IP src ip {}, dst ip {}", ipv4.getSourceAddress(), ipv4.getDestinationAddress());
			
			MQTTDecoder mdecoder = new MQTTDecoder();
			List<Object> m_results;
			m_results = new ArrayList<Object >();

			ByteBuf m_buffer = Unpooled.copiedBuffer(eth.getPayload().getPayload().getPayload().serialize());
			try {
				mdecoder.decode(null, m_buffer, m_results);
				if (!m_results.isEmpty()){ 
					log.info("byte type {}", mdecoder.getMessageType());
					switch (mdecoder.getMessageType()){
					case AbstractMessage.PUBLISH : 
						PublishMessage mPublish = (PublishMessage) m_results.get(0);
						log.info("Mqtt Topic Publish {}", mPublish.getTopicName());
						break;
					case AbstractMessage.SUBSCRIBE : 
						SubscribeMessage mSubscribe = (SubscribeMessage) m_results.get(0);
						log.info("Mqtt Topic Subscribe {}", mSubscribe.toString());
						break;
					case AbstractMessage.UNSUBSCRIBE : 
						UnsubscribeMessage mUnsubscribe = (UnsubscribeMessage) m_results.get(0);
						log.info("Mqtt Topic Unsubscribe {}", mUnsubscribe.topics());
						break;
					case AbstractMessage.UNSUBACK: 
						UnsubAckMessage mUnsuback = (UnsubAckMessage) m_results.get(0);
						log.info("Mqtt Topic UnsubAck {}", mUnsuback.toString());
						break;
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
	
		
		//log.info("Porta TCP {}", packetIn.getInPort());
		
		//int i=0;
		//for (AppReq a : reqTable){
		//	logger.info("Result of reqTable {}: {}", i++, a.toString());
		//}
		
		//Long sourceMACHash = eth.getSourceMACAddress().getLong();
		//if (!macAddresses.contains(sourceMACHash)) {
		//	macAddresses.add(sourceMACHash);
			//logger.info("MAC Address: {} seen on switch: {}",
			//		eth.getSourceMACAddress().toString(),
			//		sw.getId().toString());
		//}
		return Command.CONTINUE;
	}

	@Override
	public void rowsModified(String tableName, Set<Object> rowKeys) {
		for (Object key : rowKeys){
			appReqAddListener(key.toString());
	
		}
	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
		for (Object key : rowKeys){
			appReqDeleteListener(key.toString());
	
		}
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchDeactivated(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		
	}

	
	/*public Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		
		return Command.CONTINUE;
	}*/

}
