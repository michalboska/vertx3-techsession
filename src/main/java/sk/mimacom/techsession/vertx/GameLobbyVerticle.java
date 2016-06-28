package sk.mimacom.techsession.vertx;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;

@ProxyGen
@VertxGen
public interface GameLobbyVerticle {

	static GameLobbyVerticle create(String addressToListenOn) {
		return new GameLobbyVerticleImpl(addressToListenOn);
	}

	static GameLobbyVerticle createProxy(Vertx vertx, String address) {
		return new GameLobbyVerticleVertxEBProxy(vertx, address);
	}

	void onGameEnded(String gameGuid);

}
