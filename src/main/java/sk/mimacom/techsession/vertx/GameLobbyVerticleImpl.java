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
		serviceProxyConsumer = ProxyHelper.registerService(GameLobbyVerticle.class, getVertx(), this, privateQueueAddress);
		vertx.eventBus().consumer(QUEUE_LOBBY, createHandler(this::handleMessageFromPlayer));
		vertx.eventBus().consumer(HTTPServerVerticle.TOPIC_SOCKJS_MESSAGES, createHandler(this::handleSocketSystemMessage));
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
		boolean exists = activePlayersByName.containsKey(name);
		if (exists) {
			return new ErrorDTO("Player name already exists");
		}
		Player player;
		player = new Player(escapeString(name), Entity.generateGUID());
		activePlayers.put(player.getGuid(), player);
		activePlayersByName.put(player.getName(), player);
		return new AddPlayerDTO(player.getGuid());
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
		GameVerticle gameVerticle = GameVerticle.create(guid, playerGuid, player.getName());
		ObservableFuture<String> deployGameVerticleFuture = RxHelper.observableFuture();
		getVertx().deployVerticle((Verticle) gameVerticle, deployGameVerticleFuture.toHandler());
		deployGameVerticleFuture.subscribe(deploymentId -> {
			deploymentIDs.put(guid, deploymentId);
			logger.info(String.format("Deployed a new verticle for game %s with deployment ID: %s", guid, deploymentId));
			message.reply(new AddGameDTO(guid));
		}, throwable -> {
			message.reply(new ErrorDTO(throwable));
		});
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

		ObservableFuture<Void> addPlayerToGameFuture = RxHelper.observableFuture();
		GameVerticle gameVerticle = GameVerticle.createProxy(getVertx(), gameGuid);
		gameVerticle.addPlayer(playerGuid, player.getName(), addPlayerToGameFuture.toHandler());

		addPlayerToGameFuture.subscribe((x) -> {
		}, throwable -> {
			message.reply(new ErrorDTO(throwable));
		}, () -> {
			joinableGame = null;
			message.reply(new JoinGameDTO());
		});
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
		String id = deploymentIDs.get(gameGuid);
		if (id != null) {
			logger.info(String.format("Destroying verticle for game %s", gameGuid));
			deploymentIDs.remove(gameGuid);
			vertx.undeploy(id);
		}
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

