package net.floodlightcontroller.automaniot.mqtt;

/**
 * Doesn't care DUP, QOS and RETAIN flags.
 * @author andrea
 */
public class PingRespMessage extends ZeroLengthMessage {
    
    public PingRespMessage() {
        m_messageType = AbstractMessage.PINGRESP;
    }
}