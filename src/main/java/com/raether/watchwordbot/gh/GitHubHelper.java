package com.raether.watchwordbot.gh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;

public class GitHubHelper {

	public static void removeGHIssue(GHRepository repo, String user, int issueID)
			throws IOException {
		GHIssue issue = repo.getIssue(issueID);
		if (isSlackIssue(issue)) {
			issue.comment("Closed by " + user);
			issue.close();
		}
	}

	public static boolean isSlackIssue(GHIssue issue) throws IOException {
		for (GHLabel label : issue.getLabels()) {
			for (String slackLabel : getSlackLabels()) {
				if (label.getName().equalsIgnoreCase(slackLabel)) {
					return true;
				}
			}
		}
		return false;
	}

	public static String getPrimarySlackLabel() {
		return "SlackRequest";
	}

	public static List<String> getSlackLabels() {
		return Arrays.asList(getPrimarySlackLabel());
	}

	public static GHIssue createGHIssue(GHRepository repo, String description,
			String user) throws IOException {
		GHIssueBuilder builder = repo.createIssue(description);
		builder.label(getPrimarySlackLabel());
		GHIssue issue = builder.create();
		issue.comment("Created by " + user);
		System.out.println(issue.getHtmlUrl());
		return issue;
	}

	public static List<GHIssue> getSlackIssues(GHRepository repo)
			throws IOException {
		List<GHIssue> filteredIssues = new ArrayList<GHIssue>();
		List<GHIssue> issues = repo.getIssues(GHIssueState.OPEN);
		for (GHIssue issue : issues) {
			if (isSlackIssue(issue)) {
				filteredIssues.add(issue);
			}
		}
		return filteredIssues;
	}
}
