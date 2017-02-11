package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.Faction;
import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.Player;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class SwapCommand extends Command {

	public SwapCommand() {
		super("swap", "switch teams or swap two players", GameState.LOBBY,
				GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (bot.getLobby().getPlayer(event.getSender()) == null) {
			session.sendMessage(event.getChannel(),
					WatchWordBot.getUsernameString(event.getSender())
							+ ", you are currently not in this game!");
			return;
		}

		Player player = bot.getLobby().getPlayer(event.getSender());

		if (args.isEmpty()) {
			Faction currentFaction = bot.getLobby().getTurnOrder()
					.getFactionFor(player);
			Faction newFaction = bot.getLobby().getTurnOrder()
					.swapFactions(player);

			session.sendMessage(bot.getCurrentChannel(),
					WatchWordBot.getUsernameString(event.getSender())
							+ " swapped from " + currentFaction.getName()
							+ " to " + newFaction.getName() + ".");
		} else {
			SlackUser firstUser = null;
			SlackUser secondUser = null;
			if (args.size() == 2) {
				firstUser = bot.getLobby().findUserByUsername(args.pop());
				secondUser = bot.getLobby().findUserByUsername(args.pop());
			} else {
				bot.printUsage(
						event.getChannel(),
						"swap - swap yourself to the other team\nswap <player1> <player2> swap two players");
				return;
			}

			if (firstUser == null || secondUser == null) {
				session.sendMessage(event.getChannel(),
						"Could not find user(s) with those names.");
				return;
			}

			Player firstPlayer = bot.getLobby().getPlayer(firstUser);
			Player secondPlayer = bot.getLobby().getPlayer(secondUser);

			if (firstPlayer == null || secondPlayer == null) {
				session.sendMessage(event.getChannel(),
						"One or more of those users are not currently in the game.");
				return;
			}

			bot.getLobby().getTurnOrder()
					.swapPlayers(firstPlayer, secondPlayer);
			session.sendMessage(
					bot.getCurrentChannel(),
					WatchWordBot.getUsernameString(event.getSender())
							+ " has swapped "
							+ WatchWordBot.getUsernameString(firstUser)
							+ " with "
							+ WatchWordBot.getUsernameString(secondUser));
		}
		session.sendMessage(event.getChannel(), bot.printFactions());

	}
}
