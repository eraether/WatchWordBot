package com.raether.watchwordbot.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public abstract class Command {
	private String primaryAlias = "";
	private List<String> additionalAliases = new ArrayList<String>();
	private String helpText = "";
	private List<GameState> validGameStates = new ArrayList<GameState>();
	private boolean hidden = false;
	
	public Command(){
		
	}

	public Command(String primaryAlias, String helpText,
			GameState... validGameStates) {
		this(primaryAlias, helpText, false, validGameStates);
	}

	public Command(String primaryAlias, String helpText, boolean hidden,
			GameState... validGameStates) {
		this(primaryAlias, new ArrayList<String>(), helpText, hidden,
				validGameStates);
	}

	public Command(String primaryAlias, List<String> additionalAliases,
			String helpText, boolean hidden, GameState... validGameStates) {
		this();
		this.primaryAlias = primaryAlias;
		this.additionalAliases = additionalAliases;
		this.helpText = helpText;
		this.validGameStates = Arrays.asList(validGameStates);
		this.hidden = hidden;
	}

	// execute your command!
	public abstract void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session);

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

	public boolean matches(String commandText, List<String> arguments) {
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

	public boolean isHidden() {
		return hidden;
	}

	public void fireIncorrectUsage() {
		//do something here TODO
	}
}
