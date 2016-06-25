package sk.mimacom.techsession.vertx;

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
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        router.route(HttpMethod.GET, "/:file").handler(this::handleHttpGet);
        router.route("/eventbus/*").handler(sockJSHandler);
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
        final String notFoundPath = NOT_FOUND_PATH;
        String filePath = routingContext.pathParam("file");
        ObservableFuture<Boolean> fileExistsFuture = RxHelper.observableFuture();
        vertx.fileSystem().exists(filePath, fileExistsFuture.toHandler());
        HttpServerResponse response = routingContext.response();
        fileExistsFuture.subscribe(fileExists -> {
            if (fileExists && !filePath.contains("..")) {
                response.sendFile("www/" + filePath);
            } else {
                response.sendFile(notFoundPath);
            }
        }, throwable -> response.sendFile(notFoundPath));
    }

    private void handleSockJSBridgeEvent(BridgeEvent event) {


    }

//    public boolean handleSocketCreated(SockJSSocket sock) {
//        return true;
//    }
//
//    @Override
//    public void handleSocketClosed(SockJSSocket sock) {
//        String address = sock.remoteAddress().toString();
//        String guid = addressToPlayerGuidMap.get(address);
//        if (guid == null) {
//            //player with such remote address not found, nothing more to do
//            return;
//        }
//        vertx.eventBus().publish(TOPIC_SOCKJS_MESSAGES, new PlayerDisconnectDTO(guid));
//    }
//
//    @Override
//    public boolean handleSendOrPub(SockJSSocket sock, boolean send, JsonObject msg, String address) {
//        //we want to intercept only Lobby messages to listen for Create-Game or Join-Game message
//        //These messages contain player GUID, so we can pair player GUID with the socket's remote address
//        if (!address.equals(GameLobbyVerticleImpl.QUEUE_LOBBY)) {
//            return true;
//        }
//        JsonObject body = msg.getObject("body");
//        if (body == null) {
//            return true;
//        }
//        String type = body.getString("type");
//        if (!"addGame".equals(type) && !"joinGame".equals(type)) {
//            return true;
//        }
//        String playerGuid = body.getString("playerGuid");
//        if (playerGuid == null) {
//            return true;
//        }
//        addressToPlayerGuidMap.put(sock.remoteAddress().toString(), playerGuid);
//        return true;
//    }
//
//    @Override
//    public boolean handlePreRegister(SockJSSocket sock, String address) {
//        return true;
//    }
//
//    @Override
//    public void handlePostRegister(SockJSSocket sock, String address) {
//    }
//
//    @Override
//    public boolean handleUnregister(SockJSSocket sock, String address) {
//        return true;
//    }
//
//    @Override
//    public boolean handleAuthorise(JsonObject message, String sessionID, Handler<AsyncResult<Boolean>> handler) {
//        return true;
//    }
}
