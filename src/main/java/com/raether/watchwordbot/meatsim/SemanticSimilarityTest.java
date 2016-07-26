package com.raether.watchwordbot.meatsim;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.cmu.lti.jawjaw.db.SQL;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.SynsetDAO;
import edu.cmu.lti.jawjaw.db.SynsetDefDAO;
import edu.cmu.lti.jawjaw.pobj.Lang;
import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.jawjaw.pobj.Synlink;
import edu.cmu.lti.jawjaw.pobj.Synset;
import edu.cmu.lti.jawjaw.pobj.SynsetDef;
import edu.cmu.lti.jawjaw.util.WordNetUtil;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.util.PathFinder.Subsumer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

public class SemanticSimilarityTest {

	private ILexicalDatabase db = new NictWordNet();

	public SemanticSimilarityTest() {

	}

	public static void main(String[] args) {
		SemanticSimilarityTest test = new SemanticSimilarityTest();
		test.runTest2();
	}

	public void runTest2() {
		String word = "man";
		for (POS pos : POS.values()) {
			List<edu.cmu.lti.jawjaw.pobj.Synset> synsets = WordNetUtil
					.wordToSynsets(word, pos);
			for (Synset synset : synsets) {
				SynsetDef def = SynsetDefDAO.findSynsetDefBySynsetAndLang(
						synset.getSynset(), Lang.eng);
				// System.out.println(WordNetUtil.getGloss(def));

				System.out.println(def.getSynset() + ":" + def.getDef());
			}
		}
	}

	class MyRelatednessCalculator extends RelatednessCalculator {
		protected double min = 0; // actually, (0, 1]
		protected double max = 1;

		public MyRelatednessCalculator(ILexicalDatabase db) {
			super(db);
		}

		@Override
		protected Relatedness calcRelatedness(Concept synset1, Concept synset2) {
			StringBuilder tracer = new StringBuilder();
			if (synset1 == null || synset2 == null)
				return new Relatedness(min, null, illegalSynset);
			if (synset1.getSynset().equals(synset2.getSynset()))
				return new Relatedness(max, identicalSynset, null);

			StringBuilder subTracer = enableTrace ? new StringBuilder() : null;
			List<Subsumer> shortestPaths = pathFinder.getShortestPaths(synset1,
					synset2, subTracer);

			if (shortestPaths.size() == 0)
				return new Relatedness(min); // TODO message
			Subsumer path = shortestPaths.get(0);
			int dist = path.length;
			double score;
			if (dist > 0) {
				score = 1D / (double) dist;
			} else {
				score = -1;
			}

			if (enableTrace) {
				tracer.append(subTracer.toString());
				tracer.append("Shortest path: " + path + "\n");
				tracer.append("Path length = " + dist + "\n");
			}

			System.out.println(tracer.toString());

			return new Relatedness(score, tracer.toString(), null);
		}

		@Override
		public List<POS[]> getPOSPairs() {
			ArrayList<POS[]> allCombinations = new ArrayList<POS[]>();
			for (POS value1 : POS.values()) {
				for (POS value2 : POS.values()) {
					allCombinations.add(new POS[] { value1, value2 });
				}
			}
			return allCombinations;
		}
	}

	private void readExamples(String synset, Lang searchLang)
			throws SQLException {
		// System.out.println(def.getDef());
		PreparedStatement statement = SQL
				.getInstance()
				.getConnection()
				.prepareStatement(
						"SELECT * FROM synset_ex WHERE synset=? AND lang=?");
		statement.setString(1, synset);
		statement.setString(2, searchLang.toString());
		ResultSet rs = statement.executeQuery();
		synchronized (rs) {
			while (rs.next()) {
				String synsetId = rs.getString(1);
				Lang lang = Lang.valueOf(rs.getString(2));
				String example = rs.getString(3);
				Integer sid = rs.getInt(4);
				System.out.println("Example:" + example + " (" + lang + "/"
						+ sid + ")");
			}
			rs.close();
		}

	}

	private List<Synset> readAncestor(String synset) throws SQLException {
		// System.out.println(def.getDef());
		PreparedStatement statement = SQL
				.getInstance()
				.getConnection()
				.prepareStatement(
						"SELECT synset2 FROM ancestor WHERE synset1=?");
		statement.setString(1, synset);
		ResultSet rs = statement.executeQuery();
		List<Synset> synsets = new ArrayList<Synset>();
		synchronized (rs) {
			while (rs.next()) {
				String parentId = rs.getString(1);
				synsets.add(SynsetDAO.findSynsetBySynset(parentId));
			}
			rs.close();
		}
		return synsets;
	}

