package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.PongDTO;

/**
 * Created by Michal on 29. 12. 2014.
 */
public class PlayerDisconnectDTO extends PongDTO {

    public PlayerDisconnectDTO(String playerGuid) {
        putString("playerGuid", playerGuid);
    }

    @Override
    public String getType() {
        return "disconnect";
    }
}
