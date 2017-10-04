package net.floodlightcontroller.automaniot.mqtt;

/**
 * Placeholder for PUBREC message.
 * 
 */
public class PubRecMessage extends MessageIDMessage {
    
    public PubRecMessage() {
        m_messageType = AbstractMessage.PUBREC;
    }
}
