package sk.mimacom.techsession.vertx.dto;

import io.vertx.core.json.JsonObject;

public abstract class PongDTO extends JsonObject {

    public PongDTO() {
        put("type", getType());
    }

    protected void setStatusOk() {
        put("status", "ok");
    }

    public abstract String getType();
}
