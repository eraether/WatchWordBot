package com.raether.watchwordbot;

import java.util.concurrent.TimeUnit;

public class WatchWordGame {
	private WatchWordGrid grid;
	private TurnOrder turnOrder;
	private Faction neutralFaction;
	private Faction assassinFaction;
	private WatchWordClue currentClue;
	private int totalGuessesMadeThisTurn = 0;

	Long lastCountdownStartTime = null;
	Faction lastFactionToAct = null;

	public WatchWordGame(WatchWordGrid grid, TurnOrder playerFactions,
			Faction neutralFaction, Faction assassinFaction) {
		this.grid = grid;
		this.turnOrder = playerFactions;
		this.neutralFaction = neutralFaction;
		this.assassinFaction = assassinFaction;
	}

	public WatchWordGrid getGrid() {
		return this.grid;
	}

	public TurnOrder getTurnOrder() {
		return turnOrder;
	}

	public Faction getNeutralFaction() {
		return this.neutralFaction;
	}

	public Faction getAssassinFaction() {
		return this.assassinFaction;
	}

	public Faction getFactionForPlayer(Player player) {
		for (Faction faction : turnOrder.getAllFactions()) {
			if (faction.getAllPlayers().contains(player)) {
				return faction;
			}
		}
		return null;
	}

	public void changeTurns() {
		getTurnOrder().nextTurn();
		this.currentClue = null;
		this.totalGuessesMadeThisTurn = 0;
	}

	public void removeFaction(Faction guesserFaction) {
		getTurnOrder().removeFaction(guesserFaction);
	}

	public boolean wasClueGivenThisTurn() {
		return this.currentClue != null;
	}

	public void giveClue(WatchWordClue clue) {
		this.currentClue = clue;
	}

	public WatchWordClue getClue() {
		return this.currentClue;
	}

	public void guess() {
		totalGuessesMadeThisTurn++;
	}

	public int getRemainingGuesses() {
		if (!wasClueGivenThisTurn())
			return 0;
		return getClue().getAmount() - totalGuessesMadeThisTurn;
	}

	public void startCountingDown(int time, TimeUnit unit) {
		long leeway = TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS);

		if (lastFactionToAct != null && lastCountdownStartTime != null) {
			CompetitiveTimer timer = lastFactionToAct.getTimer();
			if (timer != null) {
				long diff = System.nanoTime() - lastCountdownStartTime - leeway;
				timer.reduceTimeBy(diff, TimeUnit.NANOSECONDS);
			}
		}
		lastFactionToAct = getTurnOrder().getCurrentTurn();
		lastCountdownStartTime = System.nanoTime();
		if (lastFactionToAct.hasTimer()) {
			lastFactionToAct.getTimer().setRemainingTime(time, unit);
		}
	}

	public CompetitiveTime getRemainingTime() {
		if (lastFactionToAct != null && lastFactionToAct.hasTimer()
				&& lastCountdownStartTime != null) {
			long diff = System.nanoTime() - lastCountdownStartTime;
			return lastFactionToAct.getTimer().getTimeAfter(diff,
					TimeUnit.NANOSECONDS);
		}
		return null;
	}

	public Faction getActingFaction() {
		return lastFactionToAct;
	}
}