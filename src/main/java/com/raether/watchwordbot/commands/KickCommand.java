package com.raether.watchwordbot.commands;

import java.util.Arrays;
import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class KickCommand extends Command {

	public KickCommand() {
		super("kick", Arrays.asList("remove"),
				"removes the specified player from the game.", false,
				GameState.LOBBY, GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (args.isEmpty()) {
			bot.printUsage(event.getChannel(),
					"[kick|remove] <player1, player2, ...>");
			return;
		}
		while (!args.isEmpty()) {
			String username = args.pop();
			SlackUser user = bot.getLobby().findUserByUsername(username);
			if (user != null) {
				bot.getLobby().removeUser(user);
				session.sendMessage(bot.getCurrentChannel(),
						event.getSender().getUserName() + " removed "
								+ WatchWordBot.getUsernameString(user));
			} else {
				session.sendMessage(bot.getCurrentChannel(),
						"Could not find user already in game with username '"
								+ username + "'.");
			}
		}
		session.sendMessage(bot.getLobby().getChannel(), bot.printFactions());
		bot.printMatchQuality();
	}
}
