package sk.mimacom.techsession.vertx;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import sk.mimacom.techsession.vertx.dto.lobby.PlayerDisconnectDTO;

public class HTTPServerVerticle extends AbstractVerticle {
	public static final String CONFIG_PORT = "port";
	public static final String CONFIG_ADDRESS = "address";
	public static final String CONFIG_ALLOWED_ENDPOINTS_IN = "allowedEndpointsIn";
	public static final String CONFIG_ALLOWED_ENDPOINTS_OUT = "allowedEndpointsOut";
	public static final String TOPIC_SOCKJS_MESSAGES = "HTTPServerVerticle.topic.sockjs";

	private static final String NOT_FOUND_PATH = "www/404.html";

	private Logger logger = LoggerFactory.getLogger(HTTPServerVerticle.class);
	private SockJSHandler sockJSHandler;
	private Map<String, String> addressToPlayerGuidMap = new HashMap<>();

	@Override
	public void start() throws Exception {

		//TODO: Note that we are using the start() version without the future

		/**
		 * TODO: Create an instance of Vert.x's HTTP server and SockJS handler.
		 * Then call the setupSockjsBridge method to configure the EventBus <-> SockJS bridge
		 */

		/**
		 * TODO: Now create an instance of {@link Router} object. Configure it to handle GET requests to our HTTP server
		 *
		 * Let the special URL pattern "/eventbus/*" be handled by the sockJS handler,
		 * all other URLs should be handled by the handleHttpGet method.
		 * Be sure to capture the request path in a regex group, so that we can extract the required file path later.
		 *
		 * Finally, set the configured router to be the http server's request handler.
		 */

		/**
		 * TODO: Start the http server using its listen() method. Use address and port supplied by the configuration.
		 */

	}

	private void setupSockjsBridge(SockJSHandler sockJSHandler) {
		BridgeOptions bridgeOptions = new BridgeOptions();
		Function<JsonArray, List<PermittedOptions>> extractPermittedOptions = (JsonArray array) -> array
				.stream()
				.map(o -> {
					JsonObject jsonObject = (JsonObject) o;
					return new PermittedOptions()
							.setAddress(jsonObject.getString("address"))
							.setAddressRegex(jsonObject.getString("address_re"));
				})
				.collect(Collectors.toList());
		JsonArray allowedIn = JsonParser.getArray(CONFIG_ALLOWED_ENDPOINTS_IN, context);
		JsonArray allowedOut = JsonParser.getArray(CONFIG_ALLOWED_ENDPOINTS_OUT, context);
		extractPermittedOptions.apply(allowedIn).forEach(bridgeOptions::addInboundPermitted);
		extractPermittedOptions.apply(allowedOut).forEach(bridgeOptions::addOutboundPermitted);

		sockJSHandler.bridge(bridgeOptions, this::handleSockJSBridgeEvent);
	}

	private void handleHttpGet(RoutingContext routingContext) {
		/**
		 * TODO: Extract the requested file from request, fill the default one (if blank).
		 *
		 * Then check if the requested file exists and finally serve the file. Don't forget to check for .. path traversal
		 */
	}

	private void handleSockJSBridgeEvent(BridgeEvent event) {
		SockJSSocket socket = event.socket();
		switch (event.type()) {
			case SOCKET_CLOSED:
				this.handleSocketClosed(socket);
				break;
			case SEND:
			case PUBLISH:
				JsonObject eventEnvelope = event.getRawMessage();
				String address = eventEnvelope.getString("address");
				JsonObject body = eventEnvelope.getJsonObject("body");
				event.complete(handleSendOrPublish(socket, body, address));
				return;
		}
		event.complete(true);
	}

	private void handleSocketClosed(SockJSSocket sock) {
		/**
		 * TODO: Get the remote address for the socket being closed. Check if we have a player registered for that socket.
		 * If yes, broadcast the event, so that if a game including that player is running,
		 * we let it know that this player is an ugly leaver and we should declare his opponent victorious
		 *
		 * Note: the topic has address: TOPIC_SOCKJS_MESSAGES
		 */
	}


	private boolean handleSendOrPublish(SockJSSocket sock, JsonObject body, String address) {
		//we want to intercept only Lobby messages to listen for Create-Game or Join-Game message
		//These messages contain player GUID, so we can pair player GUID with the socket's remote address
		if (!address.equals(GameLobbyVerticleImpl.QUEUE_LOBBY)) {
			return true;
		}

		if (body == null) {
			return true;
		}
		String type = body.getString("type");
		if (!"addGame".equals(type) && !"joinGame".equals(type)) {
			return true;
		}
		String playerGuid = body.getString("playerGuid");
		if (playerGuid == null) {
			return true;
		}
		String remoteAddress = sock.remoteAddress().toString();
		addressToPlayerGuidMap.put(remoteAddress, playerGuid);
		return true;
	}

}
