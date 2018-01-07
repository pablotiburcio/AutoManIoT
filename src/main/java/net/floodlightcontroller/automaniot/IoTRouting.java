package net.floodlightcontroller.automaniot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.floodlightcontroller.automaniot.mqtt.AbstractMessage;
import net.floodlightcontroller.automaniot.mqtt.MQTTDecoder;
import net.floodlightcontroller.automaniot.mqtt.MqttUtils;
import net.floodlightcontroller.automaniot.mqtt.PingReqMessage;
import net.floodlightcontroller.automaniot.mqtt.PingRespMessage;
import net.floodlightcontroller.automaniot.mqtt.PublishMessage;
import net.floodlightcontroller.automaniot.mqtt.SubscribeMessage;
import net.floodlightcontroller.automaniot.mqtt.UnsubAckMessage;
import net.floodlightcontroller.automaniot.mqtt.UnsubscribeMessage;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.internal.ISwitchDriverRegistry;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.internal.Entity;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.IPv6;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.IRoutingService.PATH_METRIC;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.routing.PathId;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.FlowModUtils;
import net.floodlightcontroller.util.MatchUtils;
import net.floodlightcontroller.util.OFDPAUtils;
import net.floodlightcontroller.util.OFMessageDamper;
import net.floodlightcontroller.util.OFMessageUtils;


public class IoTRouting implements IOFIoTRouting, IFloodlightModule, IOFMessageListener, IOFSwitchListener, ILinkDiscoveryListener  {
	protected static Logger log = LoggerFactory.getLogger(IoTRouting.class);

	public static final String MODULE_NAME = "iotrouting";
	
    protected IFloodlightProviderService floodlightProviderService;
    protected IOFSwitchService switchService;
    protected IDeviceService deviceManagerService;
    protected IRoutingService routingEngineService;
    protected ITopologyService topologyService;
    protected IDebugCounterService debugCounterService;
    protected ILinkDiscoveryService linkService;
	protected ITopicReqPusherService topicReqService;
	protected IAppReqPusherService appReqService;
	

    
    // flow-mod - for use in the cookie
    public static final int FORWARDING_APP_ID = 2;
    static {
        AppCookie.registerApp(FORWARDING_APP_ID, "forwarding");
    }
    protected static final U64 DEFAULT_FORWARDING_COOKIE = AppCookie.makeCookie(FORWARDING_APP_ID, 0);
	
    public static int FLOWMOD_DEFAULT_IDLE_TIMEOUT = 0; // in seconds
    public static int FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
    public static int FLOWMOD_DEFAULT_PRIORITY = 1; // 0 is the default table-miss flow in OF1.3+, so we need to use 1

    protected static TableId FLOWMOD_DEFAULT_TABLE_ID = TableId.ZERO;

    protected static boolean FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG = false;

    protected static boolean FLOWMOD_DEFAULT_MATCH_IN_PORT = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_VLAN = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_MAC = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_IP = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;
    
    protected static boolean FLOWMOD_DEFAULT_MATCH_MAC_SRC = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_MAC_DST = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_IP_SRC = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_IP_DST = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_TCP_FLAG = true;

    protected static boolean FLOOD_ALL_ARP_PACKETS = false;

    protected static boolean REMOVE_FLOWS_ON_LINK_OR_PORT_DOWN = true;
	
    private static final short DECISION_BITS = 24;
    private static final short DECISION_SHIFT = 0;
    private static final long DECISION_MASK = ((1L << DECISION_BITS) - 1) << DECISION_SHIFT;

    private static final short FLOWSET_BITS = 28;
    protected static final short FLOWSET_SHIFT = DECISION_BITS;
    private static final long FLOWSET_MASK = ((1L << FLOWSET_BITS) - 1) << FLOWSET_SHIFT;
    private static final long FLOWSET_MAX = (long) (Math.pow(2, FLOWSET_BITS) - 1);
    protected static FlowSetIdRegistry flowSetIdRegistry;
    
    private AbstractMessage mqttMessageType=null;
    
    //store the last calculated Path to app
    private Map<String, Path> lastRoute;

    protected static class FlowSetIdRegistry {
        private volatile Map<NodePortTuple, Set<U64>> nptToFlowSetIds;
        private volatile Map<U64, Set<NodePortTuple>> flowSetIdToNpts;
        
        private volatile long flowSetGenerator = -1;

        private static volatile FlowSetIdRegistry instance;

        private FlowSetIdRegistry() {
            nptToFlowSetIds = new ConcurrentHashMap<NodePortTuple, Set<U64>>();
            flowSetIdToNpts = new ConcurrentHashMap<U64, Set<NodePortTuple>>();
        }

        protected static FlowSetIdRegistry getInstance() {
            if (instance == null) {
                instance = new FlowSetIdRegistry();
            }
            return instance;
        }
        
        protected synchronized U64 generateFlowSetId() {
            flowSetGenerator += 1;
            if (flowSetGenerator == FLOWSET_MAX) {
                flowSetGenerator = 0;
                log.warn("Flowset IDs have exceeded capacity of {}. Flowset ID generator resetting back to 0", FLOWSET_MAX);
            }
            U64 id = U64.of(flowSetGenerator << FLOWSET_SHIFT);
            log.debug("Generating flowset ID {}, shifted {}", flowSetGenerator, id);
            return id;
        }
    }
    
    protected OFMessageDamper messageDamper;
    private static int OFMESSAGE_DAMPER_CAPACITY = 10000;
    private static int OFMESSAGE_DAMPER_TIMEOUT = 250; // ms
    
    protected void init() {
        messageDamper = new OFMessageDamper(OFMESSAGE_DAMPER_CAPACITY,
                EnumSet.of(OFType.FLOW_MOD),
                OFMESSAGE_DAMPER_TIMEOUT);
        
        lastRoute = new HashMap<String, Path>();
    }
	
