package sk.mimacom.techsession.vertx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

public class StartupVerticle extends AbstractVerticle {

	private Logger logger = LoggerFactory.getLogger(StartupVerticle.class);

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		String httpAddress = JsonParser.getString("http.address", context);
		final Integer httpPort = determineHttpPort();
		Integer numInstances = Runtime.getRuntime().availableProcessors();
		JsonArray allowedEndpointsIn = JsonParser.getArray("allowedEndpointsIn", context);
		JsonArray allowedEndpointsOut = JsonParser.getArray("allowedEndpointsOut", context);

		JsonObject configObject = new JsonObject();
		configObject.put(HTTPServerVerticle.CONFIG_ADDRESS, httpAddress);
		configObject.put(HTTPServerVerticle.CONFIG_PORT, httpPort);
		configObject.put(HTTPServerVerticle.CONFIG_ALLOWED_ENDPOINTS_IN, allowedEndpointsIn);
		configObject.put(HTTPServerVerticle.CONFIG_ALLOWED_ENDPOINTS_OUT, allowedEndpointsOut);

		DeploymentOptions httpServerdeploymentOptions = new DeploymentOptions()
				.setConfig(configObject)
				.setInstances(numInstances);

		logger.info("Starting " + numInstances + " instances of Pong HTTP server at address " + httpAddress + " port:" + httpPort);
		logger.info("Allowed bridges for inbound eventbus endpoints: " + allowedEndpointsIn.toString());
		logger.info("Allowed bridges for outbound eventbus endpoints: " + allowedEndpointsOut.toString());

		ObservableFuture<String> deployHttpServerFuture = RxHelper.observableFuture();
		ObservableFuture<String> deployGameLobbyFuture = RxHelper.observableFuture();

		GameLobbyVerticle gameLobbyVerticle = GameLobbyVerticle.create(EventbusAddresses.GAME_LOBBY_PRIVATE_QUEUE);

		vertx.deployVerticle((Verticle) gameLobbyVerticle, deployGameLobbyFuture.toHandler());
		vertx.deployVerticle(HTTPServerVerticle.class.getName(), httpServerdeploymentOptions, deployHttpServerFuture.toHandler());

		deployGameLobbyFuture
				.mergeWith(deployHttpServerFuture)
				.subscribe(x -> {
					//we don't care about deployment IDs
				}, throwable -> {
					logger.error("An error has occured while starting Pong server", throwable);
					startFuture.fail(throwable);
				}, () -> {
					logger.info("Pong server successfully started");
					startFuture.complete();
				});
	}

	private Integer determineHttpPort() {
		Integer cloudPort;
		try {
			cloudPort = Integer.parseInt(System.getenv("PORT"));
			logger.info("Got HTTP port definition from cloud provider, will listen on port " + cloudPort);
		} catch (NumberFormatException e) {
			cloudPort = JsonParser.getInteger("http.port", context);
			logger.info("Environment property PORT is not defined, will use http port from local config: " + cloudPort);
		}
		return cloudPort;
	}

	public static class EventbusAddresses {
		public static final String GAME_LOBBY_PRIVATE_QUEUE = "GameLobbyVerticle.private-queue";
	}

	// ------------- CODE FOR STARTING UP THE APPLICATION USING EMBEDDED VERT.X DIRECTLY FROM THE CONSOLE/IDE/JAR ----------------- //

	private static JsonObject loadConfigJson() {
		StringBuilder builder = new StringBuilder();
		try (InputStream stream = StartupVerticle.class.getClassLoader().getResourceAsStream("config.json")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			reader
					.lines()
					.forEach(builder::append);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new JsonObject(builder.toString());
	}

	public static void main(String[] args) {
		DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(loadConfigJson());
		Vertx.vertx().deployVerticle(StartupVerticle.class.getName(), deploymentOptions);
	}
}
