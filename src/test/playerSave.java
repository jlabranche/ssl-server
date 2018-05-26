package test;

import ethos.Config;
import ethos.model.players.Player;
import ethos.model.players.PlayerSave;
import java.io.IOException;

public class playerSave {

	public static void main(String[] args) throws IOException {
        //setup user
        Config c = new Config();
	    Player p = new Player(0, "a", null);
	    PlayerSave ps = new PlayerSave();
        p.playerName = c.testUser;
        p.playerName2 = c.testUser + "2";
        p.playerPass = c.testPass;
        ps.loadGame(p, c.testUser, c.testPass);
        p.cmdLine = 1;

        //test
        p.saveFile = true;
        p.saveCharacter = true;
        p.sendMessage("Player " + p.playerName + " saving...");
        ps.saveGame(p);
        p.sendMessage("Player " + p.playerName + " has been saved.");
        ps.loadGame(p, c.testUser, c.testPass);
        System.exit(0);
    }
}
