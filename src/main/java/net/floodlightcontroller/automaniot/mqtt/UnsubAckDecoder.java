package net.floodlightcontroller.automaniot.mqtt;


/**
 *
 */
class UnsubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new UnsubAckMessage();
    }
}
