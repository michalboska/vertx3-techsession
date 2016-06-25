package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.PongDTO;

/**
 * Created by bol on 8. 12. 2014.
 */
public class JoinGameDTO extends PongDTO {

    public JoinGameDTO() {
        setStatusOk();
    }

    @Override
    public String getType() {
        return "joinGame";
    }
}
