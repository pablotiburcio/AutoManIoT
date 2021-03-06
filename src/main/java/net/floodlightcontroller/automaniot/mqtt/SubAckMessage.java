package net.floodlightcontroller.automaniot.mqtt;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SubAckMessage extends MessageIDMessage {

    List<QOSType> m_types = new ArrayList<QOSType>();
    
    public SubAckMessage() {
        m_messageType = AbstractMessage.SUBACK;
    }

    public List<QOSType> types() {
        return m_types;
    }

    public void addType(QOSType type) {
        m_types.add(type);
    }
}
