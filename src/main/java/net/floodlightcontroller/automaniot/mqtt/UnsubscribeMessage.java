package net.floodlightcontroller.automaniot.mqtt;


import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class UnsubscribeMessage extends MessageIDMessage {
    List<String> m_types = new ArrayList<String>();
    
    public UnsubscribeMessage() {
        m_messageType = AbstractMessage.UNSUBSCRIBE;
    }

    public List<String> topics() {
        return m_types;
    }

    public void addTopic(String type) {
        m_types.add(type);
    }
}