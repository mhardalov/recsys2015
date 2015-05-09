package org.recsyschallenge.algorithms.recommender;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.spark.api.java.JavaRDD;
import org.recsyschallenge.algorithms.helpers.ProgressMesurer;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.helpers.SparkHelper;
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
		// FastByIDMap<Long> timestamps = new FastByIDMap<>();

		// JavaRDD<SessionInfo> rddSessions =
		// SparkHelper.sc.parallelize(sessions);
		// rddSessions.foreach(session -> userData.put(session.getSessionId(),
		// session.toPreferenceArray()));

		progress = new ProgressMesurer(5, sessions.size());

		for (SessionInfo session : sessions) {
			userData.put(session.getSessionId(), session.toPreferenceArray());
			progress.stepIt();
		}

		sessions.clear();
		sessions = null;

		this.buySessions = buySessions;

		// JavaRDD<SessionInfo> rddBuySessions = SparkHelper.sc
		// .parallelize(buySessions);
		// rddBuySessions.foreach(session ->
		// userData.put(session.getSessionId(),
		// session.toPreferenceArray()));

		for (SessionInfo session : this.buySessions) {
			userData.put(session.getSessionId(), session.toPreferenceArray());
		}

		dataModel = new GenericDataModel(userData);

		InfoOutputHelper.printInfo("Done building data model.");

		InfoOutputHelper.printInfo("Start building recommender.");

		recommender = new UserBasedRecomenderBuilder()
				.buildRecommender(dataModel);

		InfoOutputHelper.printInfo("Done building recommender.");

	}

	public void evalute() throws TasteException {
		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
		double score = evaluator.evaluate(new UserBasedRecomenderBuilder(),
				null, dataModel, 0.9, 0.1);
		InfoOutputHelper.printInfo("Recommender RMS: " + score);
	}

	public Map<Integer, List<Integer>> getUserIntersect() throws TasteException {
		InfoOutputHelper.printInfo("Starting recommendation phase");

		JavaRDD<SessionInfo> rddBuySessions = SparkHelper.sc
				.parallelize(buySessions);

		final long sessionCount = rddBuySessions.count();

		progress = new ProgressMesurer(5, sessionCount);

		rddBuySessions.foreach(session -> {
			int sessionId = session.getSessionId();
			List<RecommendedItem> recommendations = recommender.recommend(
					sessionId, 20, true);
			List<Integer> items = session.getRecIntersect(recommendations);
			if (items != null) {
				intersect.put(sessionId, items);
			}

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
