package org.recsyschallenge.algorithms.recommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

public class UserBasedRecomenderBuilder implements RecommenderBuilder {

	@Override
	public Recommender buildRecommender(DataModel dataModel)
			throws TasteException {
		Factorizer factorizer = new SVDPlusPlusFactorizer(dataModel, 50, 5);
		// Factorizer factorizer = new ParallelSGDFactorizer(dataModel, 1000,
		// 0.01, 10);
		// Factorizer factorizer = new ALSWRFactorizer(dataModel, 100, 0.5, 10);
		return new SVDRecommender(dataModel, factorizer);
		// ItemSimilarity sim = new PearsonCorrelationSimilarity(dataModel);
		// return new GenericItemBasedRecommender(dataModel, sim);

		// UserSimilarity sim = new EuclideanDistanceSimilarity(dataModel);
		// UserSimilarity sim = new PearsonCorrelationSimilarity(dataModel,
		// Weighting.WEIGHTED);
		// return new GenericUserBasedRecommender(dataModel,
		// new NearestNUserNeighborhood(10, sim, dataModel), sim);
	}
}
