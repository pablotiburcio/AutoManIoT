package net.floodlightcontroller.automaniot.mqtt;

/**
*
*/
class PubCompDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubCompMessage();
    }
}
