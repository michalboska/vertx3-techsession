package sk.mimacom.techsession.vertx.dto.game;

public class GameCommandDTO extends GameDTO {

    public GameCommandDTO() {
        setCommand("");
    }

    public void setCommand(String command) {
        this.put("command", command);
    }

    @Override
    public String getType() {
        return "command";
    }
}
