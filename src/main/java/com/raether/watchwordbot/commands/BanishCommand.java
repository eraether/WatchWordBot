package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class BanishCommand extends Command {
	
	public BanishCommand(){
		super("banish",
				"banish a word from the game forever", GameState.GAME);
	}
	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (args.isEmpty()) {
			fireIncorrectUsage();
			return;
		}

		String banishedWord = args.pop();

		boolean match = bot.getWordGenerator().isWordPresent(banishedWord);

		if (!match) {
			session.sendMessage(event.getChannel(), "'" + banishedWord
					+ "' not found in wordlist.");
			return;
		}

		session.sendMessage(event.getChannel(),
				"Banish command not implemented.");

	}
}
