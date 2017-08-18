package net.floodlightcontroller.automaniot.mqtt;


/**
 *
 * @author andrea
 */
class UnsubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new UnsubAckMessage();
    }
}
