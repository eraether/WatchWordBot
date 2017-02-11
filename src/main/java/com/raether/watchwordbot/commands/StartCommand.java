package com.raether.watchwordbot.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.raether.watchwordbot.BuildableWatchWordGrid;
import com.raether.watchwordbot.CompetitiveTimer;
import com.raether.watchwordbot.Faction;
import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.TurnOrder;
import com.raether.watchwordbot.WatchWordBot;
import com.raether.watchwordbot.WatchWordGame;
import com.raether.watchwordbot.WatchWordGrid;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class StartCommand extends Command {
	public StartCommand() {
		super("start", "starts the game", GameState.LOBBY);
	}

	@Override
	public boolean matches(String command, List<String> args) {
		return super.matches(command, args)
				|| (command.equals("game") && args.size() == 1 && args.get(0)
						.equals("on"));
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (args.size() == 1) {
			if (args.pop().equalsIgnoreCase("debug")) {
				WatchWordBot.DEBUG = Boolean.TRUE;
			} else {
				WatchWordBot.DEBUG = Boolean.FALSE;
			}
		}

		bot.refreshGameResources();
		bot.updateGameState(GameState.GAME);
		session.sendMessage(bot.getCurrentChannel(), bot.getMessageGenerator()
				.getGameStartMessage());
		long seed1 = System.nanoTime();
		Random random1 = new Random(seed1);
		int totalRows = 5;
		int totalCols = 5;
		List<String> words = bot.getWordGenerator().generateWords(
				totalRows * totalCols, random1);

		BuildableWatchWordGrid buildableGrid = new BuildableWatchWordGrid(
				words, totalRows, totalCols);

		TurnOrder turnOrder = bot.getLobby().getTurnOrder();
		turnOrder.shuffle(random1);
		for (Faction faction : turnOrder.getAllFactions()) {

			long overtimeDuration = 10;
			TimeUnit overtimeTimeUnit = TimeUnit.MINUTES;
			faction.setCompetitiveTimer(new CompetitiveTimer(overtimeDuration,
					overtimeTimeUnit));
		}

		Faction neutralFaction = new Faction("Neutral", null);
		Faction assassinFaction = new Faction("Assassin", null);

		int firstFactionCards = 9;
		int secondFactionCards = 8;
		int assassinCards = 1;

		buildableGrid.randomlyAssign(turnOrder.getCurrentTurn(),
				firstFactionCards, random1);
		buildableGrid.randomlyAssign(turnOrder.getNextTurn(),
				secondFactionCards, random1);
		buildableGrid.randomlyAssign(assassinFaction, assassinCards, random1);
		buildableGrid.fillRemainder(neutralFaction);
		WatchWordGrid grid = buildableGrid.build();
		WatchWordGame game = new WatchWordGame(grid, turnOrder, neutralFaction,
				assassinFaction);

		bot.setGame(game);

		session.sendMessage(bot.getCurrentChannel(), bot.printCardGrid());
		session.sendMessage(bot.getCurrentChannel(), bot.printFactions());
		session.sendMessage(bot.getCurrentChannel(), bot.printCurrentTurn());
		session.sendMessage(bot.getCurrentChannel(), bot.printGivenClue());

		for (Faction faction : game.getTurnOrder().getAllFactions()) {
			if (faction.hasLeader() && !faction.getLeader().isAIControlled()) {
				SlackUser user = bot.getLobby().getUser(faction.getLeader());

				SlackUser opponent = bot.getLobby().getUser(
						game.getTurnOrder().getFactionAfter(faction)
								.getLeader());
				try {
					session.sendMessageToUser(user, bot
							.printWordSmithInstructions(user, opponent,
									faction, game.getAssassinFaction()), null);
					session.sendMessageToUser(user, bot.printCardGrid(true),
							null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		bot.printMatchQuality();
		bot.waitForClue();
	}
}
