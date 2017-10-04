package net.floodlightcontroller.automaniot.mqtt;

/**
 * Doesn't care DUP, QOS and RETAIN flags.
 * 
 */
public class DisconnectMessage extends ZeroLengthMessage {
    
    public DisconnectMessage() {
        m_messageType = AbstractMessage.DISCONNECT;
    }
}
