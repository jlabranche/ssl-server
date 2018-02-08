package ethos.model.players.packets.commands.all;

import ethos.model.players.Player;
import ethos.model.players.packets.commands.Command;

import java.util.Optional;

/**
 * New Starter - Temp @Tyler
 */
public class Starter extends Command {

	@Override
	public void execute(Player c, String input) {
		c.getPA().addStarter();

	}

	@Override
	public Optional<String> getDescription() {
		return Optional.of("Gives you a starter after reset");
	}

}
