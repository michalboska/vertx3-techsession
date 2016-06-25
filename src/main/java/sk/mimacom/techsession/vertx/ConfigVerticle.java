package sk.mimacom.techsession.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

public class ConfigVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(ConfigVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        String httpAddress = Configuration.getString("http.address", context);
        Integer cloudPort;
        try {
            cloudPort = Integer.parseInt(System.getenv("PORT"));
            logger.info("Got HTTP port definition from cloud provider, will listen on port " + cloudPort);
        } catch (NumberFormatException e) {
            cloudPort = Configuration.getInteger("http.port", context);
            logger.info("Environment property PORT is not defined, will use http port from local config: " + cloudPort);
        }
        final Integer httpPort = cloudPort; //we need a final variable to use in lambda
        Integer numInstances = Runtime.getRuntime().availableProcessors();
        JsonArray allowedEndpointsIn = Configuration.getArray("allowedEndpointsIn", context);
        JsonArray allowedEndpointsOut = Configuration.getArray("allowedEndpointsOut", context);

        JsonObject configObject = new JsonObject();
        configObject.put(HTTPServerVerticle.CONFIG_ADDRESS, httpAddress);
        configObject.put(HTTPServerVerticle.CONFIG_PORT, httpPort);
        configObject.put(HTTPServerVerticle.CONFIG_ALLOWED_ENDPOINTS_IN, allowedEndpointsIn);
        configObject.put(HTTPServerVerticle.CONFIG_ALLOWED_ENDPOINTS_OUT, allowedEndpointsOut);

        DeploymentOptions httpServerdeploymentOptions = new DeploymentOptions().setConfig(configObject);

        logger.info("Starting " + numInstances + " instances of Pong HTTP server at address " + httpAddress + " port:" + httpPort);
        logger.info("Allowed bridges for inbound eventbus endpoints: " + allowedEndpointsIn.toString());
        logger.info("Allowed bridges for outbound eventbus endpoints: " + allowedEndpointsOut.toString());

        ObservableFuture<String> deployHttpServerFuture = RxHelper.observableFuture();
        ObservableFuture<String> deployGameLobbyFuture = RxHelper.observableFuture();

        vertx.deployVerticle(HTTPServerVerticle.class.getName(), httpServerdeploymentOptions, deployHttpServerFuture.toHandler());
        vertx.deployVerticle(GameLobbyVerticleImpl.class.getName(), deployGameLobbyFuture.toHandler());

        deployHttpServerFuture
                .mergeWith(deployGameLobbyFuture)
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

}
