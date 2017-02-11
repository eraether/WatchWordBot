package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class ListCommand extends Command {

	public ListCommand() {
		super("list", "show all the players in the game", GameState.LOBBY,
				GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		session.sendMessage(event.getChannel(), bot.printFactions());
	}
}
