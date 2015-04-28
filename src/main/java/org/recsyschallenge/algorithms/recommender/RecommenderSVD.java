package org.recsyschallenge.algorithms.recommender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

public class RecommenderSVD {
	GenericDataModel dataModel;
	Recommender recommender;

	public RecommenderSVD(List<SessionInfo> sessions) throws IOException,
			TasteException {
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

		dataModel = new GenericDataModel(userData);

		recommender = new SVDRecommenderBuilder().buildRecommender(dataModel);

		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
		double score = evaluator.evaluate(new SVDRecommenderBuilder(), null,
				dataModel, 0.95, 0.05);
		InfoOutputHelper.printInfo("Recommender RMS: " + score);
	}

	public List<RecommendedItem> recommendUser(long userId, int howMany)
			throws TasteException {
		List<RecommendedItem> recommended = recommender.recommend(userId,
				howMany, true);

		return recommended;
	}
}
