package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import net.floodlightcontroller.automaniot.TopicReq;
import net.floodlightcontroller.automaniot.web.TopicReqMap;

public class TopicReqMapSerializer extends JsonSerializer<TopicReqMap> {

	@Override
	public void serialize(TopicReqMap topicReqMap, JsonGenerator jGen, SerializerProvider serializer)
			throws IOException, JsonProcessingException {
		
		if (topicReqMap == null) {
			jGen.writeStartObject();
			jGen.writeString("No flows have been added to the Static Flow Pusher.");
			jGen.writeEndObject();
			return;
		}
		
		Map<String, TopicReq> theMap = topicReqMap.getMap();
		
		for (String id : theMap.keySet()) {
			if (theMap.get(id) != null) {
				jGen.writeArrayFieldStart(id);
				jGen.writeStartObject();
				jGen.writeFieldName("topic");
				jGen.writeString(theMap.get(id).getTopic());
				jGen.writeFieldName("min");
				jGen.writeString(Integer.toString(theMap.get(id).getMin()));
				jGen.writeFieldName("max");
				jGen.writeString(Integer.toString(theMap.get(id).getMax()));
				jGen.writeFieldName("time_out");
				jGen.writeString(Integer.toString(theMap.get(id).getTimeout()));
				jGen.writeEndObject();
			}
		}
		
		//jGen.writeString(appReq.getMap().getSrcIP().toString());
		
	}

}
