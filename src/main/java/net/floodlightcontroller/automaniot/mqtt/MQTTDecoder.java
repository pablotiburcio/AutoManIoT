package net.floodlightcontroller.automaniot.mqtt;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MQTTDecoder extends ByteToMessageDecoder {
	protected static Logger log = LoggerFactory.getLogger(MQTTDecoder.class);

    private Map<Byte, DemuxDecoder> m_decoderMap = new HashMap<Byte, DemuxDecoder>();
    
    private byte messageType;
    
    
    
    public MQTTDecoder() {
       m_decoderMap.put(AbstractMessage.CONNECT, new ConnectDecoder());
       m_decoderMap.put(AbstractMessage.CONNACK, new ConnAckDecoder());
       m_decoderMap.put(AbstractMessage.PUBLISH, new PublishDecoder());
       m_decoderMap.put(AbstractMessage.PUBACK, new PubAckDecoder());
       m_decoderMap.put(AbstractMessage.SUBSCRIBE, new SubscribeDecoder());
       m_decoderMap.put(AbstractMessage.SUBACK, new SubAckDecoder());
       m_decoderMap.put(AbstractMessage.UNSUBSCRIBE, new UnsubscribeDecoder());
       m_decoderMap.put(AbstractMessage.DISCONNECT, new DisconnectDecoder());
       m_decoderMap.put(AbstractMessage.PINGREQ, new PingReqDecoder());
       m_decoderMap.put(AbstractMessage.PINGRESP, new PingRespDecoder());
       m_decoderMap.put(AbstractMessage.UNSUBACK, new UnsubAckDecoder());
       m_decoderMap.put(AbstractMessage.PUBCOMP, new PubCompDecoder());
       m_decoderMap.put(AbstractMessage.PUBREC, new PubRecDecoder());
       m_decoderMap.put(AbstractMessage.PUBREL, new PubRelDecoder());
    }

    @Override
	public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    	log.info("-----------------------------bytebuf = {}",in.toString());
        in.markReaderIndex();
        if (!MqttUtils.checkHeaderAvailability(in)) {
        	log.info("Byte menor que 1");
            in.resetReaderIndex();
            return;
        }
        in.resetReaderIndex();
        
        messageType = MqttUtils.readMessageType(in);
        
        DemuxDecoder decoder = m_decoderMap.get(messageType);
        if (decoder == null) {
            throw new CorruptedFrameException("Can't find any suitable decoder for message type: " + messageType);
        }
        decoder.decode(ctx, in, out);
        
    }
    
    public byte getMessageType(){
    	 return messageType;
         
    }
    
    public String getMessageTypeName() {
        
        return  messageType==1 ? "CONNECT": 
        	messageType==2 ? "CONNACK": 
        	messageType==3 ? "PUBLISH":
        	messageType==4 ? "PUBACK":
        	messageType==5 ? "PUBREC":
        	messageType==6 ? "PUBREL":
        	messageType==7 ? "PUBCOMP":
        	messageType==8 ? "SUBSCRIBE":
        	messageType==9 ? "SUBACK":
        	messageType==10 ? "UNSUBSCRIBE":
        	messageType==11 ? "UNSUBACK":
        	messageType==12 ? "PINGREQ":
        	messageType==13 ? "PINGRESP": 
        	messageType==14 ? "DISCONNECT" : "NO_MQTT";
     
    }
}
