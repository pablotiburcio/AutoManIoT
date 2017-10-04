package net.floodlightcontroller.automaniot.mqtt;

/**
 * Doesn't care DUP, QOS and RETAIN flags.
 * 
 */
public class PingReqMessage extends ZeroLengthMessage {
    
    public PingReqMessage() {
        m_messageType = AbstractMessage.PINGREQ;
    }
}