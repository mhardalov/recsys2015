package org.recsyschallenge.algorithms.recommender;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.models.SessionClicks;
import org.recsyschallenge.models.SessionInfo;

public class UserBasedRecomender {
	GenericDataModel dataModel;
	Recommender recommender;

	public UserBasedRecomender(List<SessionInfo> sessions,
			List<SessionInfo> buySessions) throws IOException, TasteException {
		InfoOutputHelper.printInfo("Starting building recommendation phase");

		FastByIDMap<PreferenceArray> userData = new FastByIDMap<>();
		// FastByIDMap<Long> timestamps = new FastByIDMap<>();

		for (SessionInfo session : sessions) {

			if (session.getClicks().size() == 0) {
				continue;
			}

			int userId = session.getSessionId();

			List<GenericPreference> items = new ArrayList<>();
			for (SessionClicks click : session.getClicks()) {
				int itemId = click.getItemId();
				items.add(new GenericPreference(userId, itemId,
						(float) (session.isItemBought(itemId) ? 1 : 0)));
			}

			userData.put(userId, new GenericUserPreferenceArray(items));
		}
		sessions.clear();

		for (SessionInfo session : buySessions) {
			if (session.getClicks().size() == 0) {
				continue;
			}

			int userId = session.getSessionId();

			List<GenericPreference> items = new ArrayList<>();
			for (SessionClicks click : session.getClicks()) {
				int itemId = click.getItemId();
				items.add(new GenericPreference(userId, itemId,
						(float) (session.isItemBought(itemId) ? 1 : 0)));
			}

			userData.put(userId, new GenericUserPreferenceArray(items));
		}		
		
		dataModel = new GenericDataModel(userData);
		
		InfoOutputHelper.printInfo("Done building data model.");
		
		InfoOutputHelper.printInfo("Start building recommender.");

		recommender = new UserBasedRecomenderBuilder()
				.buildRecommender(dataModel);

		InfoOutputHelper.printInfo("Done building recommender.");
//		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
//		double score = evaluator.evaluate(new UserBasedRecomenderBuilder(),
//				null, dataModel, 0.9, 0.1);
//		InfoOutputHelper.printInfo("Recommender RMS: " + score);
	}

	public List<RecommendedItem> recommendUser(long userId, int howMany)
			throws TasteException {
		List<RecommendedItem> recommended = recommender.recommend(userId,
				howMany, true);

		return recommended;
	}

	public Map<Integer, Set<Integer>> getUserIntersect(
			List<SessionInfo> buySessions) throws TasteException {
		InfoOutputHelper.printInfo("Starting recommendation phase");

		Map<Integer, Set<Integer>> intersect = new HashMap<Integer, Set<Integer>>();

		// TODO: optimize
		int i = 0;
		int perc = 5;
		int clickCount = buySessions.size();
		for (SessionInfo session : buySessions) {
			int sessionId = session.getSessionId();

			List<RecommendedItem> recommendations = this.recommendUser(
					sessionId, 20);

			for (SessionClicks click : session.getClicks()) {
				int clickItemId = click.getItemId();

				for (RecommendedItem item : recommendations) {
					if (item.getValue() > 0 && item.getItemID() == clickItemId) {
						Set<Integer> boughtItems = intersect.get(clickItemId);
						if (boughtItems == null) {
							boughtItems = new HashSet<Integer>();
						}

						boughtItems.add(clickItemId);
						intersect.put(sessionId, boughtItems);

						break;
					}
				}
			}
			i++;

			if ((int) (((float) i / clickCount) * 100) == perc) {
				InfoOutputHelper.printInfo(perc + "% done");
				perc += 5;
			}
		}

		return intersect;
	}

	public void exportToFile(String filePath,
			Map<Integer, Set<Integer>> buySessions) throws IOException {
		InfoOutputHelper.printInfo("Starting export phase");

		PrintWriter writer = new PrintWriter(filePath, "UTF-8");
		try {
			// File format is sessionId;boughtItem1,....,boughtItemN
			for (Entry<Integer, Set<Integer>> buySession : buySessions
					.entrySet()) {
				String line = buySession.getKey() + ";";
				Set<Integer> buyItems = buySession.getValue();

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
