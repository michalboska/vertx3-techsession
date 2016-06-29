package sk.mimacom.techsession.vertx;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;

@ProxyGen
@VertxGen
public interface GameLobbyVerticle {

	static GameLobbyVerticle create(String addressToListenOn) {

		//TODO: create and return the verticle's instance
		return null;
	}

	static GameLobbyVerticle createProxy(Vertx vertx, String address) {
		//TODO: Return the proxy object
		return null;
	}

	void onGameEnded(String gameGuid);

}
