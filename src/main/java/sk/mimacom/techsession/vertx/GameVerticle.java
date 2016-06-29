package sk.mimacom.techsession.vertx;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@ProxyGen
@VertxGen
public interface GameVerticle {

	static GameVerticle create(String gameGuid, String firstPlayerGuid, String firstPlayerName) {
		//TODO: Create and return a configured GameVerticle instance
		return null;
	}

	static GameVerticle createProxy(Vertx vertx, String gameGuid) {
		//TODO: Return a Proxy using a specifig Eventbus address to send messages to. That address is calculated based on gameGuid
		return null;
	}

	void addPlayer(String playerGuid, String playerName, Handler<AsyncResult<Void>> handler);
}
