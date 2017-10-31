package net.floodlightcontroller.automaniot.mqtt;

/**
*
*/
class PubRecDecoder extends MessageIDDecoder {

   @Override
   protected MessageIDMessage createMessage() {
       return new PubRecMessage();
   }
}
