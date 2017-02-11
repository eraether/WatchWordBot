package com.raether.watchwordbot.commands;

import java.util.Arrays;
import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class WinCommand extends Command {

	public WinCommand() {
		super("win", "win", true, GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		bot.finishGame(
				Arrays.asList(bot.getGame().getTurnOrder().getCurrentTurn()),
				session);
	}
}
