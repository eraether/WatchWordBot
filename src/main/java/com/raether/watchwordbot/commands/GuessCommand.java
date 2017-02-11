package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class GuessCommand extends Command {

	public GuessCommand() {
		super("guess", "guess a word", GameState.GAME);
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
		if (args.isEmpty()) {
			bot.printUsage(event.getChannel(), "guess <word>");
			return;
		}

		String wordBeingGuessed = args.pop();
		bot.makeGuess(wordBeingGuessed, session, event.getChannel(),
				event.getSender());
	}
}
