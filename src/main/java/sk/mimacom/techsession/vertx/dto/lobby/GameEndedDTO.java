package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.PongDTO;

public class GameEndedDTO extends PongDTO {

    public GameEndedDTO(String gameGuid) {
        put("guid", gameGuid);
    }

    @Override
    public String getType() {
        return "gameEnded";
    }
}
