package net.floodlightcontroller.automaniot.mqtt;


class PubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubAckMessage();
    }
    
}