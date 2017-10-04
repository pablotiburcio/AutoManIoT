package net.floodlightcontroller.automaniot.mqtt;

/**
 * Doesn't care DUP, QOS and RETAIN flags.
 */
public class PingRespMessage extends ZeroLengthMessage {
    
    public PingRespMessage() {
        m_messageType = AbstractMessage.PINGRESP;
    }
}