package test;

import ethos.Config;
import ethos.model.players.Player;
import ethos.model.players.PlayerSave;
import ethos.model.players.combat.Hitmark;
import java.io.IOException;

public class playerCombat {

	public static void main(String[] args) throws IOException {
        //setup user
        Config c = new Config();
	    Player p = new Player(0, "a", null);
	    PlayerSave ps = new PlayerSave();
        p.playerName = c.testUser;
        p.playerPass = c.testPass;
        ps.loadGame(p, c.testUser, c.testPass);
        p.cmdLine = 1;


        //tests
        p.sendMessage("Player Name: " + p.playerName);
        p.sendMessage("HP: "+p.getHealth().getAmount());
        p.appendDamage(3, Hitmark.POISON);
        p.sendMessage("HP: "+p.getHealth().getAmount());
        p.logout();
        System.exit(0);
//        ps.saveGame
    }
}
