package com.raether.watchwordbot.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHIssue;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.raether.watchwordbot.gh.GitHubHelper;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class FeatCommand extends Command {

	public FeatCommand() {
		super("feat", Arrays.asList("feature"), "submit a feature request",
				false, GameState.IDLE, GameState.LOBBY, GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {

		if (!bot.getGHRepo().isPresent()) {
			session.sendMessage(event.getChannel(),
					"Could not establish link to github.");
			return;
		}
		List<Command> featCommands = new ArrayList<Command>();

		featCommands.add(new Command("list", "list all feature requests",
				GameState.getAllStates()) {

			@Override
			public void run(WatchWordBot bot, SlackMessagePosted event,
					LinkedList<String> args, SlackSession session) {
				if (!args.isEmpty()) {
					bot.printUsage(event.getChannel(), "feat list");
					return;
				}

				try {
					String out = "Feature Requests:\n";
					List<GHIssue> issues = GitHubHelper.getSlackIssues(bot
							.getGHRepo().get());
					for (GHIssue issue : issues) {
						out += printUserFeedbackRequest(issue) + "\n";
					}
					session.sendMessage(event.getChannel(), out);
				} catch (Exception e) {
					session.sendMessage(event.getChannel(),
							"Could not read features");
				}
			}
		});
		featCommands.add(new Command("add", "add a feature request", GameState
				.getAllStates()) {
			@Override
			public void run(WatchWordBot bot, SlackMessagePosted event,
					LinkedList<String> args, SlackSession session) {
				if (args.isEmpty()) {
					bot.printUsage(event.getChannel(), "feat add [description]");
					return;
				}
				String description = StringUtils.join(args, " ");

				try {
					GHIssue issue = GitHubHelper.createGHIssue(bot.getGHRepo()
							.get(), description, printFullUserDetails(event
							.getSender()));
					session.sendMessage(event.getChannel(),
							"Successfully created issue:"
									+ printUserFeedbackRequest(issue));

				} catch (Exception e) {
					session.sendMessage(event.getChannel(),
							"Could not create issue:" + e);
				}
			}
		});

		featCommands.add(new Command("close", "close a feature request",
				GameState.getAllStates()) {
			@Override
			public void run(WatchWordBot bot, SlackMessagePosted event,
					LinkedList<String> args, SlackSession session) {
				if (args.isEmpty()) {
					bot.printUsage(event.getChannel(), "feat close [id]");
					return;
				}
				Integer id = null;
				try {
					id = Integer.parseInt(args.pop());
				} catch (Exception e) {
					session.sendMessage(event.getChannel(), "Could not parse '"
							+ args.pop() + "'");
					return;
				}

				try {
					List<GHIssue> issues = GitHubHelper.getSlackIssues(bot
							.getGHRepo().get());

					GHIssue foundIssue = null;
					for (GHIssue issue : issues) {
						if (issue.getNumber() == id) {
							foundIssue = issue;
							break;
						}
					}
					if (foundIssue == null) {
						session.sendMessage(event.getChannel(),
								"Could not find feature with id " + id);
						return;
					}

					GitHubHelper.removeGHIssue(bot.getGHRepo().get(),
							printFullUserDetails(event.getSender()), id);
					session.sendMessage(event.getChannel(), "Deleted " + id);
				} catch (Exception e) {
					session.sendMessage(event.getChannel(),
							"Could not delete feature with id " + id);
				}
			}
		});

		String featureSubcommandHelp = bot.printCommands(
				"Feature subcommand help", featCommands);

		if (args.isEmpty()) {
			bot.printUsage(event.getChannel(), "feat [sub_command] (params)");
			session.sendMessage(event.getChannel(), featureSubcommandHelp);
			return;
		}

		String commandText = args.pop();
		Command command = bot.findMatchingCommand(commandText, args,
				featCommands, event.getChannel());
		if (command != null) {
			command.run(bot, event, args, session);
		} else {
			session.sendMessage(event.getChannel(), featureSubcommandHelp);
		}

	}

	private static String printFullUserDetails(SlackUser user) {
		return user.getUserName() + " (" + user.getRealName() + ")";
	}

	private static String printUserFeedbackRequest(GHIssue issue) {
		return issue.getNumber() + ":" + issue.getTitle() + "("
				+ issue.getHtmlUrl() + ")";
	}
}
