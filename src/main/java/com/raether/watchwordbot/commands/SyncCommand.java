package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class SyncCommand extends Command {

	public SyncCommand() {
		super("sync", "synchronization test", true, GameState.IDLE,
				GameState.LOBBY, GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		session.sendMessage(bot.getCurrentChannel(), "Synchronization test...");
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		session.sendMessage(bot.getCurrentChannel(), "Done!");
	}
}
