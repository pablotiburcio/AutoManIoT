package net.floodlightcontroller.automaniot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMatchBmap;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.SwitchDescription;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.forwarding.Forwarding;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.core.IFloodlightProviderService;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.MidiDevice.Info;

import java.util.Set;

import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.IRoutingService.PATH_METRIC;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.TopologyInstance;
import net.floodlightcontroller.topology.TopologyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedLong;

public class AutoManIoT implements IOFMessageListener, IFloodlightModule, IStorageSourceListener, IOFSwitchListener {
	protected static Logger log = LoggerFactory.getLogger(AutoManIoT.class);

	
	protected IFloodlightProviderService floodlightProvider;
	protected IAppReqPusherService appReqService;
	protected IStorageSourceService storageSourceService;
	protected ILinkDiscoveryService linkDiscoveryService;
	protected IOFSwitchService switchService;
	protected IRoutingService routingService;
	protected ITopologyService topologyService;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	//protected Set<AppReq> reqTable;
	protected Map<String, AppReq> appReqMap;
	protected Map<String, ScheduledFuture<?>> scheduledFutureMap;
	protected ScheduledThreadPoolExecutor threadPool;
	
	
	
	class DelayMonitor implements Runnable {
		private AppReq appReq;
		DelayMonitor(AppReq ar){
			appReq = ar;
		}
		public void run(){
			
			log.info("Monitoring app:{} delay each {}s", appReq.getName(), appReq.getTimeout());
			//TODO: Corrigir latencia mais alta do que a definida no mininet (alem de esta variando bastante). Sugestao pacote de sinalizacao.
			//log.info("links: {}", linkDiscoveryService.getLinks());
			
			//DatapathId srcId = new DatapathId();
			//Link l = new Link(appReq.getSrcId(), OFPort.ofInt(Integer.valueOf(appReq.getSrcPort().toString())), appReq.getDstId(), 
			///		OFPort.ofInt(Integer.valueOf(appReq.getDstPort().toString())), U64.of(0L));

			Path p = routingService.getPath(appReq.getSrcId(), appReq.getDstId());
			U64 lat = p.getLatency();
			if (lat != null){
				if (lat.getValue() > appReq.getMax()){
					log.info("PAth Atual {}", p);
					log.info("Latencia Atual {}", lat.getValue());
					log.info("Trying to Set new latency...");
					setLowerLatencyPath(appReq);
				}
			} 
			
			//topologyService.getAllLinks();
			//linkDiscoveryService.getLinkInfo(l);
			//new SwitchDescription();
			
		}
		//TODO: Definir uma % de variacao para nao mudar a rota a todo instante 
		//Aplica a rota de menor latencia para a app 
		private boolean setLowerLatencyPath(AppReq appReq) {
			IOFSwitch ofSwitch;
			//Match match;
			//OFActionOutput action;
			//List<OFAction> actionList = new ArrayList<OFAction>();
			int idleTimeout = 0;
			int hardTimeout = 0;
			int priority = 1;
			
			Path p = routingService.getPath(appReq.getSrcId(), appReq.getDstId(), PATH_METRIC.LATENCY);
			
			if(p.getLatency() != null){
				log.info("Nova latencia {}", p.getLatency().getValue());
			} else {
				return false;
			}
			
			
			//O 1o salto so tem destino, pois a rota e calculada pelo MAC/DatapathId e nao por IP
			NodePortTuple firstNodePortTuple = p.getPath().get(0); 
				ofSwitch = switchService.getSwitch(firstNodePortTuple.getNodeId());					
			
			if (ofSwitch != null) {  // is the switch connected
				if (log.isDebugEnabled()) {
					log.debug("Sending 1 new entries to {}", ofSwitch.getId().toString());
				}
				OFFactory factory = ofSwitch.getOFFactory();
				Match match = factory.buildMatch()
						.setExact(MatchField.IN_PORT, OFPort.of(Integer.valueOf(appReq.getSrcPort().toString())))
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_SRC, appReq.getSrcIP())
						.setExact(MatchField.IPV4_DST, appReq.getDstIP())
						.build();
				
				OFActionOutput action = factory.actions().buildOutput()
						.setPort(firstNodePortTuple.getPortId())
						.build();
				
				List<OFAction> actionList = new ArrayList<OFAction>();
				actionList.clear();
				actionList.add(action);

				OFFlowMod fm = factory.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match)
						.setActions(actionList)
						.setOutPort(firstNodePortTuple.getPortId())
						.build();
				
