package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.PongDTO;
import sk.mimacom.techsession.vertx.entity.Game;

public class AvailableGameDTO extends PongDTO {

    public AvailableGameDTO(Game game) {
        put("guid", game != null ? game.getGuid() : null);
        put("name", game != null ? game.getName() : null);
        setStatusOk();
    }

    @Override
    public String getType() {
        return "availableGame";
    }
}
