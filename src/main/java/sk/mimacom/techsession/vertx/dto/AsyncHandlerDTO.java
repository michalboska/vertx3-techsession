package sk.mimacom.techsession.vertx.dto;

import io.vertx.core.json.JsonObject;

public class AsyncHandlerDTO extends JsonObject {

    public static AsyncHandlerDTO getInstance() {
        return _instance;
    }

    private static final AsyncHandlerDTO _instance = new AsyncHandlerDTO();

    private AsyncHandlerDTO() {
    }
}
