package sk.mimacom.techsession.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import sk.mimacom.techsession.vertx.dto.AsyncHandlerDTO;
import sk.mimacom.techsession.vertx.dto.ErrorDTO;

import java.util.function.Function;

public abstract class PongVerticle extends AbstractVerticle {

    protected Handler<Message<JsonObject>> createHandler(Function<Message<JsonObject>, JsonObject> handlerFunction) {
        return msg -> {
            JsonObject result = handlerFunction.apply(msg);
            if (result == null) {
                unknownHandlerError(msg);
            } else if (!(result instanceof AsyncHandlerDTO)) {
                msg.reply(result);
            }
            //else, if the result is an instance of AsyncHandlerDTO, do nothing, as the handlerFunction will handle
            //the response itself
        };
    }

    protected void unknownHandlerError(Message msg) {
        msg.reply(new ErrorDTO("No handler registered for this message type"));
    }

}
