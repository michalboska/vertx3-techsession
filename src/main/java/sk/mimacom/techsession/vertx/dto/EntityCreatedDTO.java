package sk.mimacom.techsession.vertx.dto;

/**
 * Created by bol on 8. 12. 2014.
 */
public abstract class EntityCreatedDTO extends PongDTO {

    public EntityCreatedDTO(String guid) {
        putString("guid", guid);
        setStatusOk();
    }
}