	//TODO: Definir uma % de variacao para nao mudar a rota a todo instante 
	//Aplica a rota de menor latencia para a app 
    //Deprecated: Implementado no inicio. Recodificado pra aproveitar o codigo Forwarding+ForwardingBase do floodlight 
    @Override
    public boolean setLowerLatencyPath(Path oldPath, IOFSwitchService switchService, AppReq appReq) {
		IOFSwitch ofSwitch;

		int idleTimeout = 0;
		int hardTimeout = 0;
		int priority = 1;
		
		Path p = routingEngineService.getPath(appReq.getSrcId(), appReq.getDstId(), PATH_METRIC.LATENCY);
		log.info("Path Atual {}", p.toString());
		
		if(p.getLatency() != null){
			log.info("Nova latencia {}", p.getLatency().getValue());
		} else {
			return false;
		}
		
		
		//O 1o salto so tem destino, pois a rota e calculada pelo MAC/DatapathId e nao por IP
		NodePortTuple firstNodePortTuple = p.getPath().get(0); 
			ofSwitch = switchService.getSwitch(firstNodePortTuple.getNodeId());					
		
		if (ofSwitch != null) {  // if switch is connected
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
		
		//aplica a rota/flow para os Switches internos. Os switches de borda foram tratados fora do loop pois so tem origem nem destino
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

		//log.info("Rota Anterior {} ", routingService.getPath(appReq.getSrcId(), appReq.getDstId()));
		log.info("Nova Rota {} ", p);
		return true;
				
		//IOFSwitch mySwitch = sw 
		
		// TODO Auto-generated method stub
		
	}

	//Modified from Forwarding and ForwardingBase
	public net.floodlightcontroller.core.IListener.Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi
			, FloodlightContext cntx) {

		doForwardFlow(sw, pi, cntx, false);
        return Command.CONTINUE;


	}
    
	protected void doForwardFlow(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
		OFPort srcPort = OFMessageUtils.getInPort(pi);
		DatapathId srcSw = sw.getId();
		IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
		IDevice srcDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);

		if (dstDevice == null) {
			log.debug("Destination device unknown. Flooding packet");
			return;
		}

		if (srcDevice == null) {
			log.error("No device entry found for source device. Is the device manager running? If so, report bug.");
			return;
		}

		/* Some physical switches partially support or do not support ARP flows */
		if (FLOOD_ALL_ARP_PACKETS && 
				IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD).getEtherType() 
				== EthType.ARP) {
			log.debug("ARP flows disabled in Forwarding.");
			return;
		}

		/* This packet-in is from a switch in the path before its flow was installed along the path */
		if (!topologyService.isEdge(srcSw, srcPort)) {  
			log.debug("Packet destination is known, but packet was not received on an edge port (rx on {}/{}).", srcSw, srcPort);
			return; 
		}   

		/* 
		 * Search for the true attachment point. The true AP is
		 * not an endpoint of a link. It is a switch port w/o an
		 * associated link. Note this does not necessarily hold
		 * true for devices that 'live' between OpenFlow islands.
		 * 
		 * TODO Account for the case where a device is actually
		 * attached between islands (possibly on a non-OF switch
		 * in between two OpenFlow switches).
		 */
		SwitchPort dstAp = null;
		for (SwitchPort ap : dstDevice.getAttachmentPoints()) {
			if (topologyService.isEdge(ap.getNodeId(), ap.getPortId())) {
				dstAp = ap;
				break;
			}
		}	

		/* 
		 * This should only happen (perhaps) when the controller is
		 * actively learning a new topology and hasn't discovered
		 * all links yet, or a switch was in standalone mode and the
		 * packet in question was captured in flight on the dst point
		 * of a link.
		 */
		if (dstAp == null) {
			log.debug("Could not locate edge attachment point for destination device {}.");
			return; 
		}

		/* Validate that the source and destination are not on the same switch port */
		if (sw.getId().equals(dstAp.getNodeId()) && srcPort.equals(dstAp.getPortId())) {
			log.info("Both source and destination are on the same switch/port {}/{}. Dropping packet", sw.toString(), srcPort);
			return;
		}			

		U64 flowSetId = flowSetIdRegistry.generateFlowSetId();
		U64 cookie = makeForwardingCookie(flowSetId);
		Path path = routingEngineService.getPath(srcSw, 
				srcPort,
				dstAp.getNodeId(),
				dstAp.getPortId());

		Match m = createMatchFromPacket(sw, srcPort, pi, cntx);

		if (! path.getPath().isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("pushRoute inPort={} route={} " +
						"destination={}:{}",
						new Object[] { srcPort, path,
								dstAp.getNodeId(),
								dstAp.getPortId()});
				log.debug("Creating flow rules on the route, match rule: {}", m);
			}

			pushRoute(path, m, sw.getId(), cookie,
					requestFlowRemovedNotifn,
					OFFlowModCommand.ADD, FLOWMOD_DEFAULT_HARD_TIMEOUT, FLOWMOD_DEFAULT_IDLE_TIMEOUT);	
			/*pushRoute(path, m, pi, sw.getId(), cookie, 
					cntx, requestFlowRemovedNotifn,
					OFFlowModCommand.ADD);*/

			/* 
			 * Register this flowset with ingress and egress ports for link down
			 * flow removal. This is done after we push the path as it is blocking.
			 */
			//for (NodePortTuple npt : path.getPath()) {
			//    flowSetIdRegistry.registerFlowSetId(npt, flowSetId);
			//}
		} /* else no path was found */
	}
        
        /**
         * Push routes from back to front
         * @param route Route to push
         * @param match OpenFlow fields to match on
         * @param srcSwPort Source switch port for the first hop
         * @param dstSwPort Destination switch port for final hop
         * @param cookie The cookie to set in each flow_mod
         * @param cntx The floodlight context
         * @param requestFlowRemovedNotification if set to true then the switch would
         *        send a flow mod removal notification when the flow mod expires
         * @param flowModCommand flow mod. command to use, e.g. OFFlowMod.OFPFC_ADD,
         *        OFFlowMod.OFPFC_MODIFY etc.
         * @return true if a packet out was sent on the first-hop switch of this route
         */
	@Override
        public boolean pushRoute(Path route, Match match,
                DatapathId pinSwitch, U64 cookie,
                boolean requestFlowRemovedNotification, OFFlowModCommand flowModCommand, int hardTimeOut, int idleTimeOut) {

            boolean packetOutSent = false;
            
            //log.info("Pushing new route to swithes-----match={}----------"+ match.toString());

            List<NodePortTuple> switchPortList = route.getPath();

            for (int indx = switchPortList.size() - 1; indx > 0; indx -= 2) {
                // indx and indx-1 will always have the same switch DPID.
                DatapathId switchDPID = switchPortList.get(indx).getNodeId();
                IOFSwitch sw = switchService.getSwitch(switchDPID);

                if (sw == null) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to push route, switch at DPID {} " + "not available", switchDPID);
                    }
                    return packetOutSent;
                }

                // need to build flow mod based on what type it is. Cannot set command later
                OFFlowMod.Builder fmb;
                switch (flowModCommand) {
                case ADD:
                    fmb = sw.getOFFactory().buildFlowAdd();
                    break;
                case DELETE:
                    fmb = sw.getOFFactory().buildFlowDelete();
                    break;
                case DELETE_STRICT:
                    fmb = sw.getOFFactory().buildFlowDeleteStrict();
                    break;
                case MODIFY:
                    fmb = sw.getOFFactory().buildFlowModify();
                    break;
                default:
                    log.error("Could not decode OFFlowModCommand. Using MODIFY_STRICT. (Should another be used as the default?)");        
                case MODIFY_STRICT:
                    fmb = sw.getOFFactory().buildFlowModifyStrict();
                    break;			
                }

                OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
                List<OFAction> actions = new ArrayList<OFAction>();	
                Match.Builder mb = MatchUtils.convertToVersion(match, sw.getOFFactory().getVersion());

                // set input and output ports on the switch
                OFPort outPort = switchPortList.get(indx).getPortId();
                OFPort inPort = switchPortList.get(indx - 1).getPortId();
                if (FLOWMOD_DEFAULT_MATCH_IN_PORT) {
                    mb.setExact(MatchField.IN_PORT, inPort);
                }
                aob.setPort(outPort);
                aob.setMaxLen(Integer.MAX_VALUE);
                actions.add(aob.build());

                if (FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG || requestFlowRemovedNotification) {
                    Set<OFFlowModFlags> flags = new HashSet<>();
                    flags.add(OFFlowModFlags.SEND_FLOW_REM);
                    fmb.setFlags(flags);
                }

                fmb.setMatch(mb.build())
                .setIdleTimeout(idleTimeOut)
                .setHardTimeout(hardTimeOut)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setCookie(cookie)
                .setOutPort(outPort)
                .setPriority(FLOWMOD_DEFAULT_PRIORITY);

                FlowModUtils.setActions(fmb, actions, sw);

                /* Configure for particular switch pipeline */
                if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) != 0) {
                    fmb.setTableId(FLOWMOD_DEFAULT_TABLE_ID);
                }
                            
                if (log.isTraceEnabled()) {
                    log.trace("Pushing Route flowmod routeIndx={} " +
                            "sw={} inPort={} outPort={}",
                            new Object[] {indx,
                                    sw,
                                    fmb.getMatch().get(MatchField.IN_PORT),
                                    outPort });
                }

                if (OFDPAUtils.isOFDPASwitch(sw)) {
                    OFDPAUtils.addLearningSwitchFlow(sw, cookie, 
                            FLOWMOD_DEFAULT_PRIORITY, 
                            FLOWMOD_DEFAULT_HARD_TIMEOUT,
                            FLOWMOD_DEFAULT_IDLE_TIMEOUT,
                            fmb.getMatch(), 
                            null, // TODO how to determine output VLAN for lookup of L2 interface group
                            outPort);
                } else {
                    //log.info("wrote to switch {} message {} to port {}", sw.getId(), fmb.getMatch().toString());
                    //log.info("outport {}, action{}", fmb.getOutPort().toString(), fmb.getActions().toString());
                    messageDamper.write(sw, fmb.build());
                    
                    packetOutSent = true;
                }

                /* Push the packet out the first hop switch */
