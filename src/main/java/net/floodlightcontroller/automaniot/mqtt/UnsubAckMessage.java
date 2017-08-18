package net.floodlightcontroller.automaniot.mqtt;


/**
 *
 * @author andrea
 */
public class UnsubAckMessage extends MessageIDMessage {
    
    public UnsubAckMessage() {
        m_messageType = AbstractMessage.UNSUBACK;
    }
}