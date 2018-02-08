package ethos.model.players.packets.commands.all;

import ethos.model.players.Player;
import ethos.model.players.packets.commands.Command;

import java.util.Optional;

/**
 * Opens the highscores in the default web browser.
 * 
 * @author Emiel
 */
public class Highscores extends Command {

	@Override
	public void execute(Player c, String input) {
		c.getPA().sendFrame126("https://exoticrs.com/hiscores/", 12000);
	}

	@Override
	public Optional<String> getDescription() {
		return Optional.of("Opens a webpage with the highscores");
	}

}