	private void printSynsetDef(Synset synset, int offset,
			boolean alreadyInCache) {
		// SynsetDef def = SynsetDefDAO.findSynsetDefBySynsetAndLang(
		// synset.getSynset(), Lang.eng);

		String padding = StringUtils.repeat("    ", offset);
		System.out.println(padding + synset.getName() + "("
				+ synset.getPos().toString() + "): "
				+ (alreadyInCache ? "(Already in cache)" : ""));// +
																// def.getDef());
	}

	private List<String> cache = new ArrayList<String>();
	int cacheHits = 0;

	private void recursivelyPrintDefinitions(Synset synset, int currentDepth,
			int maxDepth) throws SQLException {
		if (currentDepth > maxDepth) {
			return;
		}
		boolean alreadyInCache = cache.contains(synset.getSynset());
		printSynsetDef(synset, currentDepth, alreadyInCache);
		if (alreadyInCache) {
			cacheHits++;
			return;
		}
		cache.add(synset.getSynset());
		List<Synset> synsets = readAncestor(synset.getSynset());
		for (Synset ancestorSynset : synsets) {
			recursivelyPrintDefinitions(ancestorSynset, currentDepth + 1,
					maxDepth);
		}

		// List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynset(synset
		// .getSynset());
		// for (Synlink synlink : synlinks) {
		// recursivelyPrintDefinitions(
		// SynsetDAO.findSynsetBySynset(synlink.getSynset2()),
		// currentDepth + 1, maxDepth);
		// }

	}

	private List<Result> manualCompete(String word) throws SQLException {
		System.out.println("Finding values for " + word);
		for (POS pos : POS.values()) {
			List<edu.cmu.lti.jawjaw.pobj.Synset> synsets = WordNetUtil
					.wordToSynsets(word, pos);
			for (Synset synset : synsets) {
				recursivelyPrintDefinitions(synset, 1, 100);
			}

		}
		System.out.println("----------------------");

		// Collection<Concept> synsets1 = db.getAllConcepts(word1,
		// posPair[0].toString());
		// Collection<Concept> synsets2 = db.getAllConcepts(word2,
		// posPair[1].toString());
		//
		// WordSimilarityCalculator wordSimilarity = new
		// WordSimilarityCalculator();
		// MyRelatednessCalculator calculator = new MyRelatednessCalculator(db);

		return new ArrayList<Result>();
	}

	// MFS = most frequent sense
	private List<Result> compute(String word1, String word2) {
		WS4JConfiguration.getInstance().setMFS(false);
		WS4JConfiguration.getInstance().setTrace(true);
		RelatednessCalculator[] rcs = { new MyRelatednessCalculator(db) };
		// new HirstStOnge(db),
		// new LeacockChodorow(db), new Lesk(db), new WuPalmer(db),
		// new Resnik(db), new JiangConrath(db), new Lin(db), new Path(db) };
		List<Result> allResults = new ArrayList<Result>();
		for (RelatednessCalculator calc : rcs) {
			double s = calc.calcRelatednessOfWords(word1, word2);
			allResults.add(new Result(word1, word2, calc, s));
		}
		return allResults;
	}

	public void runTest() {
		List<String> allWords = Arrays.asList("pursuit");/*-, "vegetation", "prize",
															"jockey", "blade", "teacher", "leave", "price", "currency",
															"rob", "utter", "bomb", "have", "magnitude", "gallery", "limb",
															"detector", "husband", "altar", "bracket", "talk", "dive",
															"kitchen", "lover", "update");*/

		// String wordA = JOptionPane
		// .showInputDialog("Enter word to compare against");
		List<Result> results = new ArrayList<Result>();
		for (String wordB : allWords) {
			try {
				List<Result> result = manualCompete(wordB);// compute(wordA,
															// wordB);
				results.addAll(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
			;
		}

		Collections.sort(results, new Comparator<Result>() {

			@Override
			public int compare(Result o1, Result o2) {
				return o1.result.compareTo(o2.result);
			}

		});

		for (Result res : results) {
			System.out.println(res.calc.getClass().getName() + ": " + res.a
					+ "," + res.b + "," + res.result);
		}

		System.out.println("Cache hits:" + cacheHits);

		// String[] words = { "add", "get", "filter", "remove", "check", "find",
		// "collect", "create" };
		//
		// for (int i = 0; i < words.length - 1; i++) {
		// for (int j = i + 1; j < words.length; j++) {
		// double distance = compute(words[i], words[j]);
		// System.out.println(words[i] + " -  " + words[j] + " = "
		// + distance);
		// }
		// }
	}
}

class Result {
	public String a;
	public String b;
	public Double result;
	public RelatednessCalculator calc;

	public Result(String a, String b, RelatednessCalculator calc, double result) {
		this.a = a;
		this.b = b;
		this.result = result;
		this.calc = calc;
	}
}
