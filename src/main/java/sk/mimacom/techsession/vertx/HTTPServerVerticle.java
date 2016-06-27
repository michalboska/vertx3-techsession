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
		HttpServer httpServer = vertx.createHttpServer();
		SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
		setupSockjsBridge(sockJSHandler);

		Router router = Router.router(vertx);
		router.route("/eventbus/*").handler(sockJSHandler);
		router.routeWithRegex(HttpMethod.GET, "/(.*)").handler(this::handleHttpGet);
		httpServer.requestHandler(router::accept);

		httpServer.listen(Configuration.getInteger(CONFIG_PORT, context), Configuration.getString(CONFIG_ADDRESS, context));
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
		JsonArray allowedIn = Configuration.getArray(CONFIG_ALLOWED_ENDPOINTS_IN, context);
		JsonArray allowedOut = Configuration.getArray(CONFIG_ALLOWED_ENDPOINTS_OUT, context);
		extractPermittedOptions.apply(allowedIn).forEach(bridgeOptions::addInboundPermitted);
		extractPermittedOptions.apply(allowedOut).forEach(bridgeOptions::addOutboundPermitted);

		sockJSHandler.bridge(bridgeOptions, this::handleSockJSBridgeEvent);
	}

	private void handleHttpGet(RoutingContext routingContext) {
		String requestedPath = routingContext.pathParam("param0");
		String filePath = StringUtils.isBlank(requestedPath) ? "www/index.html" : "www/" + requestedPath;
		ObservableFuture<Boolean> fileExistsFuture = RxHelper.observableFuture();
		vertx.fileSystem().exists(filePath, fileExistsFuture.toHandler());
		HttpServerResponse response = routingContext.response();
		fileExistsFuture.subscribe(fileExists -> {
			if (fileExists && !filePath.contains("..")) {
				response.sendFile(filePath);
			} else {
				response.sendFile(NOT_FOUND_PATH);
			}
		}, throwable -> response.sendFile(NOT_FOUND_PATH));
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
		String address = sock.remoteAddress().toString();
		String guid = addressToPlayerGuidMap.get(address);
		if (guid == null) {
			//player with such remote address not found, nothing more to do
			return;
		}
		vertx.eventBus().publish(TOPIC_SOCKJS_MESSAGES, new PlayerDisconnectDTO(guid));
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
