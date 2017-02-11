package com.raether.watchwordbot.commands;

import java.util.LinkedList;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class PenaltyCommand extends Command {

	public PenaltyCommand() {
		super("penalty", "penalize a player", GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		int penaltyAmount = 0;

		if (!args.isEmpty()) {
			try {
				penaltyAmount = Integer.parseInt(args.pop());
				if (penaltyAmount < 0) {
					throw new IllegalArgumentException(
							"Penalty severity must be >= 0");
				}
			} catch (Exception e) {
				bot.printUsage(event.getChannel(), this.getPrimaryAlias()
						+ " <penalty severity(0+)>");
				return;
			}
		}

		bot.penalizeCurrentFaction(penaltyAmount);
	}
}
