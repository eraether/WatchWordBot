package com.raether.watchwordbot.commands;

import java.util.LinkedList;
import java.util.List;

import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.WatchWordBot;
import com.raether.watchwordbot.lex.LexicalDatabaseHelper;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

import edu.cmu.lti.jawjaw.db.SynsetDefDAO;
import edu.cmu.lti.jawjaw.pobj.Lang;
import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.jawjaw.pobj.Synset;
import edu.cmu.lti.jawjaw.pobj.SynsetDef;
import edu.cmu.lti.jawjaw.util.WordNetUtil;

public class DefineCommand extends Command {

	public DefineCommand() {
		super("define", "define a word", GameState.IDLE, GameState.LOBBY,
				GameState.GAME);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (!LexicalDatabaseHelper.canBeCreated()) {
			session.sendMessage(event.getChannel(),
					"Lexical database link could not be established.");
			return;
		}

		if (args.size() != 1) {
			this.fireIncorrectUsage();
			// printUsage(event.getChannel(), "define [word]");
			return;
		}
		String word = args.pop();

		String messageOutput = "Defining *" + word + "*:\n";
		for (POS pos : POS.values()) {
			List<edu.cmu.lti.jawjaw.pobj.Synset> synsets = WordNetUtil
					.wordToSynsets(word, pos);
			for (Synset synset : synsets) {
				SynsetDef def = SynsetDefDAO.findSynsetDefBySynsetAndLang(
						synset.getSynset(), Lang.eng);
				messageOutput += def.getSynset() + ":" + def.getDef() + "\n";
			}
		}
		session.sendMessage(event.getChannel(), messageOutput);
	}
}
