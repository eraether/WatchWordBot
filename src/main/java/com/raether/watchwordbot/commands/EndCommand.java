package com.raether.watchwordbot.commands;

import java.util.Arrays;
import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class EndCommand extends Command {

	public EndCommand() {
		super("end", Arrays.asList("pass"), "end the guessing phase", false,
				GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		bot.endTurn(session, event.getChannel(), event.getSender());
	}
}
