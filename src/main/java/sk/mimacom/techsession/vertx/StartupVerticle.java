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

		/**
		 * TODO: Create and deploy instances of HttpServerVerticle that will listen for all incomming HTTP and websocket connections.
		 * This server will handle some low-level communication specifics and inform other verticles about related events.
		 *
		 * First we will initialize configuration JSON object for HttpServerVerticle, using these constants
		 * 		HTTPServerVerticle.CONFIG_ADDRESS,
		 		HTTPServerVerticle.CONFIG_PORT,
		 		HTTPServerVerticle.CONFIG_ALLOWED_ENDPOINTS_IN,
		 		HTTPServerVerticle.CONFIG_ALLOWED_ENDPOINTS_OUT
		 constants to pass configuration parameters
		 */

		/**
		 * TODO: When this configuration object is created, use vert.x APIs to deploy one {@link HTTPServerVerticle} for each CPU core.
		 *
		 * Use reactive (rxJava) APIs to be signalled when the deployment is finished, because we will need to synchronize this with waiting for other deployments
		 * to signal that our application is successfully started up.
		 */


		logger.info("Starting " + numInstances + " instances of Pong HTTP server at address " + httpAddress + " port:" + httpPort);
		logger.info("Allowed bridges for inbound eventbus endpoints: " + allowedEndpointsIn.toString());
		logger.info("Allowed bridges for outbound eventbus endpoints: " + allowedEndpointsOut.toString());

		/**
		 * TODO: Now create and deploy a single instance of game lobby verticle. There will be only one lobby,
		 * which will manage connected players and running games and put players through.
		 *
		 * This will be handled a litle bit differently than {@link HTTPServerVerticle}, because Lobby verticle uses new "microservice" api
		 */


		/**
		 * TODO: Now wait (in a non-blocking way) until all verticles have successfully been completed, or an error occured.
		 * Let the vert.x container know of the result, whether it should consider this app to be up and running, or failed.
		 *
		 *
		 * Also log the result to the standard logger.
		 * In case of error, use
		 * logger.error("An error has occured while starting Pong server", throwable);
		 *
		 * In case of success, use
		 * logger.info("Pong server successfully started");
		 *
		 * Don't forget to signal the startFuture!
		 *
		 */
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
