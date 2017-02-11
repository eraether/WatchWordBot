package com.raether.watchwordbot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class TextRenderer {
	private SlackMessagePosted event;
	private SlackSession session;

	public void updateScope(SlackMessagePosted event, SlackSession session) {
		this.session = session;
		this.event = event;
	}

	public void handleException(Exception e) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		e.printStackTrace(ps);
		String content;
		content = baos.toString();
		session.sendMessage(event.getChannel(), "Exception Encountered:\n"
				+ content);
	}

}
