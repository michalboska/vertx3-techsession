package sk.mimacom.techsession.vertx;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.Message;
import sk.mimacom.techsession.vertx.dto.AsyncHandlerDTO;
import sk.mimacom.techsession.vertx.dto.ErrorDTO;

import java.util.function.Function;

abstract class PongVerticle extends AbstractVerticle {

	Handler<Message<JsonObject>> createHandler(Function<Message<JsonObject>, JsonObject> handlerFunction) {

		/**
		 * TODO: This should return a function that takes a funstion of Message as an argument and:
		 * 		- if that function returns null, return error (unknownHandlerError(...))
		 * 		- if it returns {@link AsyncHandlerDTO} instance, do nothing, as that function will take care of replying itself
		 * 		- otherwise send the value returned by that function as a reply to the message
		 */
		return msg -> {
		};
	}

	private void unknownHandlerError(Message msg) {
		msg.reply(new ErrorDTO("No handler registered for this message type"));
	}

}
