package net.floodlightcontroller.automaniot.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;

/**
 *
 */
abstract class MessageIDDecoder extends DemuxDecoder {
    
    protected abstract MessageIDMessage createMessage();

    @Override
    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.resetReaderIndex();
        //Common decoding part
        MessageIDMessage message = createMessage();
        if (!decodeCommonHeader(message, in)) {
            in.resetReaderIndex();
            return;
        }
        
        //read  messageIDs
        message.setMessageID(in.readUnsignedShort());
        out.add(message);
    }
    
}