//                if (sw.getId().equals(pinSwitch) &&
//                        !fmb.getCommand().equals(OFFlowModCommand.DELETE) &&
//                        !fmb.getCommand().equals(OFFlowModCommand.DELETE_STRICT)) {
//                    /* Use the buffered packet at the switch, if there's one stored */
//                    if (pushPacket){
//                		pushPacket(sw, pi, outPort, true, cntx); //in continuous adaptation rate there are no packets to write
//                    	packetOutSent = true;
//                    }
//                }
            }

            return packetOutSent;
        }
        

        /**
         * Pushes a packet-out to a switch. The assumption here is that
         * the packet-in was also generated from the same switch. Thus, if the input
         * port of the packet-in and the outport are the same, the function will not
         * push the packet-out.
         * @param sw switch that generated the packet-in, and from which packet-out is sent
         * @param pi packet-in
         * @param outport output port
         * @param useBufferedPacket use the packet buffered at the switch, if possible
         * @param cntx context of the packet
         */
        protected void pushPacket(IOFSwitch sw, OFPacketIn pi, OFPort outport, boolean useBufferedPacket, FloodlightContext cntx) {
            if (pi == null) {
                return;
            }

            // The assumption here is (sw) is the switch that generated the
            // packet-in. If the input port is the same as output port, then
            // the packet-out should be ignored.
            if ((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)).equals(outport)) {
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to do packet-out to the same " +
                            "interface as packet-in. Dropping packet. " +
                            " SrcSwitch={}, pi={}",
                            new Object[]{sw, pi});
                    return;
                }
            }

            if (log.isTraceEnabled()) {
                log.trace("PacketOut srcSwitch={} pi={}",
                        new Object[] {sw, pi});
            }

            OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(sw.getOFFactory().actions().output(outport, Integer.MAX_VALUE));
            pob.setActions(actions);

            /* Use packet in buffer if there is a buffer ID set */
            if (useBufferedPacket) {
                pob.setBufferId(pi.getBufferId()); /* will be NO_BUFFER if there isn't one */
            } else {
                pob.setBufferId(OFBufferId.NO_BUFFER);
            }

            if (pob.getBufferId().equals(OFBufferId.NO_BUFFER)) {
                byte[] packetData = pi.getData();
                pob.setData(packetData);
            }

            pob.setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));

            messageDamper.write(sw, pob.build());
            
        }
        @Override
        public Match createMatch(IOFSwitch sw, OFPort inPort, AppReq appReq, String protocol) {
        	
        	//log.info("creating match.......................................................");
        	
        	Match.Builder mb = sw.getOFFactory().buildMatch();
        	mb.setExact(MatchField.IN_PORT, inPort);
        	
        	mb.setExact(MatchField.IPV4_SRC, appReq.getSrcIP());
        	mb.setExact(MatchField.IPV4_DST, appReq.getDstIP());
        	
        	
        	if (protocol == "ip"){ //TODO: put it in an organized way
            	
        		mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
        		mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
        		
        		if(!appReq.getSrcPort().equals(TransportPort.of(0)))
        			mb.setExact(MatchField.TCP_SRC, appReq.getSrcTransPort());
        		
        		if(!appReq.getDstPort().equals(TransportPort.of(0)))
        			mb.setExact(MatchField.TCP_DST, appReq.getDstTransPort());
        		
        	} else if (protocol == "arp"){
        		mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
        		
        	}
        	return mb.build();
        
        	
        }
        
        @Override
        public Match createReverseMatch(IOFSwitch sw, OFPort inPort, AppReq appReq, String protocol) {
        	
        	//log.info("creating match.......................................................");
        	
        	Match.Builder mb = sw.getOFFactory().buildMatch();
        	mb.setExact(MatchField.IN_PORT, inPort);
        	
        	mb.setExact(MatchField.IPV4_DST, appReq.getSrcIP());
        	
        	mb.setExact(MatchField.IPV4_SRC, appReq.getDstIP());
        	
        	
        	if (protocol == "ip"){ //TODO: put it in an organized way
            	
        		mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
        		mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
        		
        		if(!appReq.getSrcPort().equals(TransportPort.of(0)))
        			mb.setExact(MatchField.TCP_DST, appReq.getSrcTransPort());
        		
        		if(!appReq.getDstPort().equals(TransportPort.of(0)))
        			mb.setExact(MatchField.TCP_SRC, appReq.getDstTransPort());
        		
        	} else if (protocol == "arp"){
        		mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
        		
        	}
        	return mb.build();
        
        	
        }
        
        /**
         * Instead of using the Firewall's routing decision Match, which might be as general
         * as "in_port" and inadvertently Match packets erroneously, construct a more
         * specific Match based on the deserialized OFPacketIn's payload, which has been 
         * placed in the FloodlightContext already by the Controller.
         * 
         * @param sw, the switch on which the packet was received
         * @param inPort, the ingress switch port on which the packet was received
         * @param cntx, the current context which contains the deserialized packet
         * @return a composed Match object based on the provided information
         */
        @Override
        public Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, OFPacketIn pi, FloodlightContext cntx) {
            // The packet in match will only contain the port number.
            // We need to add in specifics for the hosts we're routing between.
            Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

            VlanVid vlan = null;      
            if (pi.getVersion().compareTo(OFVersion.OF_11) > 0 && /* 1.0 and 1.1 do not have a match */
                    pi.getMatch().get(MatchField.VLAN_VID) != null) { 
                vlan = pi.getMatch().get(MatchField.VLAN_VID).getVlanVid(); /* VLAN may have been popped by switch */
            }
            if (vlan == null) {
                vlan = VlanVid.ofVlan(eth.getVlanID()); /* VLAN might still be in packet */
            }
            
            MacAddress srcMac = eth.getSourceMACAddress();
            MacAddress dstMac = eth.getDestinationMACAddress();

            Match.Builder mb = sw.getOFFactory().buildMatch();
            if (FLOWMOD_DEFAULT_MATCH_IN_PORT) {
                mb.setExact(MatchField.IN_PORT, inPort);
            }

            if (FLOWMOD_DEFAULT_MATCH_MAC) {
                if (FLOWMOD_DEFAULT_MATCH_MAC_SRC) {
                    mb.setExact(MatchField.ETH_SRC, srcMac);
                }
                if (FLOWMOD_DEFAULT_MATCH_MAC_DST) {
                    mb.setExact(MatchField.ETH_DST, dstMac);
                }
            }

            if (FLOWMOD_DEFAULT_MATCH_VLAN) {
                if (!vlan.equals(VlanVid.ZERO)) {
                    mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
                }
            }

            // TODO Detect switch type and match to create hardware-implemented flow
            if (eth.getEtherType() == EthType.IPv4) { /* shallow check for equality is okay for EthType */
                IPv4 ip = (IPv4) eth.getPayload();
                IPv4Address srcIp = ip.getSourceAddress();
                IPv4Address dstIp = ip.getDestinationAddress();

                if (FLOWMOD_DEFAULT_MATCH_IP) {
                    mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
                    if (FLOWMOD_DEFAULT_MATCH_IP_SRC) {
                        mb.setExact(MatchField.IPV4_SRC, srcIp);
                    }
                    if (FLOWMOD_DEFAULT_MATCH_IP_DST) {
                        mb.setExact(MatchField.IPV4_DST, dstIp);
                    }
                }

                if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
                    /*
                     * Take care of the ethertype if not included earlier,
                     * since it's a prerequisite for transport ports.
                     */
                    if (!FLOWMOD_DEFAULT_MATCH_IP) {
                        mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
                    }

                    if (ip.getProtocol().equals(IpProtocol.TCP)) {
                        TCP tcp = (TCP) ip.getPayload();
                        mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                            mb.setExact(MatchField.TCP_SRC, tcp.getSourcePort());
                        }
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                            mb.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
                        }
                        if(
                        sw.getSwitchDescription().getHardwareDescription().toLowerCase().contains("open vswitch") && (
                        Integer.parseInt(sw.getSwitchDescription().getSoftwareDescription().toLowerCase().split("\\.")[0]) > 2  || (
                        Integer.parseInt(sw.getSwitchDescription().getSoftwareDescription().toLowerCase().split("\\.")[0]) == 2 &&
                        Integer.parseInt(sw.getSwitchDescription().getSoftwareDescription().toLowerCase().split("\\.")[1]) >= 1 ))
                        ){
    	                    if(FLOWMOD_DEFAULT_MATCH_TCP_FLAG){
    	                        mb.setExact(MatchField.OVS_TCP_FLAGS, U16.of(tcp.getFlags()));
    	                    }
                        }
                    } else if (ip.getProtocol().equals(IpProtocol.UDP)) {
                        UDP udp = (UDP) ip.getPayload();
                        mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                            mb.setExact(MatchField.UDP_SRC, udp.getSourcePort());
                        }
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                            mb.setExact(MatchField.UDP_DST, udp.getDestinationPort());
                        }
                    }
                }
            } else if (eth.getEtherType() == EthType.ARP) { /* shallow check for equality is okay for EthType */
                mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
            } else if (eth.getEtherType() == EthType.IPv6) {
                IPv6 ip = (IPv6) eth.getPayload();
                IPv6Address srcIp = ip.getSourceAddress();
                IPv6Address dstIp = ip.getDestinationAddress();

                if (FLOWMOD_DEFAULT_MATCH_IP) {
                    mb.setExact(MatchField.ETH_TYPE, EthType.IPv6);
                    if (FLOWMOD_DEFAULT_MATCH_IP_SRC) {
                        mb.setExact(MatchField.IPV6_SRC, srcIp);
                    }
                    if (FLOWMOD_DEFAULT_MATCH_IP_DST) {
                        mb.setExact(MatchField.IPV6_DST, dstIp);
                    }
                }

                if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
                    /*
                     * Take care of the ethertype if not included earlier,
                     * since it's a prerequisite for transport ports.
                     */
                    if (!FLOWMOD_DEFAULT_MATCH_IP) {
                        mb.setExact(MatchField.ETH_TYPE, EthType.IPv6);
                    }

                    if (ip.getNextHeader().equals(IpProtocol.TCP)) {
                        TCP tcp = (TCP) ip.getPayload();
                        mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                            mb.setExact(MatchField.TCP_SRC, tcp.getSourcePort());
                        }
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                            mb.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
                        }
                        if(
                        sw.getSwitchDescription().getHardwareDescription().toLowerCase().contains("open vswitch") && (
                        Integer.parseInt(sw.getSwitchDescription().getSoftwareDescription().toLowerCase().split("\\.")[0]) > 2  || (
                        Integer.parseInt(sw.getSwitchDescription().getSoftwareDescription().toLowerCase().split("\\.")[0]) == 2 &&
                        Integer.parseInt(sw.getSwitchDescription().getSoftwareDescription().toLowerCase().split("\\.")[1]) >= 1 ))
                        ){
    	                    if(FLOWMOD_DEFAULT_MATCH_TCP_FLAG){
    	                        mb.setExact(MatchField.OVS_TCP_FLAGS, U16.of(tcp.getFlags()));
    	                    }
                        }
                    } else if (ip.getNextHeader().equals(IpProtocol.UDP)) {
                        UDP udp = (UDP) ip.getPayload();
                        mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                            mb.setExact(MatchField.UDP_SRC, udp.getSourcePort());
                        }
                        if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                            mb.setExact(MatchField.UDP_DST, udp.getDestinationPort());
                        }
                    }
                }
            }
            return mb.build();
        }
        
        /**
         * Builds a cookie that includes routing decision information.
         *
         * @param decision The routing decision providing a descriptor, or null
         * @return A cookie with our app id and the required fields masked-in
         */
        protected U64 makeForwardingCookie(U64 flowSetId) {
            long user_fields = 0;

            if (flowSetId != null) {
                user_fields |= AppCookie.extractUser(flowSetId) & FLOWSET_MASK;
            }

            // TODO: Mask in any other required fields here

            if (user_fields == 0) {
                return DEFAULT_FORWARDING_COOKIE;
            }
            return AppCookie.makeCookie(FORWARDING_APP_ID, user_fields);
        }
        
        // IFloodlightModule methods

        @Override
        public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        	Collection<Class<? extends IFloodlightService>> l =
    				new ArrayList<Class<? extends IFloodlightService>>();
    		l.add(IOFIoTRouting.class);
    		return l;
        }

        @Override
        public Map<Class<? extends IFloodlightService>, IFloodlightService>
        getServiceImpls() {
    		Map<Class<? extends IFloodlightService>,
    		IFloodlightService> m =
    		new HashMap<Class<? extends IFloodlightService>,
    		IFloodlightService>();
    		m.put(IOFIoTRouting.class, this);
    		return m;
        }

        @Override
        public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
            Collection<Class<? extends IFloodlightService>> l =
                    new ArrayList<Class<? extends IFloodlightService>>();
            l.add(IFloodlightProviderService.class);
            l.add(IDeviceService.class);
            l.add(IRoutingService.class);
            l.add(ITopologyService.class);
            l.add(IDebugCounterService.class);
            l.add(IOFSwitchService.class);
            l.add(ILinkDiscoveryService.class);
            return l;
        }

        @Override
        public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        	this.init();
            this.floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
            this.deviceManagerService = context.getServiceImpl(IDeviceService.class);
            this.routingEngineService = context.getServiceImpl(IRoutingService.class);
            this.topologyService = context.getServiceImpl(ITopologyService.class);
            this.debugCounterService = context.getServiceImpl(IDebugCounterService.class);
            this.switchService = context.getServiceImpl(IOFSwitchService.class);
            this.linkService = context.getServiceImpl(ILinkDiscoveryService.class);
    		this.topicReqService = context.getServiceImpl(ITopicReqPusherService.class);
    		this.appReqService = context.getServiceImpl(IAppReqPusherService.class);



            flowSetIdRegistry = FlowSetIdRegistry.getInstance();
        }

		@Override
		public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
			switchService.addOFSwitchListener(this);
			
		    log.info("Starting IoTRouting...");

			
			/* Register only if we want to remove stale flows */
			if (REMOVE_FLOWS_ON_LINK_OR_PORT_DOWN) {
				linkService.addListener(this);
			}
			floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);

		}
	
		
		
		@Override
		public boolean pushRoute(Path path, AppReq appReq){
			
			//Hard SDN time out factor calculus
			int timeOutFactor = 2;
			
			//to use in bidirectional way
			Path reversePath = new Path(new PathId(appReq.getDstId(), appReq.getSrcId()), path.getReversePath());
			
			//log.info("Path para escrever {}", path.toString());
			//log.info("reversePath para escrever {}", reversePath.toString());
		
			IOFSwitch firstSwitch = switchService.getSwitch(appReq.getSrcId());
			IOFSwitch lastSwitch = switchService.getSwitch(appReq.getDstId());
			
			//TODO: Organize it to work inside IoTRouting and receiving ARP/IP/Bidirecional as parameter
			Match matchIP = createMatch(firstSwitch, appReq.getSrcPort(), appReq, "ip");
			Match reverseMatchIP = createReverseMatch(lastSwitch, appReq.getDstPort(), appReq, "ip");
			Match matchARP = createMatch(firstSwitch, appReq.getSrcPort(), appReq, "arp");
			Match reverseMatchArp = createReverseMatch(lastSwitch, appReq.getDstPort(), appReq, "arp");
			OFFlowModCommand flowModCommand = OFFlowModCommand.ADD;
			
			//push route but don't send packet
			boolean toReturn;
			toReturn = pushRoute(path, matchIP, appReq.getSrcId(), U64.of(0L), false, flowModCommand, 0, appReq.getTimeout()*timeOutFactor);
			toReturn = toReturn && pushRoute(reversePath, reverseMatchIP ,appReq.getDstId(), U64.of(0L), false, flowModCommand, 0, appReq.getTimeout()*timeOutFactor);
			toReturn = toReturn && pushRoute(path, matchARP, appReq.getSrcId(), U64.of(0L), false, flowModCommand, 0, appReq.getTimeout()*timeOutFactor);
			toReturn = toReturn && pushRoute(reversePath, reverseMatchArp, appReq.getDstId(), U64.of(0L), false, flowModCommand, 0, appReq.getTimeout()*timeOutFactor);
			
			return toReturn;
		}
		
		private ArrayList<SwitchPort> getSrcDstSwitchByIP(IPv4Address srcIP, IPv4Address dstIP){
			SwitchPort[] switches;
			SwitchPort srcSwitch = null;
			SwitchPort dstSwitch = null;
			Iterator<? extends IDevice> devIter = deviceManagerService.queryDevices(MacAddress.NONE, null, srcIP, IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);

			if(devIter.hasNext()){
				switches = devIter.next().getAttachmentPoints();
				for (SwitchPort srcsw : switches) {
					srcSwitch=srcsw;
				} //TODO: Take only one attached switch - find a best/efficient way to take one
			} 

			devIter = deviceManagerService.queryDevices(MacAddress.NONE, null, dstIP, IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
			if(devIter.hasNext()){
				switches = devIter.next().getAttachmentPoints();
				for (SwitchPort dstsw : switches) {
					dstSwitch=dstsw;
				} //TODO: Take only one attached switch - find a best/efficient way to take it
			} 
			
			
			ArrayList<SwitchPort> vals = 
	                new ArrayList<SwitchPort>(2);
	
	        vals.add(srcSwitch);
	        vals.add(dstSwitch);
	        return vals;
			
		}
		
		@Override
		public Path getLowerPathLatency(AppReq appReq){
			//TODO: Corrigir: problema: todos hosts tem que dar um ping na rede para o floodlight cadastrar seu IP
			//Isso altera as regras aplicadas aos roteadores (verificar)
			
			ArrayList<SwitchPort> srcDstSwitches = 
	                new ArrayList<SwitchPort>(2);
			SwitchPort srcSwitch = null;
			SwitchPort dstSwitch = null;
			
			srcDstSwitches = getSrcDstSwitchByIP(appReq.getSrcIP(), appReq.getDstIP());
			
			srcSwitch = srcDstSwitches.get(0);
			dstSwitch = srcDstSwitches.get(1);
			
			Path newPath = null;
			if ((srcSwitch!=null) & (dstSwitch!=null)){
				
				//Continuous or Lazy Monitoring
				if (appReq.getAdaptionRateType()==1) {
					newPath = routingEngineService.getPath(srcSwitch.getNodeId(), srcSwitch.getPortId(), dstSwitch.getNodeId(), dstSwitch.getPortId(), PATH_METRIC.LATENCY);	
				} else if (appReq.getAdaptionRateType()==2) {
					newPath = routingEngineService.getPathLazyLatency(srcSwitch.getNodeId(), srcSwitch.getPortId(), dstSwitch.getNodeId(), dstSwitch.getPortId());
				}
				//newPath = routingEngineService.getPath(srcSwitch.getNodeId(), dstSwitch.getNodeId(), PATH_METRIC.LATENCY);
				//log.info("path low latency inserted {}", newPath);
				
			}
			return newPath;
		}
		

		
		/**
         * Applies a Path with lower latency than the original if its possible 
         * Push the packet that originate this call
         *
         * @param  
         * @return true if new Path and Package was applied.
         */
		private boolean applyLowerLatencyPath(AppReq appReq, OFPacketIn pi,FloodlightContext cntx){
			//Path originalPath = routingService.getPath(srcSwitch.getNodeId(), srcSwitch.getPortId(), dstSwitch.getNodeId(), dstSwitch.getPortId());
			//TODO: Latencia retorna null quando o getPath e calculado com 4 parametros (se corrigir em topology o ping nao funciona mais)
			//Path originalPath = routingService.getPath(appReq.getSrcId(), appReq.getSrcPort(), appReq.getDstId(), appReq.getDstPort());
			Path originalPath = routingEngineService.getPath(appReq.getSrcId(), appReq.getDstId());
			//log.info("Path Atual {}", originalPath);
			U64 originalLatency = originalPath.getLatency();

			if (originalLatency != null){
				//log.info("Latencia da Rota Original {}", originalLatency.getValue());
				if (originalLatency.getValue() > appReq.getMax()){
					//log.info("Path Atual {}", originalPath);
					//log.info("Latencia da Rota Original {}", originalLatency.getValue());
					log.info("Trying to Set new latency.................");

					Path newPath = getLowerPathLatency(appReq);
					//log.info("Latencia da Rota Nova {}", newPath.getLatency());
					//TODO: verificar ao comparar rota original e nova, pois a nova eh calculada da porta
					//de inicio a porta fim, jah a rota original, descatar a 1a e ultima porta (getPath(src, dst) retorna a latencia, mas getpath(src,dst,srcport, dstport) nao retorna latencia)
					//if (!originalPath.equals(newPath) & newPath!=null){
					if (newPath!=null){ //only to tests 
					///TODO: Ou mesmo que a nova latencia ainda seja mais alta que max, se ela for menor que a latencia da rota antiga, substitui-la
					//if(newPath.getLatency().getValue()<originalLatency.getValue())

						log.info("Rota Nova {}", newPath.toString());
						boolean toReturn = pushRoute(newPath, appReq);
						//log.info("A nova rota foi escrita {} com latencia {}", toReturn, newPath.getLatency());
						IOFSwitch sw = switchService.getSwitch(appReq.getSrcId());
						pushPacket(sw, pi, newPath.getPath().get(0).getPortId(), false, cntx);
						return toReturn;
					} else {
						log.info("There are no lower delay path/route.");
					}
				} else {
					log.info("Latencia atual ja eh suficiente");
				} 

				//topologyService.getAllLinks();
				//linkDiscoveryService.getLinkInfo(l);			
			}
			log.info("Original Latency is null");
			return false;
		}
		
		
		/**
         * Applies a Path with lower latency than the original if its possible 
         *
         * @param  
         * @return true if new Path and Package was applied.
         */
		//TODO: Latencia retorna null quando o getPath e calculado com 4 parametros (se corrigir em topology o ping nao funciona mais)
		
		@Override
		public boolean applyLowerLatencyPath(AppReq appReq){
			
			/*Path originalPath = lastRoute.get(appReq.getName());
			
			//Se não tiver rota anterior
			if (originalPath == null) {
				//if routing in same PID. TODO: Solve it in Topology Instance - 
				if ((appReq.getSrcId().equals(appReq.getDstId()))) {
					log.info("origem e destino no mesmo switch");
					originalPath = routingEngineService.getPath(appReq.getSrcId(), appReq.getSrcPort(), appReq.getDstId(), appReq.getDstPort());
					boolean result = pushRoute(originalPath, appReq);
					if (result) {
						lastRoute.put(appReq.getName(), originalPath);
						log.info("nova rota adicionada MESMO SWITCH ao historico para {}", appReq.getName());
						return true;
					}

				} else {
					log.info("sem rota no historico. Calculando nova rota para {}", appReq.getName());
					originalPath = routingEngineService.getPath(appReq.getSrcId(), appReq.getDstId());
				}
			//Se tiver rota anterior
			} else {
				//if routing in same PID. TODO: Solve it in Topology Instance - 
				if ((appReq.getSrcId().equals(appReq.getDstId()))) {
					return false;
				}
			}*/
	
			/*
			U64 originalLatency = originalPath.getLatency();
			//	log.info("Latencia da rota antiga {}", originalPath.getLatency());
			
			//cuidado com a diferença entre getPath com 2 e 4 parametros
			//Aplica rota com nova latencia
			if (originalLatency != null){
				if (originalLatency.getValue() > appReq.getMax()){ 
					//log.info("Path Atual {}", originalPath);
					//log.info("Latencia Atual {}", originalLatency.getValue());
					//log.info("Trying to Set new latency to {}", appReq.getName());

*/
			Path newPath = getLowerPathLatency(appReq);
			if(newPath.getLatency().getValue() < appReq.getMax()){ //TODO: Verificar a eficiencia
				boolean result = pushRoute(newPath, appReq);
				if (result) {
					log.info("rota aplicada para {}", appReq.getName());
					log.info("rota aplicada {}", newPath.getPath());
					return true;
				}						
			} else {
				//TODO: ALERTA
				log.info("ALERT: There are no lower delay path/route to {}", appReq.getName());
				log.info("rota com delay acima do permitido {}", newPath.getPath());
				return false;
			}
			
			return false;
		}
	
		@Override
		public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
				FloodlightContext cntx) {

			//long startTime=System.nanoTime();
			//long endTime = System.nanoTime();
			//log.info("time {} in ms", (endTime-startTime)/1000000);
			
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			
			
			if (MqttUtils.isMqttMessage(eth)){
				IPv4 ipv4 = (IPv4) eth.getPayload();
				TCP tcp = (TCP) ipv4.getPayload();
				Data data = (Data) tcp.getPayload();
				
				MQTTDecoder mdecoder = new MQTTDecoder();
				List<Object> m_results;
				m_results = new ArrayList<Object >();
				
				//log.info("is mqtt, IP src ip {}, dst ip {}", ipv4.getSourceAddress(), ipv4.getDestinationAddress());

				/*
				log.info("Ethernet:{}", eth.toString());
				log.info("IP:{}", ipv4.toString());
				log.info("TCP:{}", tcp.getDataOffset());
				log.info("TCP:{}", tcp.getDestinationPort());
				log.info("TCP:{}", tcp.getSourcePort());
				log.info("TCP:{}", tcp.getSequence());*/
				//log.info("MQTT Serialized:{}", data.getData()	);
				//log.info("mensagem completa MQTT Serializ2:{}", eth.getPayload().getPayload().getPayload().serialize());
				
			
			
			//return Command.CONTINUE;
		
				
				ByteBuf m_buffer = Unpooled.copiedBuffer(eth.getPayload().getPayload().getPayload().serialize());
				
		        
				try {			
					mdecoder.decode(null, m_buffer, m_results);
					//log.info("Mensagem do tipo {}", mdecoder.getMessageTypeName());
					
					if (!m_results.isEmpty()){ 
						//log.info("IP src ip {}, dst ip {}", ipv4.getSourceAddress(), ipv4.getDestinationAddress()+ " "+mdecoder.getMessageTypeName());
						//log.info("Mensagem do tipo {}", mdecoder.getMessageTypeName());
						switch (mdecoder.getMessageType()){
						//TODO: tratar todos os tipos de mensagens MQTT	
						case AbstractMessage.PUBLISH : 
							PublishMessage mPublish = (PublishMessage) m_results.get(0);
							mqttMessageType = mPublish;
							//log.info("Mqtt Topic Publish {}", mPublish.getTopicName());
							//log.info("Mqtt Topic Publish {}", mPublish.getPayload());


							String topic = mPublish.getTopicName();
							Set<String> topics = topicReqService.getAllTopics();
							//log.info("All Topics  {}", topics);
							if (topics.contains(topic)){
								//log.info("Mqtt Topic Publish {}", mPublish.getTopicName());
								//log.info("Mqtt All Topics {}",topicReqService.getAllTopics());
								TopicReq topicReq = topicReqService.getTopicReqFromTopic(topic);
								

								ArrayList<SwitchPort> srcDstSwitches = 
						                new ArrayList<SwitchPort>(2);
								SwitchPort srcSwitch = null;
								SwitchPort dstSwitch = null;
								
								srcDstSwitches = getSrcDstSwitchByIP(ipv4.getSourceAddress(), ipv4.getDestinationAddress());
								
								//endTime = System.nanoTime();

								srcSwitch = srcDstSwitches.get(0);
								dstSwitch = srcDstSwitches.get(1);
								
								
								//Calculates Path and put in appReq
								//TODO: Verificar se nao encontrou valores/switches
								if ((srcSwitch!=null) & (dstSwitch!=null)){
									AppReq appReq = new AppReq(topic, topic, 
											//AppReq appReq = new AppReq(topic, topic, 
											ipv4.getSourceAddress(), ipv4.getDestinationAddress(),
											srcSwitch.getNodeId(), dstSwitch.getNodeId(), 
											srcSwitch.getPortId(), dstSwitch.getPortId(),
											tcp.getSourcePort(), tcp.getDestinationPort(), 
											topicReq.getMin(), topicReq.getMax(), topicReq.getAdaptationRateType(), topicReq.getTimeout());

									//TODO: Verify the efficiency
									if (appReqService.containsValue(appReq)){ //if appReq is already in list (compared with specific hashcode in AppReq)
										
										AppReq databasedAppReq = appReqService.getAppReq(appReq.getName());
										if (databasedAppReq.getTimeout()==0) {
											//Calculate timeout between first two messages 
											Long time = System.currentTimeMillis() - databasedAppReq.getTime();
											time=time/1000;
											appReq.setTimeout(time.intValue());
											appReqService.updateAppReq(appReq.getName(), appReq);
									  	} else { // 
									  		//log.info("Apagando historico de rota de {}, pois a msg chegou no controlador mesmo com monitor", appReq.getName());
									  		//lastRoute.remove(appReq.getName());
									  		
											log.info("Problem!!! app {} registered but message goes to Controller", appReq.getName());
											applyLowerLatencyPath(appReq);
											
											
									  	}
									} else {// TODO: verificar a eficiencia
										
										appReqService.addAppReq(AppReqPusher.TABLE_NAME, appReq); //insert to monitoring
										//A aplicacao da rota de menor latencia eh feita no proximo pacote encaminhado no switch, aplicado pelo AutoManIoT
										//Nao eh feito mais nesta etapa para nao atrasar o encaminhamento do pacote
										
//										if (appReq.getAdaptionRateType()==1){
//											//If it applied the new path, don't forward packet
//											if (applyLowerLatencyPath(appReq, (OFPacketIn) msg, cntx)){
//												log.info("-------------------COMMAND STOP ---- msg foi reenviada pelo IoTRouting, havia nova rota");
//												
//												endTime = System.nanoTime();
//										        log.info("-----------Tempo aplicar nova rota IoTRouting = {}ms", (endTime-startTime)/1000000);
//												
//												return Command.STOP;
//												
//											} else {
//												log.info("-------------------COMMAND CONTINUE ---- msg nao foi reenviada pelo IoTRouting e sim por Forwarding, nao havia nova rotas ou rota atual ja suficiente");
//												endTime = System.nanoTime();
//										        log.info("-----------Tempo pelo forwarding, conferindo por IoT = {}ms", (endTime-startTime)/1000000);	
//												return Command.CONTINUE;
//											}
//										}
//										
										
									}
									
									return Command.CONTINUE;
									//TODO: verificar se o pacote nao foi reenviado para o roteador para encaminhamento antes da nova rota ser aplicada
	
								
									
								}
						
							}
													
							//Se nao ha o topico na lista, prosseguir encaminhamento convencional
							
							break;
						case AbstractMessage.SUBSCRIBE : 
							SubscribeMessage mSubscribe = (SubscribeMessage) m_results.get(0);
							//log.info("Mqtt Topic Subscribe {}", mSubscribe.toString());
							break;
						case AbstractMessage.UNSUBSCRIBE : 
							UnsubscribeMessage mUnsubscribe = (UnsubscribeMessage) m_results.get(0);
							//log.info("Mqtt Topic Unsubscribe {}", mUnsubscribe.topics());
							break;
						case AbstractMessage.UNSUBACK: 
							UnsubAckMessage mUnsuback = (UnsubAckMessage) m_results.get(0);
							//log.info("Mqtt Topic UnsubAck {}", mUnsuback.toString());
							break;
						case AbstractMessage.PINGREQ: 
							PingReqMessage mPingReq = (PingReqMessage) m_results.get(0);
							//log.info("Mqtt Topic PingRequest {}", mPingReq.toString());
							break;
						case AbstractMessage.PINGRESP: 
							PingRespMessage mPingResp = (PingRespMessage) m_results.get(0);
							//log.info("Mqtt Topic PingResponse {}", mPingResp.toString());
							break;
						}
						
						
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			return Command.CONTINUE;
		
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return IoTRouting.class.getSimpleName();
		}

		@Override
		public boolean isCallbackOrderingPrereq(OFType type, String name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isCallbackOrderingPostreq(OFType type, String name) {
			//If its an IoT MQTT packet, it will processed only by IoTRouting, with forwarding actions
			return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
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
		public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
			// TODO Auto-generated method stub
			
		}


}
	
	

