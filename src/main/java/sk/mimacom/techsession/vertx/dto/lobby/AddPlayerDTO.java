package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.EntityCreatedDTO;

/**
 * Created by Michal on 6. 12. 2014.
 */
public class AddPlayerDTO extends EntityCreatedDTO {

    public AddPlayerDTO(String guid) {
        super(guid);
    }
    @Override
    public String getType() {
        return "addPlayer";
    }
}
