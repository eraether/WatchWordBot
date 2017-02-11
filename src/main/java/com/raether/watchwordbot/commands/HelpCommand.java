package com.raether.watchwordbot.commands;

import java.util.LinkedList;
import java.util.List;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class HelpCommand extends Command {

	public HelpCommand() {
		super("help", "yes, this is help", true, GameState.IDLE,
				GameState.LOBBY, GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		List<Command> commands = bot.generateCommands(event, args, session);

		session.sendMessage(event.getChannel(),
				bot.printCommands("Contextual Help", commands));
	}
}
