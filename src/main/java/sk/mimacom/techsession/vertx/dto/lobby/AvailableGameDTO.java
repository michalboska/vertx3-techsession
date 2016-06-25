package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.PongDTO;
import sk.mimacom.techsession.vertx.entity.Game;

/**
 * Created by Michal on 13. 12. 2014.
 */
public class AvailableGameDTO extends PongDTO {

    public AvailableGameDTO(Game game) {
        putString("guid", game != null ? game.getGuid() : null);
        putString("name", game != null ? game.getName() : null);
        setStatusOk();
    }

    @Override
    public String getType() {
        return "availableGame";
    }
}
