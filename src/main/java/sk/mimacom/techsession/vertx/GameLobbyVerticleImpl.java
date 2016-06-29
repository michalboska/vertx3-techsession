package sk.mimacom.techsession.vertx;


import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import io.vertx.core.Verticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.StringEscapeUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.serviceproxy.ProxyHelper;
import sk.mimacom.techsession.vertx.dto.AsyncHandlerDTO;
import sk.mimacom.techsession.vertx.dto.ErrorDTO;
import sk.mimacom.techsession.vertx.dto.lobby.AddGameDTO;
import sk.mimacom.techsession.vertx.dto.lobby.AddPlayerDTO;
import sk.mimacom.techsession.vertx.dto.lobby.AvailableGameDTO;
import sk.mimacom.techsession.vertx.dto.lobby.JoinGameDTO;
import sk.mimacom.techsession.vertx.dto.lobby.ListPlayersDTO;
import sk.mimacom.techsession.vertx.entity.Entity;
import sk.mimacom.techsession.vertx.entity.Game;
import sk.mimacom.techsession.vertx.entity.Player;

class GameLobbyVerticleImpl extends PongVerticle implements GameLobbyVerticle {
	public static final String QUEUE_LOBBY = "GameLobbyVerticle.queue";

	private static final String ERROR_NO_SUCH_PLAYER = "No such player exists";
	private static final String ERROR_NO_SUCH_GAME = "No such game exists";

	private Logger logger = LoggerFactory.getLogger(GameLobbyVerticleImpl.class);

	private Map<String, Game> activeGames = new HashMap<>();
	private Map<String, Player> activePlayers = new HashMap<>();
	private Map<String, Player> activePlayersByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private Map<String, String> deploymentIDs = new HashMap<>();
	private Game joinableGame;
	private String privateQueueAddress;

	//we don't actually need this instance in our app, but if we needed to be able to unsubscribe from this address
	// (while keeping this verticle still alive), we would do it by calling this consumer's unregister() method
	private MessageConsumer<JsonObject> serviceProxyConsumer;

	GameLobbyVerticleImpl(String privateQueueAddress) {
		this.privateQueueAddress = privateQueueAddress;
	}

	@Override
	public void start() throws Exception {

		/**
		 * TODO: Subscribe to following eventbus addresses:
		 * - configuration supplied address for private queue (use the "microservice" API for this)
		 * - QUEUE_LOBBY for incomming requests from players (more commonly, websocket clients)
		 * - HTTPServerVerticle.TOPIC_SOCKJS_MESSAGES to be informed about players that disconnect
		 */
	}

	private JsonObject handleMessageFromPlayer(Message<JsonObject> message) {
		JsonObject result = null;
		JsonObject body = message.body();
		switch (body.getString("type")) {
			case "addPlayer":
				logger.info("Adding a new player");
				result = addPlayer(body);
				break;
			case "addGame":
				logger.info("Creating a new game");
				result = addGame(message);
				break;
			case "joinGame":
				logger.info("Joining an existing game");
				result = joinGame(message);
				break;
			case "getAvailableGame":
				logger.info("Getting available game");
				result = getAvailableGame();
				break;
			case "listPlayers":
				logger.info("Listing players");
				result = listPlayers();
				break;
		}
		if (result != null && ErrorDTO.isError(result)) {
			logger.error(result.getString("error"));
		}
		return result;
	}

	private JsonObject handleSocketSystemMessage(Message<JsonObject> message) {
		JsonObject body = message.body();
		if (body != null && "disconnect".equals(body.getString("type"))) {
			playerDisconnected(body.getString("playerGuid"));
		}
		return AsyncHandlerDTO.getInstance();
	}

	private JsonObject addPlayer(JsonObject message) {
		String name = message.getString("name");

		/**
		 * TODO: Register a new player. Check if such player already does not exist (return an error in such case)
		 */
		Player player = new Player(escapeString(name), Entity.generateGUID());
		/**
		 * TODO: Player check succeeded, update internal state and send successful response ({@link AddPlayerDTO})
		 */
		return null;
	}

	private JsonObject addGame(Message<JsonObject> message) {
		JsonObject body = message.body();
		String name = body.getString("name");
		boolean exists = activeGames.containsKey(name);
		if (exists) {
			return new ErrorDTO("Game with this name already exists");
		}
		String playerGuid = body.getString("playerGuid");
		Player player = activePlayers.get(playerGuid);
		if (player == null) {
			return new ErrorDTO(ERROR_NO_SUCH_PLAYER);
		}
		String guid = Entity.generateGUID();
		Game game = new Game(escapeString(name), guid, player);
		activeGames.put(guid, game);
		//deploy and configure a new game verticle
		/**
		 * TODO: Create an instance of the {@link GameVerticle} that will handle this particular game.
		 * It will keep all game-related internal state, accept players' input, calculate movements and inform players about the new state
		 */

		/**
		 * TODO: Deploy the newly created instance, wait for the deployment to succeed and inform the client
		 * Use {@link AddGameDTO} for successful result
		 */
		joinableGame = game;
		return AsyncHandlerDTO.getInstance();
	}

	private JsonObject joinGame(Message<JsonObject> message) {
		JsonObject body = message.body();
		String playerGuid = body.getString("playerGuid");
		String gameGuid = body.getString("gameGuid");
		Player player = activePlayers.get(playerGuid);
		if (player == null) {
			return new ErrorDTO(ERROR_NO_SUCH_PLAYER);
		}
		Game game = activeGames.get(gameGuid);
		if (game == null) {
			return new ErrorDTO(ERROR_NO_SUCH_GAME);
		}
		if (game.isFull()) {
			return new ErrorDTO("Game is full");
		}
		game.addSecondPlayer(player);
		//send message to existing verticle that new player has joined

		/**
		 * TODO: Call the corresponding Game verticle and inform it that a new player has joined.
		 * Use the "microservice" API. Get a proxy instance, call its method passing a {@link io.vertx.core.Handler} as an argument
		 * Wait for that handler to complete (or fail) and signal the caller.
		 */

		return AsyncHandlerDTO.getInstance();
	}

	@Override
	public void onGameEnded(String gameGuid) {
		Game gameInMap = activeGames.get(gameGuid);
		if (gameInMap != null) {
			IntStream.rangeClosed(1, 2).forEach(i -> {
				playerDisconnected(gameInMap.getPlayer(i).getGuid());
			});
		}
		activeGames.remove(gameGuid);
		/**
		 * TODO: Undeploy a game's verticle as it has already ended.
		 * The verticle will be stopped and destroyed by the vert'x container, freeing system resources
		 */
	}

	private JsonObject getAvailableGame() {
		return new AvailableGameDTO(joinableGame);
	}

	private JsonObject listPlayers() {
		String[] strings = new String[activePlayersByName.size()];
		strings = activePlayersByName.keySet().toArray(strings);
		return new ListPlayersDTO(strings);
	}

	private void playerDisconnected(String playerGuid) {
		Player player = activePlayers.get(playerGuid);
		if (player == null) {
			return;
		}
		activePlayers.remove(playerGuid);
		activePlayersByName.remove(player.getName());
	}

	private String escapeString(String string) {
		try {
			return StringEscapeUtils.escapeJavaScript(string);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

