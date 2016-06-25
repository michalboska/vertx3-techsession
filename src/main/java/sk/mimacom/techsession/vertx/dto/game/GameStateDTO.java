package sk.mimacom.techsession.vertx.dto.game;

public class GameStateDTO extends GameDTO {

    public GameStateDTO() {
        setBallPosition(0, 0);
        setPlayer1position(0);
        setPlayer2position(0);
        setPlayer1score(0);
        setPlayer2score(0);
    }

    @Override
    public String getType() {
        return "state";
    }

    public void setPlayer1position(int y) {
        put("player1pos", y);
    }

    public void setPlayer2position(int y) {
        put("player2pos", y);
    }

    public void setPlayer1score(int score) {
        put("player1score", score);
    }

    public void setPlayer2score(int score) {
        put("player2score", score);
    }

    public void setBallPosition(int x, int y) {
        put("ballx", x);
        put("bally", y);
    }
}
