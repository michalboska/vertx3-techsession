package sk.mimacom.techsession.vertx.dto;

public abstract class EntityCreatedDTO extends PongDTO {

    public EntityCreatedDTO(String guid) {
        put("guid", guid);
        setStatusOk();
    }
}
