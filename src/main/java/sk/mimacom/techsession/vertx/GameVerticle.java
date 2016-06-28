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
		return new GameVerticleImpl(gameGuid, firstPlayerGuid, firstPlayerName);
	}

	static GameVerticle createProxy(Vertx vertx, String gameGuid) {
		return new GameVerticleVertxEBProxy(vertx, GameVerticleImpl.Constants.getPrivateQueueAddressForGame(gameGuid));
	}

	void addPlayer(String playerGuid, String playerName, Handler<AsyncResult<Void>> handler);
}
