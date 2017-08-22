package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import net.floodlightcontroller.automaniot.AppReq;
import net.floodlightcontroller.automaniot.web.AppReqMap;

public class AppReqMapSerializer extends JsonSerializer<AppReqMap> {

	@Override
	public void serialize(AppReqMap appReqMap, JsonGenerator jGen, SerializerProvider serializer)
			throws IOException, JsonProcessingException {
		
		if (appReqMap == null) {
			jGen.writeStartObject();
			jGen.writeString("No flows have been added to the Static Flow Pusher.");
			jGen.writeEndObject();
			return;
		}
		
		Map<String, AppReq> theMap = appReqMap.getMap();
		
		for (String id : theMap.keySet()) {
			if (theMap.get(id) != null) {
				jGen.writeArrayFieldStart(id);
				jGen.writeStartObject();
				jGen.writeFieldName("topic");
				jGen.writeString(theMap.get(id).getTopic());
				jGen.writeFieldName("src_ip");
				jGen.writeString(theMap.get(id).getSrcIP().toString());
				jGen.writeFieldName("dst_ip");
				jGen.writeString(theMap.get(id).getDstIP().toString());
				jGen.writeFieldName("src_id");
				jGen.writeString(theMap.get(id).getSrcId().toString());
				jGen.writeFieldName("dst_id");
				jGen.writeString(theMap.get(id).getDstId().toString());
				jGen.writeFieldName("src_port");
				jGen.writeString(theMap.get(id).getSrcPort().toString());
				jGen.writeFieldName("dst_port");
				jGen.writeString(theMap.get(id).getDstPort().toString());
				jGen.writeFieldName("src_trans_port");
				jGen.writeString(theMap.get(id).getSrcTransPort().toString());
				jGen.writeFieldName("dst_trans_port");
				jGen.writeString(theMap.get(id).getDstTransPort().toString());
				jGen.writeFieldName("min");
				jGen.writeString(Integer.toString(theMap.get(id).getMin()));
				jGen.writeFieldName("max");
				jGen.writeString(Integer.toString(theMap.get(id).getMax()));
				jGen.writeFieldName("adap_rate_type");
				jGen.writeString(Integer.toString(theMap.get(id).getAdaptionRateType()));
				jGen.writeFieldName("time_out");
				jGen.writeString(Integer.toString(theMap.get(id).getTimeout()));
				jGen.writeEndObject();
			}
		}
		
		//jGen.writeString(appReq.getMap().getSrcIP().toString());
		
	}

}
