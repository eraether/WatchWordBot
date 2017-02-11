package com.raether.watchwordbot.commands;

import java.util.Arrays;
import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.Player;
import com.raether.watchwordbot.WatchWordBot;
import com.raether.watchwordbot.meatsim.AIPlayer;
import com.raether.watchwordbot.meatsim.AISlackPlayer;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class BotCommand extends Command {

	public BotCommand() {
		super("bot", Arrays.asList("bots"), "add a bot to the game", false,
				GameState.LOBBY, GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {

		int numberOfBots = 1;
		if (!args.isEmpty()) {
			try {
				numberOfBots = Integer.parseInt(args.pop());
				if (numberOfBots <= 0) {
					throw new IllegalArgumentException(
							"Number of bots must be > 0");
				}
			} catch (Exception e) {
				bot.printUsage(event.getChannel(), "bot [# of desired bots]");
				return;
			}
		}
		boolean addedBots = false;
		for (int addedBotCount = 0; addedBotCount < numberOfBots; addedBotCount++) {
			int maxAIPlayers = 4;
			int totalAIPlayers = bot.getLobby().getAIPlayerCount();
			if (totalAIPlayers >= maxAIPlayers) {
				session.sendMessage(event.getChannel(),
						"Can't add any more bots!  You have reached the bot limit ("
								+ maxAIPlayers + " bots)");
				break;
			}

			if (!AIPlayer.canBeCreated()) {
				session.sendMessage(event.getChannel(),
						"Sorry, bots cannot be added due to an environmental error.");
				break;
			}

			AISlackPlayer slackPlayer = new AISlackPlayer("Sim"
					+ (totalAIPlayers + 1));
			Player aiPlayer = new Player(true);
			bot.getLobby().addUser(slackPlayer, aiPlayer);
			addedBots = true;
			session.sendMessage(bot.getCurrentChannel(),
					WatchWordBot.getUsernameString(slackPlayer) + " has joined the game!");

		}
		if (addedBots) {
			session.sendMessage(bot.getCurrentChannel(), bot.printFactions());
			bot.printMatchQuality();
			if (!bot.isAIThreadRunning()) {
				bot.handleAIGuesses();
			}
		}
	}
}
