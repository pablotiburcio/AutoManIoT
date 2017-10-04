package net.floodlightcontroller.automaniot.mqtt;


/**
 *
 */
public class UnsubAckMessage extends MessageIDMessage {
    
    public UnsubAckMessage() {
        m_messageType = AbstractMessage.UNSUBACK;
    }
}