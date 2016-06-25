package sk.mimacom.techsession.vertx.dto.lobby;

import io.vertx.core.json.JsonArray;
import sk.mimacom.techsession.vertx.dto.PongDTO;

import java.util.Arrays;

public class ListPlayersDTO extends PongDTO {

    public ListPlayersDTO(String[] playerNames) {
        put("players", new JsonArray(Arrays.asList(playerNames)));
        setStatusOk();
    }

    @Override
    public String getType() {
        return "listPlayers";
    }
}
