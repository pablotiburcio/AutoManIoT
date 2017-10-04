package net.floodlightcontroller.automaniot.mqtt;


/**
 *
 */
public class PubCompMessage extends MessageIDMessage {
    
    public PubCompMessage() {
        m_messageType = AbstractMessage.PUBCOMP;
    }
}