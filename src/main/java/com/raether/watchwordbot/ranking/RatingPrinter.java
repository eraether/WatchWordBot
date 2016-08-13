package com.raether.watchwordbot.ranking;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import jskills.Rating;

public class RatingPrinter {

	public static String printRating(Rating rating) {
		if (rating == null) {
			return "";
		}

		NavigableMap<Integer, String> allRatingStrings = createAllRatingStrings(3);

		double valueBeingCompared = rating.getMean()
				- rating.getStandardDeviation();// aka
												// conservativeStandardDeviationMultiplier=1

		Entry<Integer, String> floorEntry = allRatingStrings
				.floorEntry((int) valueBeingCompared);
		Entry<Integer, String> ceilEntry = allRatingStrings
				.ceilingEntry((int) valueBeingCompared);

		if (ceilEntry == null && floorEntry == null) {
			return "";
		}

		if (ceilEntry == null) {
			return floorEntry.getValue();
		}

		if (floorEntry == null) {
			return ceilEntry.getValue();
		}

		double floorError = Math.abs(valueBeingCompared - floorEntry.getKey());
		double ceilError = Math.abs(valueBeingCompared - ceilEntry.getKey());

		System.out.println("Floor Entry:" + floorEntry + "," + floorError
				+ ", Ceil Entry:" + ceilEntry + "," + ceilError);

		// floor is closer
		if (floorError < ceilError) {
			return floorEntry.getValue();
		}
		return ceilEntry.getValue();
	}

	private static NavigableMap<Integer, String> createAllRatingStrings(
			int maxDepth) {
		NavigableMap<Integer, String> currencyValues = new TreeMap<Integer, String>();
		currencyValues.put(1, "π");
		currencyValues.put(2, "Π");
		currencyValues.put(4, "φ");
		currencyValues.put(8, "Φ");
		currencyValues.put(16, "ω");
		currencyValues.put(32, "Ω");

		NavigableMap<Integer, String> intermediateResults = new TreeMap<Integer, String>();
		intermediateResults.put(0, "");
		for (int x = 0; x < maxDepth; x++) {
			NavigableMap<Integer, String> accumulatedResults = new TreeMap<Integer, String>();
			for (Integer key : intermediateResults.keySet()) {
				for (Integer currency : currencyValues.keySet()) {
					accumulatedResults.put(
							currency + key,
							intermediateResults.get(key)
									+ currencyValues.get(currency));
				}
			}
			intermediateResults = accumulatedResults;
		}
		return intermediateResults;
	}
}
