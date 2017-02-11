package com.raether.watchwordbot.commands;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import com.raether.watchwordbot.CompetitiveTime;
import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class TimeCommand extends Command {

	public TimeCommand() {
		super("time", "shows the remaining time", GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {

		if (bot.getGame().getActingFaction() == null) {
			session.sendMessage(event.getChannel(),
					"Game is currently in an invalid state, there is currently no acting faction!");
			return;
		}
		CompetitiveTime time = bot.getGame().getRemainingTime();
		if (time == null) {
			session.sendMessage(event.getChannel(),
					"There is currently no timer enabled for the "
							+ bot.getGame().getActingFaction().getName()
							+ " team.");
			return;
		}
		session.sendMessage(event.getChannel(),
				"Remaining time for the "
						+ bot.getGame().getActingFaction().getName()
						+ " team: " + time.getTime(TimeUnit.SECONDS)
						+ " secs. (" + time.getOvertime(TimeUnit.SECONDS)
						+ " secs. of overtime)");
	}
}
