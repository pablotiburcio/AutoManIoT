package net.floodlightcontroller.automaniot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.automaniot.web.RoutableAppReq;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.storage.IResultSet;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.storage.StorageException;

public class AppReqPusher
implements IFloodlightModule, IStorageSourceListener, IAppReqPusherService {
	protected static Logger log = LoggerFactory.getLogger(AppReqPusher.class);
	public static final String MODULE_NAME = "appreqpusher";

	public static final int STATIC_ENTRY_APP_ID = 10;
	//static {
	//	AppCookie.registerApp(STATIC_ENTRY_APP_ID, MODULE_NAME);
	//}

	public static final String TABLE_NAME = "appreqtable";
	
	protected IFloodlightProviderService floodlightProviderService;
	protected IOFSwitchService switchService;
	protected IStorageSourceService storageSourceService;
	protected IRestApiService restApiService;

	
	protected Map<String, AppReq> appReqFromStorage;
	
	//add new values in net.floodlightcontroller.core.web.serializers.AppReqMapSerializer
	public static class Columns {
		public static final String COLUMN_NAME = "name";
		public static final String COLUMN_TOPIC = "topic";
		public static final String COLUMN_SOURCE_IP = "src_ip";
		public static final String COLUMN_DESTINATION_IP = "dst_ip";
		public static final String COLUMN_SOURCE_ID = "src_id";
		public static final String COLUMN_DESTINATION_ID = "dst_id";
		public static final String COLUMN_SOURCE_PORT = "src_port";
		public static final String COLUMN_DESTINATION_PORT = "dst_port";
		public static final String COLUMN_MIN = "min";
		public static final String COLUMN_MAX = "max";
		public static final String COLUMN_ADAPTATION_RATE_TYPE = "adap_rate_type";
		public static final String COLUMN_TIME_OUT = "time_out";
		
		private static Set<String> ALL_COLUMNS;	/* Use internally to query only */
	}
	
	
	@Override
	public void addAppReq(String name, AppReq appReq) {
		try {
			Map<String, Object> map = AppReqEntries.appReqToStorageEntry(appReq);
			storageSourceService.insertRowAsync(TABLE_NAME, map);
			appReqFromStorage = readAppReqFromStorage();
		} catch (Exception e) {
			log.error("Did not add AppReq with bad match/action combination. {}", appReq.toString());
		}
		
	}

	@Override
	public void deleteAppReq(String name) {
		storageSourceService.deleteRowAsync(TABLE_NAME, name);
		
	}

	@Override
	public void deleteAllAppReq() {
		for (String entry : appReqFromStorage.keySet()) {
			deleteAppReq(entry);
		}
		
	}

	@Override
	public Map<String, AppReq> getAllAppReq() {
		appReqFromStorage = readAppReqFromStorage();
		return appReqFromStorage;
	}

	@Override
	public AppReq getAppReq(String reqId) {
		appReqFromStorage = readAppReqFromStorage();
		AppReq m = appReqFromStorage.get(reqId);
		return m;
	}
	
	public Set<String> getAllTopics(){
		//appReqFromStorage;
		Set<String> topics = new HashSet<String>();
		for (AppReq appReq : appReqFromStorage.values()) {
			topics.add(appReq.getTopic());
		}
		return topics;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IAppReqPusherService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>,
		IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>,
		IFloodlightService>();
		m.put(IAppReqPusherService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IOFSwitchService.class);
		l.add(IStorageSourceService.class);
		l.add(IRestApiService.class);
		return l;
	}
	
	
	private void populateColumns() {
		Set<String> tmp = new HashSet<String>();
		tmp.add(Columns.COLUMN_NAME);
		tmp.add(Columns.COLUMN_TOPIC);
		tmp.add(Columns.COLUMN_SOURCE_IP);
		tmp.add(Columns.COLUMN_DESTINATION_IP);
		tmp.add(Columns.COLUMN_SOURCE_ID);
		tmp.add(Columns.COLUMN_DESTINATION_ID);
		tmp.add(Columns.COLUMN_SOURCE_PORT);
		tmp.add(Columns.COLUMN_DESTINATION_PORT);
		tmp.add(Columns.COLUMN_MIN);
		tmp.add(Columns.COLUMN_MAX);
		tmp.add(Columns.COLUMN_ADAPTATION_RATE_TYPE);
		tmp.add(Columns.COLUMN_TIME_OUT);
		
		Columns.ALL_COLUMNS = ImmutableSet.copyOf(tmp);
		
	}
	
	void parseRow(Map<String, Object> row, Map<String, AppReq> appReqs){
		
		IPv4 ipv4 = new IPv4();	
		ipv4.setSourceAddress((String)row.get(Columns.COLUMN_SOURCE_IP));
		ipv4.setDestinationAddress((String)row.get(Columns.COLUMN_DESTINATION_IP));
		
		DatapathId srcId = DatapathId.of((String)row.get(Columns.COLUMN_SOURCE_ID));
		DatapathId dstId = DatapathId.of((String)row.get(Columns.COLUMN_DESTINATION_ID));
		//DatapathId dstId = new DatapathId();
		
		TCP tcp = new TCP();
		tcp.setSourcePort(Integer.valueOf((String)row.get(Columns.COLUMN_SOURCE_PORT)));
		tcp.setDestinationPort(Integer.valueOf((String)row.get(Columns.COLUMN_DESTINATION_PORT)));

		int min = Integer.valueOf((String)row.get(Columns.COLUMN_MIN));
		int max = Integer.valueOf((String)row.get(Columns.COLUMN_MAX));
		int adap_rate_type = Integer.valueOf((String)row.get(Columns.COLUMN_ADAPTATION_RATE_TYPE));
		int timeout = Integer.valueOf((String)row.get(Columns.COLUMN_TIME_OUT));
		String name = (String)row.get(Columns.COLUMN_NAME);
		String topic = (String)row.get(Columns.COLUMN_TOPIC);

		AppReq reqTable = new AppReq(name, topic, ipv4.getSourceAddress(), ipv4.getDestinationAddress(), srcId, dstId, 
				tcp.getSourcePort(), tcp.getDestinationPort(), min, max, adap_rate_type,timeout);
		appReqs.put((String) row.get(Columns.COLUMN_NAME), reqTable);
	}
	
	/**
	 * Read entries from storageSource, and store them in a hash
	 *
	 * @return
	 */
	private Map<String, AppReq> readAppReqFromStorage() {
		Map<String, AppReq> appReqs = new HashMap<String, AppReq>();
		try {
			Map<String, Object> row;
			// null1=no predicate, null2=no ordering
			IResultSet resultSet = storageSourceService.executeQuery(TABLE_NAME, Columns.ALL_COLUMNS.toArray(new String[Columns.ALL_COLUMNS.size()]), null, null);
			for (Iterator<IResultSet> it = resultSet.iterator(); it.hasNext();) {
				row = it.next().getRow();
				//log.info("row ----- "+ row.toString());
				parseRow(row, appReqs);
			}
		} catch (StorageException e) {
			log.error("failed to access storage: {}", e.getMessage());
			// if the table doesn't exist, then wait to populate later via
			// setStorageSource()
		}
		return appReqs;
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		populateColumns();
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		storageSourceService = context.getServiceImpl(IStorageSourceService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		log.info("Starting AppReqPusher...");
		//floodlightProviderService.addOFMessageListener(OFType.FLOW_REMOVED, this);
		storageSourceService.createTable(TABLE_NAME, null);
		storageSourceService.setTablePrimaryKeyName(TABLE_NAME, Columns.COLUMN_NAME);
		storageSourceService.addListener(TABLE_NAME, this);
		appReqFromStorage = readAppReqFromStorage();
		restApiService.addRestletRoutable(new RoutableAppReq()); /* current */
		
	}

	@Override
	public void rowsModified(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, AppReq> getAppReq(int reqId) {
		// TODO Auto-generated method stub
		return null;
	}


}
