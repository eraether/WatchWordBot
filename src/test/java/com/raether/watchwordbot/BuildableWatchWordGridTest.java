package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class BuildableWatchWordGridTest {

	@Test
	public void randomlyAssignTwoFactionsTest() {
		int width = 5;
		int height = 5;
		int total = width*height;
		
		BuildableWatchWordGrid buildableGrid = createSimpleBuildableWatchWordGrid(width, height);
		Faction factionA = new Faction("A");
		int aAssignments = 9;
		Faction factionB = new Faction("B");
		int bAssignments = 8;
		Faction remainingFaction = new Faction("C");
		int cAssignments = total-aAssignments-bAssignments;
		
		Random random = new Random();
		
		
		buildableGrid.randomlyAssign(factionA, aAssignments, random);
		buildableGrid.randomlyAssign(factionB, bAssignments, random);
		buildableGrid.fillRemainder(remainingFaction);
		
		WatchWordGrid grid = buildableGrid.build();
		
		Assert.assertEquals(aAssignments, grid.getTilesForFaction(factionA).size());
		Assert.assertEquals(bAssignments, grid.getTilesForFaction(factionB).size());
		Assert.assertEquals(cAssignments, grid.getTilesForFaction(remainingFaction).size());
	}

	private static BuildableWatchWordGrid createSimpleBuildableWatchWordGrid(int width,
			int height) {
		List<String> words = new ArrayList<String>();
		int totalWords = width * height;
		for (int x = 0; x < totalWords; x++) {
			words.add(x + "");
		}
		return new BuildableWatchWordGrid(words, width, height);
	}
}
