package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TurnOrder {
	private List<Faction> allFactions = new ArrayList<Faction>();
	private int currentTurn = 0;

	public TurnOrder() {

	}

	public TurnOrder(List<Faction> factions) {
		this.allFactions = factions;
	}

	public List<Faction> getAllFactions() {
		return allFactions;
	}

	public void addFaction(Faction faction) {
		recomputeTurnOrder();
		allFactions.add(faction);
	}

	public void removeFaction(Faction faction) {
		recomputeTurnOrder();
		allFactions.remove(faction);
	}

	public Faction getCurrentTurn() {
		return getFactionAtTurn(currentTurn);
	}

	public Faction getNextTurn() {
		return getFactionAtTurn(currentTurn + 1);
	}

	private void recomputeTurnOrder() {
		List<Faction> simplifiedTurnOrder = new ArrayList<Faction>();
		for (int x = 0; x < allFactions.size(); x++) {
			int turn = currentTurn + x;
			simplifiedTurnOrder.add(getFactionAtTurn(turn));
		}

		this.allFactions = simplifiedTurnOrder;
		this.currentTurn = 0;
	}

	private Integer getTurnForFaction(Faction faction) {
		for (int x = currentTurn; x < currentTurn + allFactions.size(); x++) {
			if (getFactionAtTurn(x) == faction) {
				return x;
			}
		}
		return null;
	}

	private Faction getFactionAtTurn(int turn) {
		int circularTurn = turn % allFactions.size();
		return allFactions.get(circularTurn);
	}

	public void shuffle(Random rand) {
		Collections.shuffle(allFactions, rand);
	}

	public void nextTurn() {
		currentTurn += 1;
	}

	public Faction getLeastFullFaction() {
		if (allFactions.isEmpty()) {
			return null;
		}

		Faction leastFullFaction = allFactions.get(0);
		for (Faction faction : allFactions) {
			if (faction.getAllPlayers().size() < leastFullFaction
					.getAllPlayers().size()) {
				leastFullFaction = faction;
			}
		}
		return leastFullFaction;
	}

	public boolean removePlayer(Player player) {
		Faction faction = getFactionFor(player);
		if (faction == null) {
			return false;
		}
		return faction.removePlayer(player);
	}

	public Faction getFactionFor(Player player) {
		for (Faction faction : allFactions) {
			if (faction.getAllPlayers().contains(player)) {
				return faction;
			}
		}
		return null;
	}

	public Faction swapFactions(Player player) {
		Integer currentFactionIndex = getTurnForFaction(getFactionFor(player));
		if (currentFactionIndex == null) {
			return null;
		}

		Faction currentFaction = getFactionAtTurn(currentFactionIndex);
		Faction newFaction = getFactionAtTurn(currentFactionIndex + 1);

		currentFaction.removePlayer(player);
		newFaction.addPlayer(player);
		return newFaction;
	}

	public boolean swapPlayers(Player firstPlayer, Player secondPlayer) {
		Faction firstPlayerFaction = getFactionFor(firstPlayer);
		Faction secondPlayerFaction = getFactionFor(secondPlayer);

		Integer firstPlayerIndex = firstPlayerFaction
				.getPlayerIndex(firstPlayer);
		Integer secondPlayerIndex = secondPlayerFaction
				.getPlayerIndex(secondPlayer);

		if (firstPlayerIndex == null || secondPlayerIndex == null) {
			return false;
		}

		firstPlayerFaction.getAllPlayers().set(firstPlayerIndex, secondPlayer);
		secondPlayerFaction.getAllPlayers().set(secondPlayerIndex, firstPlayer);

		return true;
	}

	public void shuffleFactionLeaders() {
		for (Faction faction : allFactions) {
			faction.shiftMembership();
		}
	}

	public Faction getFactionAfter(Faction faction) {
		return getFactionAtTurn(getTurnForFaction(faction) + 1);
	}

	public List<Faction> getAllTurnsExceptCurrent() {
		List<Faction> allFactionsExceptCurrent = new ArrayList<Faction>();
		for (Faction faction : this.getAllFactions()) {
			if (faction != this.getCurrentTurn()) {
				allFactionsExceptCurrent.add(faction);
			}
		}
		return allFactionsExceptCurrent;
	}
}