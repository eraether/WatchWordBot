package com.raether.watchwordbot.commands;

import java.util.LinkedList;
import java.util.List;

import com.raether.watchwordbot.ELOEvent;
import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.Player;
import com.raether.watchwordbot.WatchWordBot;
import com.raether.watchwordbot.WatchWordClue;
import com.raether.watchwordbot.WordTile;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class ClueCommand extends Command {

	public ClueCommand() {
		super("clue", "give a clue to your team", GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		Player player = bot.getLobby().getPlayer(event.getSender());
		if (player == null) {
			session.sendMessage(event.getChannel(), "You are not in the game!");
			return;
		}

		if (bot.getLobby().getTurnOrder().getFactionFor(player) != bot.getGame()
				.getTurnOrder().getCurrentTurn()) {
			session.sendMessage(event.getChannel(),
					"It is not your turn to give a clue!");
			if (WatchWordBot.DEBUG) {
				session.sendMessage(event.getChannel(),
						"However, I'll allow it for now...");
			} else {
				return;
			}
		}

		if (player != bot.getGame().getTurnOrder().getCurrentTurn().getLeader()) {
			session.sendMessage(event.getChannel(),
					"Only the current turn's leader can give a clue.");
			if (WatchWordBot.DEBUG) {
				session.sendMessage(event.getChannel(),
						"However, I'll allow it for now...");
			} else {
				return;
			}
		}
		if (bot.getGame().getClue() != null) {
			session.sendMessage(event.getChannel(), "A clue was already given.");
			session.sendMessage(event.getChannel(), bot.printGivenClue());
			return;
		}

		if (args.size() != 2) {
			bot.printUsage(event.getChannel(), "clue <word> [amount|unlimited]");
			return;
		}

		String word = args.pop();
		String unparsedNumber = args.pop();
		int totalGuesses = -1;
		boolean unlimitedGuesses = false;
		boolean zeroClue = false;

		if (unparsedNumber.toLowerCase().startsWith("unlimited")) {
			unlimitedGuesses = true;
		} else {
			try {
				totalGuesses = Integer.parseInt(unparsedNumber);
				if (totalGuesses == 0) {
					zeroClue = true;
				} else if (totalGuesses < 0) {
					session.sendMessage(event.getChannel(),
							"You must give a clue with at least one guess!");
					return;
				}
				totalGuesses += 1;// bonus guess

			} catch (Exception e) {
				session.sendMessage(event.getChannel(), "Could not parse '"
						+ unparsedNumber + "' as a number.");
				return;
			}
			;
		}

		List<WordTile> tiles = bot.getGame().getGrid().getTilesForWord(word);
		if (!tiles.isEmpty()) {
			session.sendMessage(event.getChannel(),
					"Your clue matches tiles on the board!  Please give a different clue.");
			return;
		}

		bot.getGame().giveClue(new WatchWordClue(word, totalGuesses, unlimitedGuesses,
				zeroClue));

		bot.recordELOEvent(ELOEvent.CLUE_GIVEN, player);
		session.sendMessage(bot.getCurrentChannel(),
				WatchWordBot.getUsernameString(event.getSender()) + " has given a clue.");
		session.sendMessage(bot.getCurrentChannel(), bot.printGivenClue());
		session.sendMessage(bot.getCurrentChannel(), bot.printCardGrid());
		bot.waitForGuess();
	}
}
