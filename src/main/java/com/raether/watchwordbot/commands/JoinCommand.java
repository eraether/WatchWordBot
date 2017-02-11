package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class JoinCommand extends Command {

	public JoinCommand() {
		super("join", "join the game", GameState.LOBBY, GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		SlackUser newUser = event.getSender();
		if (!bot.getLobby().hasUser(newUser)) {
			bot.getLobby().addUser(newUser);
			session.sendMessage(bot.getCurrentChannel(),
					WatchWordBot.getUsernameString(event.getSender())
							+ " has joined the game!");
			session.sendMessage(bot.getCurrentChannel(), bot.printFactions());
			bot.printMatchQuality();

		} else {
			session.sendMessage(event.getChannel(),
					WatchWordBot.getUsernameString(event.getSender())
							+ ", you're already in the game!");
		}
	}
}
