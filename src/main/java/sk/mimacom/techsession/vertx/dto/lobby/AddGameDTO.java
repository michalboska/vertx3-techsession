package sk.mimacom.techsession.vertx.dto.lobby;

import sk.mimacom.techsession.vertx.dto.EntityCreatedDTO;

/**
 * Created by bol on 8. 12. 2014.
 */
public class AddGameDTO extends EntityCreatedDTO {

    public AddGameDTO(String guid) {
        super(guid);
    }

    @Override
    public String getType() {
        return "addGame";
    }
}
