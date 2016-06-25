package sk.mimacom.techsession.vertx.dto;

import io.vertx.core.json.JsonObject;

public class ErrorDTO extends PongDTO {

    public static boolean isError(JsonObject jsonObject) {
        return jsonObject.containsKey("error") && jsonObject.getString("error") != null;
    }

    public ErrorDTO(String message) {
        put("message", message);
    }

    public ErrorDTO(Throwable throwable) {
        put("message", throwable.getMessage());
    }

    @Override
    public String getType() {
        return "error";
    }
}
