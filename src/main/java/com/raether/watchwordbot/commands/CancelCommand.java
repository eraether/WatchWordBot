package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class CancelCommand extends Command {

	public CancelCommand() {
		super("cancel", "cancel the game and lobby", GameState.LOBBY,
				GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (bot.getGameState() == GameState.GAME) {
			session.sendMessage(
					bot.getCurrentChannel(),
					"Game has been canceled by "
							+ WatchWordBot.getUsernameString(event.getSender()));
			bot.partialGameReset();
			session.sendMessage(bot.getCurrentChannel(), bot.printFactions());
		} else {
			session.sendMessage(
					bot.getCurrentChannel(),
					"Lobby has been canceled by "
							+ WatchWordBot.getUsernameString(event.getSender()));
			bot.fullGameReset();
		}
	}
}
