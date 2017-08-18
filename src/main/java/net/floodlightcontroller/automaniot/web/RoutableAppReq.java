package net.floodlightcontroller.automaniot.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;


public class RoutableAppReq implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/add/json", AppReqPusherResource.class);
		router.attach("/list/{app}/json", ListAppReqResource.class);
		router.attach("/delete/{app}/json", DeleteAppReqResource.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/wm/appreq";
	}

}
