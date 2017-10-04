package net.floodlightcontroller.automaniot.mqtt;


/**
 *
 */
public class PubRelMessage extends MessageIDMessage {
    
    public PubRelMessage() {
        m_messageType = AbstractMessage.PUBREL;
    }
}