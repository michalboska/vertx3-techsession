package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.PongDTO;

public class PlayerDisconnectDTO extends PongDTO {

    public PlayerDisconnectDTO(String playerGuid) {
        put("playerGuid", playerGuid);
    }

    @Override
    public String getType() {
        return "disconnect";
    }
}
