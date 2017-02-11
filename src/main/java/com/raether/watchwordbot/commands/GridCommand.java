package com.raether.watchwordbot.commands;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class GridCommand extends Command {

	public GridCommand() {
		super("grid", Arrays.asList("board"), "show the WatchWords grid",
				false, GameState.GAME);
	}

	@Override
	public boolean matches(String command, List<String> args) {
		return super.matches(command, args) || command.matches("g+r+i+d+");
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		session.sendMessage(event.getChannel(), bot.printCardGrid());
	}
}
