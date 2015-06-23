package org.recsyschallenge.algorithms.recommender;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.spark.api.java.JavaRDD;
import org.recsyschallenge.algorithms.classification.TFIDFAnalysis;
import org.recsyschallenge.algorithms.helpers.ProgressMesurer;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.helpers.SparkHelper;
import org.recsyschallenge.models.SessionClicks;
import org.recsyschallenge.models.SessionInfo;

public class UserBasedRecomender {
	private GenericDataModel dataModel;
	private List<SessionInfo> buySessions;

	// must be static for spark task
	private static Recommender recommender;
	private static Map<Integer, List<Integer>> intersect = new HashMap<Integer, List<Integer>>();
	private static ProgressMesurer progress;

	public UserBasedRecomender(List<SessionInfo> sessions,
			List<SessionInfo> buySessions) throws IOException, TasteException {
		InfoOutputHelper.printInfo("Starting building recommendation phase");

		FastByIDMap<PreferenceArray> userData = new FastByIDMap<>();
		FastByIDMap<FastByIDMap<Long>> timestamps = new FastByIDMap<>();

		// JavaRDD<SessionInfo> rddSessions =
		// SparkHelper.sc.parallelize(sessions);
		// rddSessions.foreach(session -> userData.put(session.getSessionId(),
		// session.toPreferenceArray()));

		progress = new ProgressMesurer(5, sessions.size());

		// for (SessionInfo session : sessions) {
		// int sessionId = session.getSessionId();
		//
		// userData.put(sessionId, session.toPreferenceArray());
		// FastByIDMap<Long> itemTimestamps = new FastByIDMap<>();
		// for (SessionClicks click : session.getClicks()) {
		// itemTimestamps.put(click.getItemId(), click.getTimestamp()
		// .getTime());
		// timestamps.put(sessionId, itemTimestamps);
		// }
		// progress.stepIt();
		// }

		sessions.clear();
		sessions = null;

		this.buySessions = buySessions;

		// JavaRDD<SessionInfo> rddBuySessions = SparkHelper.sc
		// .parallelize(buySessions);
		// rddBuySessions.foreach(session ->
		// userData.put(session.getSessionId(),
		// session.toPreferenceArray()));

		// for (SessionInfo session : this.buySessions) {
		// int sessionId = session.getSessionId();
		//
		// userData.put(sessionId, session.toPreferenceArray());
		// FastByIDMap<Long> itemTimestamps = new FastByIDMap<>();
		// for (SessionClicks click : session.getClicks()) {
		// itemTimestamps.put(click.getItemId(), click.getTimestamp()
		// .getTime());
		// timestamps.put(sessionId, itemTimestamps);
		// }
		// }

		// dataModel = new GenericDataModel(userData, timestamps);

		InfoOutputHelper.printInfo("Done building data model.");

		InfoOutputHelper.printInfo("Start building recommender.");

		// recommender = new UserBasedRecomenderBuilder()
		// .buildRecommender(dataModel);

		InfoOutputHelper.printInfo("Done building recommender.");

	}

	public void evalute() throws TasteException {
		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
		double score = evaluator.evaluate(new UserBasedRecomenderBuilder(),
				null, dataModel, 0.8, 0.2);
		InfoOutputHelper.printInfo("Recommender RMS: " + score);
	}

	public Map<Integer, List<Integer>> getUserIntersect(
			TFIDFAnalysis itemWeights) throws TasteException {
		InfoOutputHelper.printInfo("Starting recommendation phase");

		JavaRDD<SessionInfo> rddBuySessions = SparkHelper.sc
				.parallelize(buySessions);

		final long sessionCount = rddBuySessions.count();
		final float threshold = 8f;
		progress = new ProgressMesurer(5, sessionCount);

		rddBuySessions.foreach(session -> {
			int sessionId = session.getSessionId();
			Set<Integer> items = new HashSet<Integer>();

			for (SessionClicks click : session.getClicks()) {
				int itemId = click.getItemId();
				double weight = itemWeights.getItemWeight(itemId);

				if (weight >= 0.5) {
					items.add(itemId);
				}

				if (items.size() > 3) {
					break;
				}
			}

			if (items.size() == 0) {
				items.add(session.getClicks().get(0).getItemId());
			}

			intersect.put(sessionId, new ArrayList<Integer>(items));
			// intersect.put(sessionId, new ArrayList<Integer>(items));

				// List<Integer> boughtItems =
				// session.getRecIntersect(recommender
				// .recommend(sessionId, 100, true));
				//
				// if (boughtItems != null) {
				// intersect.put(sessionId, boughtItems);
				// }

				// for (SessionClicks click : session.getClicks()) {
				// int itemId = click.getItemId();
				// float score = recommender.estimatePreference(sessionId,
				// itemId);
				//
				// if (Float.compare(score, threshold) > 0) {
				// List<Integer> boughtItems = intersect.get(sessionId);
				// if (boughtItems == null) {
				// boughtItems = new ArrayList<Integer>();
				// }
				// boughtItems.add(itemId);
				// System.out.printf("Session %d Item %d score %f\n",
				// sessionId, itemId, score);
				//
				// intersect.put(sessionId, boughtItems);
				// }
				// }

				progress.stepIt();
			});

		return intersect;
	}

	public void exportToFile(String filePath,
			Map<Integer, List<Integer>> buySessions) throws IOException {
		InfoOutputHelper.printInfo("Starting export phase");

		PrintWriter writer = new PrintWriter(filePath, "UTF-8");
		try {
			// File format is sessionId;boughtItem1,....,boughtItemN
			for (Entry<Integer, List<Integer>> buySession : buySessions
					.entrySet()) {
				String line = buySession.getKey() + ";";
				List<Integer> buyItems = buySession.getValue();

				Integer[] items = new Integer[buyItems.size()];

				int i = 0;
				for (Integer item : buySession.getValue()) {
					items[i] = item;
					i++;
				}
				line += StringUtils.join(items, ',');

				writer.println(line);
			}
		} finally {
			writer.close();
		}
	}
}
