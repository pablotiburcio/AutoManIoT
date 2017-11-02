<<<<<<< HEAD
#AutoManIoT
It's a project to construct an Autonomous IoT Architecture.

It uses SDN and NFV as implementation infrastructure.


*SDN infrastructure is developed over Floodlight SDN controller code;

*Testbed are executed in Mininet Emulator;

*Messages from devices (MTC or M2M) uses MQTT protocol;



In order to AutoManIoT work:

Put these lines in src/main/resources/floodlightdefault.properties: 
net.floodlightcontroller.automaniot.AppReqPusher,\
net.floodlightcontroller.automaniot.AutoManIoT

and 

net.floodlightcontroller.automaniot.AppReqPusher
net.floodlightcontroller.automaniot.AutoManIoT

in  src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
=======
# AutoManIoT
It's a project to contruct an Autonomous IoT Architecture. 

It uses SDN and NFV as implantation infrastructure. 


*SDN infrastructure is developed over Floodlight SDN controller code; 

*Testbed are executed in the Mininet Emulator; 

*Message from Devices (MTC or M2M) uses MQTT protocol; 
>>>>>>> 2a0078577e1c749cbad1a9a6e525e02d81451f0f
