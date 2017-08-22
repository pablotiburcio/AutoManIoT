package net.floodlightcontroller.automaniot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.automaniot.web.RoutableTopicReq;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.storage.IResultSet;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.storage.StorageException;

public class TopicReqPusher 
	implements IFloodlightModule, IStorageSourceListener, ITopicReqPusherService {
		protected static Logger log = LoggerFactory.getLogger(TopicReqPusher.class);
		public static final String MODULE_NAME = "topicreqpusher";

		public static final int STATIC_ENTRY_APP_ID = 10;
		//static {
		//	AppCookie.registerApp(STATIC_ENTRY_APP_ID, MODULE_NAME);
		//}

		public static final String TABLE_NAME = "topicreqtable";
		
		protected IFloodlightProviderService floodlightProviderService;
		protected IOFSwitchService switchService;
		protected IStorageSourceService storageSourceService;
		protected IRestApiService restApiService;

		
		protected Map<String, TopicReq> topicReqFromStorage;
		
		//add new values in net.floodlightcontroller.core.web.serializers.TopicReqMapSerializer
		public static class Columns {
			public static final String COLUMN_TOPIC = "topic";
			public static final String COLUMN_MIN = "min";
			public static final String COLUMN_MAX = "max";
			public static final String COLUMN_TIME_OUT = "time_out";
			
			private static Set<String> ALL_COLUMNS;	/* Use internally to query only */
		}
		
		
		@Override
		public void addTopicReq(String name, TopicReq topicReq) {
			try {
				Map<String, Object> map = TopicReqEntries.topicReqToStorageEntry(topicReq);
				storageSourceService.insertRowAsync(TABLE_NAME, map);
				topicReqFromStorage = readTopicReqFromStorage();
			} catch (Exception e) {
				log.error("Did not add TopicReq with bad match/action combination. {}", topicReq.toString());
			}
			
		}

		@Override
		public void deleteTopicReq(String name) {
			storageSourceService.deleteRowAsync(TABLE_NAME, name);
			
		}

		@Override
		public void deleteAllTopicReq() {
			for (String entry : topicReqFromStorage.keySet()) {
				deleteTopicReq(entry);
			}
			
		}

		@Override
		public Map<String, TopicReq> getAllTopicReq() {
			topicReqFromStorage = readTopicReqFromStorage();
			return topicReqFromStorage;
		}

		@Override
		public TopicReq getTopicReq(String reqId) {
			topicReqFromStorage = readTopicReqFromStorage();
			TopicReq m = topicReqFromStorage.get(reqId);
			return m;
		}
		
		public Set<String> getAllTopics(){
			//appReqFromStorage;
			Set<String> topics = new HashSet<String>();
			for (TopicReq topicReq : topicReqFromStorage.values()) {
				topics.add(topicReq.getTopic());
			}
			return topics;
		}

		@Override
		public Collection<Class<? extends IFloodlightService>> getModuleServices() {
			Collection<Class<? extends IFloodlightService>> l =
					new ArrayList<Class<? extends IFloodlightService>>();
			l.add(ITopicReqPusherService.class);
			return l;
		}

		@Override
		public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
			Map<Class<? extends IFloodlightService>,
			IFloodlightService> m =
			new HashMap<Class<? extends IFloodlightService>,
			IFloodlightService>();
			m.put(ITopicReqPusherService.class, this);
			return m;
		}

		@Override
		public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
			Collection<Class<? extends IFloodlightService>> l =
					new ArrayList<Class<? extends IFloodlightService>>();
			l.add(IFloodlightProviderService.class);
			l.add(IStorageSourceService.class);
			l.add(IRestApiService.class);
			return l;
		}
		
		
		private void populateColumns() {
			Set<String> tmp = new HashSet<String>();
			tmp.add(Columns.COLUMN_TOPIC);
			tmp.add(Columns.COLUMN_MIN);
			tmp.add(Columns.COLUMN_MAX);
			tmp.add(Columns.COLUMN_TIME_OUT);
			
			Columns.ALL_COLUMNS = ImmutableSet.copyOf(tmp);
			
		}
		
		void parseRow(Map<String, Object> row, Map<String, TopicReq> topicReqs){
			
	
			int min = Integer.valueOf((String)row.get(Columns.COLUMN_MIN));
			int max = Integer.valueOf((String)row.get(Columns.COLUMN_MAX));
			int timeout = Integer.valueOf((String)row.get(Columns.COLUMN_TIME_OUT));
			String topic = (String)row.get(Columns.COLUMN_TOPIC);

			TopicReq reqTable = new TopicReq(topic, min, max, timeout);
			topicReqs.put((String) row.get(Columns.COLUMN_TOPIC), reqTable);
		}
		
		/**
		 * Read entries from storageSource, and store them in a hash
		 *
		 * @return
		 */
		private Map<String, TopicReq> readTopicReqFromStorage() {
			Map<String, TopicReq> topicReqs = new HashMap<String, TopicReq>();
			try {
				Map<String, Object> row;
				// null1=no predicate, null2=no ordering
				IResultSet resultSet = storageSourceService.executeQuery(TABLE_NAME, Columns.ALL_COLUMNS.toArray(new String[Columns.ALL_COLUMNS.size()]), null, null);
				for (Iterator<IResultSet> it = resultSet.iterator(); it.hasNext();) {
					row = it.next().getRow();
					parseRow(row, topicReqs);
				}
			} catch (StorageException e) {
				log.error("failed to access storage: {}", e.getMessage());
				// if the table doesn't exist, then wait to populate later via
				// setStorageSource()
			}
			return topicReqs;
		}
		
		@Override
		public void init(FloodlightModuleContext context) throws FloodlightModuleException {
			populateColumns();
			floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
			storageSourceService = context.getServiceImpl(IStorageSourceService.class);
			restApiService = context.getServiceImpl(IRestApiService.class);
			
		}

		@Override
		public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
			log.info("Starting AppReqPusher...");
			//floodlightProviderService.addOFMessageListener(OFType.FLOW_REMOVED, this);
			storageSourceService.createTable(TABLE_NAME, null);
			storageSourceService.setTablePrimaryKeyName(TABLE_NAME, Columns.COLUMN_TOPIC);
			storageSourceService.addListener(TABLE_NAME, this);
			topicReqFromStorage = readTopicReqFromStorage();
			restApiService.addRestletRoutable(new RoutableTopicReq()); /* current */
			
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
		public Map<String, TopicReq> getTopicReq(int reqId) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TopicReq getTopicReqFromTopic(String topic) {
			topicReqFromStorage = readTopicReqFromStorage();
			TopicReq m = topicReqFromStorage.get(topic);
			return m;
		}

	
}
