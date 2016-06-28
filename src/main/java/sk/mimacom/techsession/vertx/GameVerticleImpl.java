package sk.mimacom.techsession.vertx;

import org.apache.commons.lang3.Validate;

import java.util.stream.IntStream;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.serviceproxy.ProxyHelper;
import sk.mimacom.techsession.vertx.dto.AsyncHandlerDTO;
import sk.mimacom.techsession.vertx.dto.game.GameCommandDTO;
import sk.mimacom.techsession.vertx.dto.game.GameStateDTO;
import sk.mimacom.techsession.vertx.entity.Player;

class GameVerticleImpl extends PongVerticle implements GameVerticle {

	private Logger logger = LoggerFactory.getLogger(GameVerticleImpl.class);

	private GameLobbyVerticle gameLobbyVerticle;

	private Player[] players = new Player[2];
	private String gameGuid, publicAddress, privateAddress, inputAddress;
	private Point ball = new Point(512, 300);
	private float ballSpeed = 10;
	private Point ballVector = new Point(-1, -1);
	//we don't want computeNewBallCoordinates to always return new instance to elliminate GC overhead,
	// so we will always modify the same one
	private Point newBallCoordinates = new Point();
	private JsonObject playerMoveResponse = new JsonObject();
	private GameStateDTO state = new GameStateDTO();
	private GameCommandDTO command = new GameCommandDTO();
	private int disconnectedPlayerIndex = -1;

	private byte speedCounter = 0;
	private long gameTimer;

	public GameVerticleImpl(String gameGuid, String firstPlayerGuid, String firstPlayerName) {
		this.gameGuid = gameGuid;
		players[0] = new Player(Validate.notNull(firstPlayerName), Validate.notNull(firstPlayerGuid));
		publicAddress = Constants.getPublicQueueAddressForGame(gameGuid);
		privateAddress = Constants.getPrivateQueueAddressForGame(gameGuid);
		inputAddress = Constants.getInputQueueAddressForGame(gameGuid);
	}

	@Override
	public void start() {
		String privateQueueAddress = Constants.getPrivateQueueAddressForGame(gameGuid);
		ProxyHelper.registerService(GameVerticle.class, getVertx(), this, privateQueueAddress);
		gameLobbyVerticle = GameLobbyVerticle.createProxy(getVertx(), StartupVerticle.EventbusAddresses.GAME_LOBBY_PRIVATE_QUEUE);
		vertx.eventBus().consumer(inputAddress, createHandler(this::handleInputMessages));

		vertx.eventBus().consumer(HTTPServerVerticle.TOPIC_SOCKJS_MESSAGES, createHandler(this::handleSockJsMessages));
	}

	private JsonObject handleInputMessages(Message<JsonObject> message) {
		JsonObject result = null;
		JsonObject body = message.body();
		switch (body.getString("type")) {
			case "move":
				result = movePlayer(body);
				break;
		}
		return result;
	}

	private JsonObject handleSockJsMessages(Message<JsonObject> message) {
		JsonObject body = message.body();
		if (body != null && body.getString("type").equals("disconnect")) {
			playerDisconnected(body.getString("playerGuid"));
		}
		return AsyncHandlerDTO.getInstance();
	}

	private void gameTick(long timerID) {
		move();
		populateGameState();
		vertx.eventBus().publish(publicAddress, state);
		int winning = getWinningPlayer();
		if (winning != -1) { //if we have a winner, end game
			command.setCommand("win" + (winning + 1));
			vertx.cancelTimer(timerID);
			vertx.eventBus().publish(publicAddress, command);
			gameLobbyVerticle.onGameEnded(gameGuid);
		}
	}

	private void move() {
		computeNewBallCoordinates();
		if (newBallCoordinates.getY() < 20) {
			ballVector.setY(1);
			computeNewBallCoordinates();
		} else if (newBallCoordinates.getY() > 560) {
			ballVector.setY(-1);
			computeNewBallCoordinates();
		}
		if (newBallCoordinates.getX() < 20) { //ball is at player1's level
			if (ballCollidesWith(0)) { //player has caught the ball
				ballVector.setX(1);
				decideSpeed();
				computeNewBallCoordinates();
			} else { //missed the ball
				players[1].setScore(players[1].getScore() + 1);
				resetBall();
				computeNewBallCoordinates();
			}
		} else if (newBallCoordinates.getX() > 994) { //ball is at player2's level
			if (ballCollidesWith(1)) {
				ballVector.setX(-1);
				decideSpeed();
				computeNewBallCoordinates();
			} else { //missed the ball
				players[0].setScore(players[0].getScore() + 1);
				resetBall();
				computeNewBallCoordinates();
			}
		}
		ball.set(newBallCoordinates);
	}

