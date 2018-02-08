package ethos.model.players.packets.commands.all;

import java.util.Optional;

import ethos.model.players.Player;
import ethos.model.players.packets.commands.Command;

public class Site extends Command {

	@Override
	public void execute(Player c, String input) {
		String[] args = input.split(" ");
		
		switch (args[0]) {
		
		case "":
			c.sendMessage("Usage: ::site forums");
			break;
		case "home":
			c.getPA().sendFrame126("https://exoticrs.com", 12000);
			break;
		case "forums":
			c.getPA().sendFrame126("https://exoticrs.com/community/index.php", 12000);
			break;
		case "discord":
			c.getPA().sendString("https://discord.gg/wqMpNB", 12000);
			break;	
		case "donate":
			c.getPA().sendFrame126("https://exoticrs.com/store", 12000);
			break;
		case "highscores":
			c.getPA().sendFrame126("https://exoticrs.com/hiscores", 12000);
			break;
		case "vote":
			c.getPA().sendString("http://exoticrs.com/vote", 12000);
			break;
		case "news":
			c.getPA().sendString("https://www.exoticrs.com/community/index.php?forums/news-announcements.2/", 12000);
			break;
		case "updates":
			c.getPA().sendString("https://www.exoticrs.com/community/index.php?forums/updates.4/", 12000);
			break;
		case "knowledge":
			c.getPA().sendString("", 12000);
			break;
		case "apply":
			c.getPA().sendString("https://www.exoticrs.com/community/index.php?forums/staff-applications.16/", 12000);
			break;
		case "events":
			c.getPA().sendString("https://www.exoticrs.com/community/index.php?forums/server-events.3/", 12000);
			break;
		case "guides":
			c.getPA().sendString("https://www.exoticrs.com/community/index.php?forums/guides.14/", 12000);
			break;
		case "media":
			c.getPA().sendString("https://www.exoticrs.com/community/index.php?forums/media.31/", 12000);
			break;
		case "goals":
			c.getPA().sendString("", 12000);
			break;
		case "market":
			c.getPA().sendString("", 12000);
			break;
		case "clans":
			c.getPA().sendString("", 12000);
			break;
		case "help":
			c.getPA().sendString("", 12000);
			break;
		case "suggest":
			c.getPA().sendString("", 12000);
			break;
		case "poll":
			c.getPA().sendString("", 12000);
			break;
		case "maps":
			c.getPA().sendString("", 12000);
			break;
		}
	}
	@Override
	public Optional<String> getDescription() {
		return Optional.of("You can visit all our different sites using this command");
	}
}
