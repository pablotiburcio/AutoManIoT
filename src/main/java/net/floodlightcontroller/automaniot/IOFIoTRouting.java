package net.floodlightcontroller.automaniot;

import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.routing.Path;

public interface IOFIoTRouting extends IFloodlightService {
	Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, OFPacketIn pi, FloodlightContext cntx);
	
	Match createMatch(IOFSwitch sw, OFPort inPort, AppReq appReq, String protocol);
	
	Match createReverseMatch(IOFSwitch sw, OFPort inPort, AppReq appReq, String protocol);
	
	boolean pushRoute(Path route, Match match,
            DatapathId pinSwitch, U64 cookie,
            boolean requestFlowRemovedNotification, OFFlowModCommand flowModCommand, boolean bidirectional);
	
	boolean setLowerLatencyPath(Path oldPath, IOFSwitchService switchService, AppReq appReq);
	
}
