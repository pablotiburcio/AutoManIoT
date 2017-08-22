package net.floodlightcontroller.automaniot.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;


public class RoutableTopicReq implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/add/json", TopicReqPusherResource.class);
		router.attach("/list/{topic}/json", ListTopicReqResource.class);
		router.attach("/delete/{topic}/json", DeleteTopicReqResource.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/wm/topicreq";
	}

}