	private void decideSpeed() {
		speedCounter++;
		if (speedCounter == 5) {
			ballSpeed *= 1.3;
			speedCounter = 0;
		}
	}

	private JsonObject movePlayer(JsonObject message) {
		Player player = null;
		String guid = message.getString("guid");
		for (Player p : players) {
			if (p != null && p.getGuid().equalsIgnoreCase(guid)) {
				player = p;
			}
		}
		playerMoveResponse.put("y", 0);
		if (player == null) {
			return playerMoveResponse;
		}
		playerMoveResponse.put("y", player.getPosition());
		Integer newPosition = message.getInteger("y");
		if (newPosition == null) {
			return playerMoveResponse;
		}
		int value = Math.round(newPosition.floatValue());
		if (value >= 20 && value <= 480 && Math.abs(value - player.getPosition()) <= 10) {
			player.setPosition(value);
			populateGameState();
			vertx.eventBus().publish(publicAddress, state);
		}
		return playerMoveResponse;
	}

	/**
	 * @return index of winning player (0 or 1) or -1 if no one is winning
	 */
	private int getWinningPlayer() {
		if (disconnectedPlayerIndex > -1) {
			return 1 - disconnectedPlayerIndex; //if i = 1, return 0 and vice-versa
		}
		for (int i = 0; i <= 1; i++) {
			if (players[i].getScore() >= 10) {
				return i;
			}
		}
		return -1;
	}

	private void populateGameState() {
		state.setBallPosition(ball.getX(), ball.getY());
		state.setPlayer1position(players[0].getPosition());
		state.setPlayer1score(players[0].getScore());
		if (players[1] != null) {
			state.setPlayer2position(players[1].getPosition());
			state.setPlayer2score(players[1].getScore());
		}
	}

	/**
	 * Only Y coordinate is taken into account
	 */
	private boolean ballCollidesWith(int playerIndex) {
		Player player = players[playerIndex];
		int playerYLow = player.getPosition();
		int playerYHigh = playerYLow + 100;
		int ballYLow = newBallCoordinates.getY();
		int ballYHigh = ballYLow + 20;
		return ballYHigh >= playerYLow && ballYLow <= playerYHigh;
	}

	private void computeNewBallCoordinates() {
		newBallCoordinates.setX(ball.getX() + Math.round(ballSpeed * ballVector.getX()));
		newBallCoordinates.setY(ball.getY() + Math.round(ballSpeed * ballVector.getY()));
	}

	private void resetBall() {
		ball.setX(500);
		ball.setY(400);
		ballVector = new Point(-1, -1);
		ballSpeed = 5;
		speedCounter = 0;
	}

	private void startGame() {
		logger.info(String.format("Game %s is starting", gameGuid));
		if (gameTimer != 0) {
			vertx.cancelTimer(gameTimer);
		}
		resetBall();
		players[0].setPosition(230);
		players[1].setPosition(230);
		players[0].setScore(0);
		players[1].setScore(0);
		populateGameState();
		command.setCommand("start");
		vertx.eventBus().publish(publicAddress, command);
		gameTimer = vertx.setPeriodic(20, this::gameTick);
	}

	@Override
	public void addPlayer(String playerGuid, String playerName, Handler<AsyncResult<Void>> handler) {
		Future<Void> handlerFuture = Future.<Void>future().setHandler(handler);
		if (players[1] != null) {
			handlerFuture.fail("Game is full");
			return;
		}
		players[1] = new Player(playerName, playerGuid);
		vertx.setTimer(2000, l -> startGame());
		handlerFuture.complete();
	}

	private void playerDisconnected(String playerGuid) {
		IntStream.rangeClosed(0, 1).forEach(i -> {
			if (players[i] != null && players[i].getGuid().equals(playerGuid)) {
				disconnectedPlayerIndex = i;
			}
		});
	}

	static class Constants {
		static final String QUEUE_PUBLIC_PREFIX = "Game.public-";
		static final String QUEUE_PRIVATE_PREFIX = "Game.private-";
		static final String QUEUE_INPUT_PREFIX = "Game.input-";

		static final String CONFIG_GAME_GUID = "gameGuid";
		static final String CONFIG_PLAYER_GUID = "playerGuid";
		static final String CONFIG_PLAYER_NAME = "playerName";

		public static final String ACTION_ADD_PLAYER = "addPlayer";

		static String getPublicQueueAddressForGame(String guid) {
			return QUEUE_PUBLIC_PREFIX + guid;
		}

		static String getPrivateQueueAddressForGame(String guid) {
			return QUEUE_PRIVATE_PREFIX + guid;
		}

		static String getInputQueueAddressForGame(String guid) {
			return QUEUE_INPUT_PREFIX + guid;
		}
	}
}