				ofSwitch.write(fm);
				
				//ARP
				OFFactory factory_arp = ofSwitch.getOFFactory();
				Match match_arp = factory_arp.buildMatch()
						.setExact(MatchField.IN_PORT, OFPort.of(Integer.valueOf(appReq.getSrcPort().toString())))
						.setExact(MatchField.ETH_TYPE, EthType.ARP)
						.setExact(MatchField.IPV4_SRC, appReq.getSrcIP())
						.setExact(MatchField.IPV4_DST, appReq.getDstIP()).build();

				OFActionOutput action_arp = factory_arp.actions().buildOutput().setPort(firstNodePortTuple.getPortId())
						.build();

				List<OFAction> actionList_arp = new ArrayList<OFAction>();
				actionList_arp.clear();
				actionList_arp.add(action_arp);

				OFFlowMod fm_arp = factory_arp.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match_arp)
						.setActions(actionList_arp)
						.setOutPort(firstNodePortTuple.getPortId()).build();

				ofSwitch.write(fm_arp);
			
				//bidirecional
				OFFactory factory1 = ofSwitch.getOFFactory();
				Match match1 = factory1.buildMatch()
						.setExact(MatchField.IN_PORT, firstNodePortTuple.getPortId())
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_SRC, appReq.getDstIP())
						.setExact(MatchField.IPV4_DST, appReq.getSrcIP())
						.build();
				
				OFActionOutput action1 = factory1.actions().buildOutput()
						.setPort(OFPort.of(Integer.valueOf(appReq.getSrcPort().toString())))
						.build();
				
				List<OFAction> actionList1 = new ArrayList<OFAction>();
				actionList1.clear();
				actionList1.add(action1);

				OFFlowMod fm1 = factory1.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match1)
						.setActions(actionList1)
						.setOutPort(action1.getPort())
						.build();
				
				ofSwitch.write(fm1);
				
				//ARP
				OFFactory factory1_arp = ofSwitch.getOFFactory();
				Match match1_arp = factory1_arp.buildMatch()
						.setExact(MatchField.IN_PORT, firstNodePortTuple.getPortId())
						.setExact(MatchField.ETH_TYPE, EthType.ARP)
						.setExact(MatchField.IPV4_SRC, appReq.getDstIP())
						.setExact(MatchField.IPV4_DST, appReq.getSrcIP())
						.build();
				
				OFActionOutput action1_arp = factory1_arp.actions().buildOutput()
						.setPort(OFPort.of(Integer.valueOf(appReq.getSrcPort().toString())))
						.build();
				
				List<OFAction> actionList1_arp = new ArrayList<OFAction>();
				actionList1_arp.clear();
				actionList1_arp.add(action1_arp);

				OFFlowMod fm1_arp = factory1_arp.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match1_arp)
						.setActions(actionList1_arp)
						.setOutPort(action1_arp.getPort())
						.build();
				
				ofSwitch.write(fm1_arp);
			
			}
						
			//O ultimo salto so tem destino pois a rota e calculada pelo MAC/DatapathId e nao por IP
			NodePortTuple lastNodePortTuple = p.getPath().get(p.getPath().size()-1);
			ofSwitch = switchService.getSwitch(lastNodePortTuple.getNodeId());
			
			if (ofSwitch != null) {  // is the switch connected
				if (log.isDebugEnabled()) {
					log.debug("Sending 1 new entries to {}", ofSwitch.getId().toString());
				}
				OFFactory factory2 = ofSwitch.getOFFactory();
				Match match2 = factory2.buildMatch()
						.setExact(MatchField.IN_PORT, lastNodePortTuple.getPortId())
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_SRC, appReq.getSrcIP())
						.setExact(MatchField.IPV4_DST, appReq.getDstIP())
						.build();
				
				OFActionOutput action2 = factory2.actions().buildOutput()
						.setPort(OFPort.of(Integer.valueOf(appReq.getDstPort().toString())))
						.build();
				
				List<OFAction> actionList2 = new ArrayList<OFAction>();
				actionList2.clear();
				actionList2.add(action2);

				OFFlowMod fm2 = factory2.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match2)
						.setActions(actionList2)
						.setOutPort(action2.getPort())
						.build();
				
				ofSwitch.write(fm2);
				
				//ARP
				OFFactory factory2_arp = ofSwitch.getOFFactory();
				Match match2_arp = factory2_arp.buildMatch()
						.setExact(MatchField.IN_PORT, lastNodePortTuple.getPortId())
						.setExact(MatchField.ETH_TYPE, EthType.ARP)
						.setExact(MatchField.IPV4_SRC, appReq.getSrcIP())
						.setExact(MatchField.IPV4_DST, appReq.getDstIP())
						.build();
				
				OFActionOutput action2_arp = factory2_arp.actions().buildOutput()
						.setPort(OFPort.of(Integer.valueOf(appReq.getDstPort().toString())))
						.build();
				
				List<OFAction> actionList2_arp = new ArrayList<OFAction>();
				actionList2_arp.clear();
				actionList2_arp.add(action2_arp);

				OFFlowMod fm2_arp = factory2_arp.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match2_arp)
						.setActions(actionList2_arp)
						.setOutPort(action2_arp.getPort())
						.build();
				
				ofSwitch.write(fm2_arp);
				
				
				//bidirecional
				OFFactory factory3 = ofSwitch.getOFFactory();
				Match match3 = factory3.buildMatch()
						.setExact(MatchField.IN_PORT, OFPort.of(Integer.valueOf(appReq.getDstPort().toString())))
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_SRC, appReq.getDstIP())
						.setExact(MatchField.IPV4_DST, appReq.getSrcIP())
						.build();
				
				OFActionOutput action3 = factory3.actions().buildOutput()
						.setPort(lastNodePortTuple.getPortId())
						.build();
				
				List<OFAction> actionList3 = new ArrayList<OFAction>();
				actionList3.clear();
				actionList3.add(action3);

				OFFlowMod fm3 = factory3.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match3)
						.setActions(actionList3)
						.setOutPort(action3.getPort())
						.build();
				
				ofSwitch.write(fm3);
				
				//ARP
				OFFactory factory3_arp = ofSwitch.getOFFactory();
				Match match3_arp = factory3_arp.buildMatch()
						.setExact(MatchField.IN_PORT, OFPort.of(Integer.valueOf(appReq.getDstPort().toString())))
						.setExact(MatchField.ETH_TYPE, EthType.ARP)
						.setExact(MatchField.IPV4_SRC, appReq.getDstIP())
						.setExact(MatchField.IPV4_DST, appReq.getSrcIP())
						.build();
				
				OFActionOutput action3_arp = factory3_arp.actions().buildOutput()
						.setPort(lastNodePortTuple.getPortId())
						.build();
				
				List<OFAction> actionList3_arp = new ArrayList<OFAction>();
				actionList3_arp.clear();
				actionList3_arp.add(action3_arp);

				OFFlowMod fm3_arp = factory3_arp.buildFlowAdd()
						.setIdleTimeout(idleTimeout)
						.setHardTimeout(hardTimeout)
						.setPriority(priority)
						.setMatch(match3_arp)
						.setActions(actionList3_arp)
						.setOutPort(action3_arp.getPort())
						.build();
				
				ofSwitch.write(fm3_arp);	
			}
			
			//aplica a rota/flow para os Switches internos. Os switches de borda foram tratados fora do loop pois so tem origem e destino
			OFPort inPort = null;
			int portTupleCount = 0;

			if (p.getPath().size() > 2)
			for (NodePortTuple npt : p.getPath().subList(1, p.getPath().size()-1)) {
				//log.info("Switch atual do loop {}", npt.getNodeId());
				//log.info("Porta do Router {}", npt.getPortId());
				
				portTupleCount++; // start value 1
				//PortTuple 'e dividido em duas partes por switch {DatapathID, InPort} e outra tupla {DatapathID, OutPort}
				if (portTupleCount % 2 != 0) { 
					//Armazena a porta de entrada
					ofSwitch = switchService.getSwitch(npt.getNodeId());

					if (ofSwitch != null) { // is the switch connected
						if (log.isDebugEnabled()) {
							log.debug("Sending 1 new entries to {}", ofSwitch.getId().toString());
						}
						inPort = npt.getPortId();
					}
				} else {
					//Com a porta de entrada do loop anterior, pega a porta de saida e escreve o flow para o switch
					OFFactory factory4 = ofSwitch.getOFFactory();
					Match match4 = factory4.buildMatch().setExact(MatchField.IN_PORT, inPort)
							.setExact(MatchField.ETH_TYPE, EthType.IPv4)
							.setExact(MatchField.IPV4_SRC, appReq.getSrcIP())
							.setExact(MatchField.IPV4_DST, appReq.getDstIP()).build();

					OFActionOutput action4 = factory4.actions().buildOutput()
							.setPort(npt.getPortId())
							.build();
					
					List<OFAction> actionList4 = new ArrayList<OFAction>();
					actionList4.clear();
					actionList4.add(action4);

					OFFlowMod fm4 = factory4.buildFlowAdd()
							.setIdleTimeout(idleTimeout)
							.setHardTimeout(hardTimeout)
							.setMatch(match4)
							.setActions(actionList4)
							.setOutPort(action4.getPort())
							.build();

					ofSwitch.write(fm4);
					
					//ARP
					OFFactory factory4_arp = ofSwitch.getOFFactory();
					Match match4_arp = factory4_arp.buildMatch().setExact(MatchField.IN_PORT, inPort)
							.setExact(MatchField.ETH_TYPE, EthType.ARP)
							.setExact(MatchField.IPV4_SRC, appReq.getSrcIP())
							.setExact(MatchField.IPV4_DST, appReq.getDstIP()).build();

					OFActionOutput action4_arp = factory4_arp.actions().buildOutput()
							.setPort(npt.getPortId())
							.build();
					
					List<OFAction> actionList4_arp = new ArrayList<OFAction>();
					actionList4_arp.clear();
					actionList4_arp.add(action4_arp);

					OFFlowMod fm4_arp = factory4_arp.buildFlowAdd()
							.setIdleTimeout(idleTimeout)
							.setHardTimeout(hardTimeout)
							.setMatch(match4_arp)
							.setActions(actionList4_arp)
							.setOutPort(action4_arp.getPort())
							.build();

					ofSwitch.write(fm4_arp);
					
					//bidirecional
					
					OFFactory factory5 = ofSwitch.getOFFactory();
					Match match5 = factory5.buildMatch().setExact(MatchField.IN_PORT, npt.getPortId())
							.setExact(MatchField.ETH_TYPE, EthType.IPv4)
							.setExact(MatchField.IPV4_SRC, appReq.getDstIP())
							.setExact(MatchField.IPV4_DST, appReq.getSrcIP()).build();

					OFActionOutput action5 = factory5.actions().buildOutput()
							.setPort(inPort)
							.build();

					List<OFAction> actionList5 = new ArrayList<OFAction>();
					actionList5.clear();
					actionList5.add(action5);

					OFFlowMod fm5 = factory5.buildFlowAdd()
							.setIdleTimeout(idleTimeout)
							.setHardTimeout(hardTimeout)
							.setMatch(match5)
							.setActions(actionList5)
							.setOutPort(action5.getPort())
							.build();

					ofSwitch.write(fm5);

					//ARP
					OFFactory factory5_arp = ofSwitch.getOFFactory();
					Match match5_arp = factory5_arp.buildMatch().setExact(MatchField.IN_PORT, npt.getPortId())
							.setExact(MatchField.ETH_TYPE, EthType.ARP)
							.setExact(MatchField.IPV4_SRC, appReq.getDstIP())
							.setExact(MatchField.IPV4_DST, appReq.getSrcIP()).build();

					OFActionOutput action5_arp = factory5_arp.actions().buildOutput()
							.setPort(inPort)
							.build();

					List<OFAction> actionList5_arp = new ArrayList<OFAction>();
					actionList5_arp.clear();
					actionList5_arp.add(action5_arp);

					OFFlowMod fm5_arp = factory5_arp.buildFlowAdd()
							.setIdleTimeout(idleTimeout)
							.setHardTimeout(hardTimeout)
							.setMatch(match5_arp)
							.setActions(actionList5_arp)
							.setOutPort(action5_arp.getPort())
							.build();

					ofSwitch.write(fm5_arp);
				
				}
				
				

			}

					
			log.info("Rota Anterior {} ", routingService.getPath(appReq.getSrcId(), appReq.getDstId()));
			log.info("Nova Rota {} ", p);
			return true;
					
			//IOFSwitch mySwitch = sw 
			
			// TODO Auto-generated method stub
			
		}
		
		private void setLowerBandwidthPath(AppReq appReq) {
			log.info("Largura de banda anterior {}", routingService.getPath(appReq.getSrcId(), appReq.getDstId()));
			//log.info("Mudando da metrica {} para a metrica {}", routingService.getPathMetric(), PATH_METRIC.LATENCY);
			Path p = routingService.getPath(appReq.getSrcId(), appReq.getDstId(), PATH_METRIC.LINK_SPEED);
			if(p.getLatency() != null)
				log.info("Latencia atual {}", p.getLatency().getValue());
			log.info("Rota Anterior {} ", routingService.getPath(appReq.getSrcId(), appReq.getDstId()));
			log.info("Nova Rota {} ", routingService.getPath(appReq.getSrcId(), appReq.getDstId(), PATH_METRIC.LINK_SPEED));
			//log.info("Rota rapidas {}",  routingService.getPath(appReq.getSrcId(), appReq.getDstId()));
			
		}
		
	}
	
	public void appReqAddListener(String name){
		//ThreadPool tp = new ThreadPool();
	    //context.addService(IThreadPoolService.class, tp);
	    //tp.init(context);
        //tp.startUp(context);
	    //context.getServiceImpl(AppReqPusher.class).getAllAppReq();
	    
	    if (appReqService.getAppReq(name) != null) {
	    	AppReq appReq = appReqService.getAppReq(name);
	    	//log.info("AppReqMap {}", appReq.toString());
	    	//Execute Delay Monitor at each X sec from timeout in ReqTable

	    	scheduledFutureMap.put(name, threadPool.scheduleAtFixedRate(new DelayMonitor(appReq), 0, appReq.getTimeout(), TimeUnit.SECONDS));
	    	
	    }
	}
	
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
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
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
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		linkDiscoveryService = context.getServiceImpl(ILinkDiscoveryService.class);
		topologyService = context.getServiceImpl(ITopologyService.class);
		routingService = context.getServiceImpl(IRoutingService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		macAddresses = new ConcurrentSkipListSet<Long>();
		logger = LoggerFactory.getLogger(AutoManIoT.class);
		//reqTable = new HashSet<AppReq>();
		appReqMap = new HashMap<String, AppReq>();
		storageSourceService = context.getServiceImpl(IStorageSourceService.class);
    	threadPool = new ScheduledThreadPoolExecutor(1);
    	scheduledFutureMap = new HashMap<String, ScheduledFuture<?>>();
    	
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
	    floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	    storageSourceService.addListener(AppReqPusher.TABLE_NAME, this);
		switchService.addOFSwitchListener(this);

	    //linkDiscoveryService.

	    log.info("Starting AutoManIoT...");
	    
	    //Registering a AppReq test
	    IPv4 ipv4 = new IPv4();
	    ipv4.setSourceAddress("10.0.0.1");
	    ipv4.setDestinationAddress("10.0.0.6");
	    TCP tcp = new TCP();
	    tcp.setSourcePort(1);
	    tcp.setDestinationPort(1);
	    AppReq ar = new AppReq("test", ipv4.getSourceAddress(), ipv4.getDestinationAddress(), DatapathId.of(1L), DatapathId.of(2L), tcp.getSourcePort(), tcp.getDestinationPort(), 1, 5, 10);
	    appReqMap.put(ar.getName(), ar);
		appReqService = context.getServiceImpl(IAppReqPusherService.class);
		appReqService.addAppReq(AppReqPusher.TABLE_NAME, ar);
	    
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		
		OFPacketIn packetIn = (OFPacketIn) msg;
		log.info("Mensagem Recebida {}", msg);
		log.info("Porta de origem {}", packetIn.getInPort());
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		log.info("Eth src {}, dst {}", eth.getSourceMACAddress(), eth.getDestinationMACAddress());
		log.info("Eth type {}", eth.getEtherType());
		
		IOFSwitch ofSwitch = switchService.getSwitch(sw.getId());
		
		/*
		if(eth.getEtherType() == EthType.IPv4){
			IPv4 ipv4 = (IPv4) eth.getPayload();
			if (ipv4.getProtocol() == IpProtocol.TCP){
				TCP tcp = (TCP) ipv4.getPayload();
				log.info("TCP src port {}, dst {}",tcp.getSourcePort(), tcp.getDestinationPort());
			}
		} else if (eth.getEtherType() == EthType.ARP){
			ARP arp = (ARP) eth.getPayload();
			log.info("ARP protocol type {}",arp.getProtocolType());
			
		}
		*/
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
		//log.info("O listener escutou a remocao de um novo registro em AppReqTable {}, {}", tableName, rowKeys.toString());
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

	
	/*public Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		
		return Command.CONTINUE;
	}*/

}
