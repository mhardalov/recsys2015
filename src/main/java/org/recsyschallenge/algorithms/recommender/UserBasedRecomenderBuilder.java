package org.recsyschallenge.algorithms.recommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class UserBasedRecomenderBuilder implements RecommenderBuilder {

	@Override
	public Recommender buildRecommender(DataModel dataModel)
			throws TasteException {
		Factorizer factorizer = new SVDPlusPlusFactorizer(dataModel, 50, 5);
		return new SVDRecommender(dataModel, factorizer);
//		ItemSimilarity sim = new EuclideanDistanceSimilarity(dataModel);
//		return new GenericItemBasedRecommender(dataModel, sim);
		
//		UserSimilarity sim = new EuclideanDistanceSimilarity(dataModel);
//		return new GenericUserBasedRecommender(dataModel,
//				new NearestNUserNeighborhood(2, sim, dataModel), sim);
	}

}
