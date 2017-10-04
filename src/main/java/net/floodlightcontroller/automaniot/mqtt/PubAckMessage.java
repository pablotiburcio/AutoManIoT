package net.floodlightcontroller.automaniot.mqtt;

/**
 * Placeholder for PUBACK message.
 * 
 */
public class PubAckMessage extends MessageIDMessage {
    
    public PubAckMessage() {
        m_messageType = AbstractMessage.PUBACK;
    }
}
