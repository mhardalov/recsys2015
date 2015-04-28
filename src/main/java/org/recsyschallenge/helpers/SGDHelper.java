package org.recsyschallenge.helpers;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.recsyschallenge.models.SessionInfo;

public final class SGDHelper {
//	private static final int FEATURES = 80000;
//	
//	public static Map<String, Set<Integer>> traceDictionary;
//	public static FeatureVectorEncoder bias;
//	public static FeatureVectorEncoder clickValues;
//	public static FeatureVectorEncoder encoder;
//		
//	public static Vector profileToVector(SessionInfo session) {
//		Vector v = new RandomAccessSparseVector(FEATURES);
//
//		// TODO: add features
//		int clickCount = session.getClicks().size();
//		clickValues.addToVector("", clickCount, v);
//
//		Map<String, Integer> clickedItems = session.getClickedItems();
//
//		for (Entry<String, Integer> item : clickedItems.entrySet()) {
//			encoder.addToVector(item.getKey(),
//					Math.log(1 + (item.getValue() / clickCount)), v);
//		}
//
//		clickValues.addToVector("",
//				Math.log(1 + session.getClickSessionLength()), v);
//
//		bias.addToVector("", 1, v);
//
//		return v;
//	}
}