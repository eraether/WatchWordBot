package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Command {
	private String primaryAlias = "";
	private List<String> additionalAliases = new ArrayList<String>();
	private String helpText = "";
	private List<GameState> validGameStates = new ArrayList<GameState>();

	public Command(String primaryAlias, String helpText,
			GameState... validGameStates) {
		this(primaryAlias, new ArrayList<String>(), helpText, validGameStates);
	}

	public Command(String primaryAlias, List<String> additionalAliases,
			String helpText, GameState... validGameStates) {
		this.primaryAlias = primaryAlias;
		this.additionalAliases = additionalAliases;
		this.helpText = helpText;
		this.validGameStates = Arrays.asList(validGameStates);
	}

	// execute your command!
	public abstract void run();

	public String getPrimaryAlias() {
		return this.primaryAlias;
	}

	public boolean hasAdditionalAliases() {
		return !additionalAliases.isEmpty();
	}

	public List<String> getAdditionalAliases() {
		return additionalAliases;
	}

	public List<String> getAllAliases() {
		ArrayList<String> allAliases = new ArrayList<String>();
		allAliases.add(getPrimaryAlias());
		allAliases.addAll(getAdditionalAliases());
		return allAliases;
	}

	public List<GameState> getValidGameStates() {
		return this.validGameStates;
	}

	public boolean matches(String commandText) {
		for (String alias : getAllAliases()) {
			if (alias.equalsIgnoreCase(commandText)) {
				return true;
			}
		}
		return false;
	}

	public String getHelpText() {
		return this.helpText;
	}

}
