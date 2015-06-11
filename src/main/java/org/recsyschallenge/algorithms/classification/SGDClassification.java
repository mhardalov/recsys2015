package org.recsyschallenge.algorithms.classification;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.mahout.classifier.sgd.AdaptiveLogisticRegression;
import org.apache.mahout.classifier.sgd.CrossFoldLearner;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.ModelDissector;
import org.apache.mahout.classifier.sgd.ModelSerializer;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;
import org.recsyschallenge.algorithms.classification.builders.SGDClassificationBuilder;
import org.recsyschallenge.algorithms.enums.SessionEventType;
import org.recsyschallenge.algorithms.helpers.AlgorithmMesurer;
import org.recsyschallenge.algorithms.helpers.ProgressMesurer;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.models.SessionClicks;
import org.recsyschallenge.models.SessionInfo;

import com.google.common.collect.Maps;

public class SGDClassification {
	private final SGDClassificationBuilder builder;
	private final TFIDFAnalysis itemWeights;

	Map<String, Set<Integer>> traceDictionary;
	FeatureVectorEncoder bias;
	FeatureVectorEncoder clickValues;
	FeatureVectorEncoder avgSessionLength;
	FeatureVectorEncoder clickSessionLength;
	FeatureVectorEncoder specialOffers;
	FeatureVectorEncoder notCategorizedItems;
	FeatureVectorEncoder categoriesCount;
	FeatureVectorEncoder productCount;
	FeatureVectorEncoder productsItemsCount;
	FeatureVectorEncoder encoder;
	FeatureVectorEncoder itemsEncoder;
	FeatureVectorEncoder productItemsEncoder;
	FeatureVectorEncoder repeatedItems;
	FeatureVectorEncoder avgItemDistance;
	FeatureVectorEncoder itemDistance;
	FeatureVectorEncoder itemsBuyProp;
	FeatureVectorEncoder itemCategoryWeight;
	FeatureVectorEncoder uniqueClicksRatio;

	OnlineLogisticRegression learningAlgorithm;
	// CrossFoldLearner model;
	ModelDissector md;

	private List<SessionInfo> buySessions;
	private AlgorithmMesurer mesurer;

	@SuppressWarnings("resource")
	public SGDClassification(SGDClassificationBuilder builder,
			TFIDFAnalysis itemWeights) {
		this.builder = builder;
		this.itemWeights = itemWeights;

		this.traceDictionary = new TreeMap<String, Set<Integer>>();

		this.encoder = new StaticWordValueEncoder("Strings");
		// this.encoder.setProbes(2);
		this.encoder.setTraceDictionary(traceDictionary);

		this.clickValues = new ConstantValueEncoder("Clicks");
		this.clickValues.setTraceDictionary(traceDictionary);

		this.avgSessionLength = new ConstantValueEncoder("TimesAvg");
		this.avgSessionLength.setTraceDictionary(traceDictionary);

		this.clickSessionLength = new ConstantValueEncoder("TimesSession");
		this.clickSessionLength.setTraceDictionary(traceDictionary);

		this.specialOffers = new ConstantValueEncoder("SpecialOffers");
		this.specialOffers.setTraceDictionary(traceDictionary);

		this.notCategorizedItems = new ConstantValueEncoder(
				"NotCategorizedItems");
		this.notCategorizedItems.setTraceDictionary(traceDictionary);

		this.categoriesCount = new ConstantValueEncoder("CategoriesCount");
		this.categoriesCount.setTraceDictionary(traceDictionary);

		this.productCount = new ConstantValueEncoder("ProductCount");
		this.productCount.setTraceDictionary(traceDictionary);

		this.productsItemsCount = new ConstantValueEncoder("ProductsItemsCount");
		this.productsItemsCount.setTraceDictionary(traceDictionary);

		this.itemsEncoder = new StaticWordValueEncoder("Items");
		this.itemsEncoder.setTraceDictionary(traceDictionary);

		this.productItemsEncoder = new StaticWordValueEncoder(
				"ProductItemsEncoder");
		this.productItemsEncoder.setTraceDictionary(traceDictionary);

		this.repeatedItems = new ConstantValueEncoder("RepeatedItems");
		this.repeatedItems.setTraceDictionary(traceDictionary);

		this.itemsBuyProp = new ConstantValueEncoder("ItemsBuyProp");
		this.itemsBuyProp.setTraceDictionary(traceDictionary);

		this.itemCategoryWeight = new StaticWordValueEncoder(
				"ItemCategoryWeight");
		this.itemCategoryWeight.setTraceDictionary(traceDictionary);

		this.uniqueClicksRatio = new ConstantValueEncoder("UniqueClicksRatio");
		this.uniqueClicksRatio.setTraceDictionary(traceDictionary);

		// this.avgItemDistance = new ConstantValueEncoder("AvgItemDistance");
		// this.avgItemDistance.setTraceDictionary(traceDictionary);
		//
		// this.itemDistance = new ConstantValueEncoder("ItemDistance");
		// this.itemDistance.setTraceDictionary(traceDictionary);

		this.bias = new ConstantValueEncoder("Intercept");
		this.bias.setTraceDictionary(traceDictionary);

		// learningAlgorithm = new AdaptiveLogisticRegression(2,
		// builder.getFeatures(), new L1());
		learningAlgorithm = new OnlineLogisticRegression(2,
				builder.getFeatures(), new L1()).alpha(1).stepOffset(1000)
				.decayExponent(0.9).lambda(3.0e-5).learningRate(20);
		// learningAlgorithm.setInterval(builder.getInterval());
		// learningAlgorithm.setAveragingWindow(builder.getAvgWindow());
		// learningAlgorithm.setThreadCount(100);

		this.buySessions = new ArrayList<SessionInfo>();

		this.mesurer = new AlgorithmMesurer();
	}

	private Vector profileToVector(SessionInfo session) {
		Vector v = new RandomAccessSparseVector(builder.getFeatures());

		// TODO: add features
		int clickCount = session.getClicks().size();
		clickValues.addToVector((byte[]) null, Math.log(clickCount), v);

//		double sessionLength = session.getClickSessionLength();
		// TODO: change time interval
//		this.clickSessionLength.addToVector((byte[]) null,
//				Math.log(1 + sessionLength / clickCount), v);
		//
		// double avgSessionLength = session.getAvgSessionLength();
		// / (60 * 1000F);
		// this.avgSessionLength.addToVector((byte[]) null, avgSessionLength,
		// v);

		Map<String, Integer> clickedItemsByCat = session.getClickedItems();

		long productsCount = 0;
		int normProductItemsCount = 0;
		for (Entry<String, Integer> item : clickedItemsByCat.entrySet()) {
			String category = item.getKey();
			int itemCount = item.getValue();
			float normItemCount = (float) itemCount;

			double catWeight = itemWeights.getCategoryWeight(category);

			if (category.length() > 3) {
				productsCount++;
				normProductItemsCount += normItemCount * catWeight;

			} else {
				switch (category) {
				case "0":
					// this.notCategorizedItems.addToVector((byte[]) null,
					// normItemCount * catWeight, v);
					break;
				case "S":
					// this.specialOffers.addToVector((byte[]) null,
					// normItemCount
					// * catWeight, v);
					break;
				}
			}

			if (catWeight > 0) {
				encoder.addToVector(category, normItemCount * catWeight, v);
			}
		}

		encoder.addToVector("product", normProductItemsCount, v);
		categoriesCount.addToVector((byte[]) null,
				Math.log(clickedItemsByCat.size()), v);
		productCount.addToVector((byte[]) null,
				((float) productsCount / clickCount), v);
		productsItemsCount.addToVector((byte[]) null,
				((float) normProductItemsCount), v);

		List<SessionClicks> clicks = session.getClicks();
		int repeatedItems = 0;
		int maxItemId = 0;
		int minItemId = Integer.MAX_VALUE;
		// float lastClickTime = (float) session.getClicks()
		// .get(session.getClicks().size() - 1).getTimestamp().getTime()
		// / (60 * 1000F);

		Map<Integer, Integer> uniqueClicksInSession = new HashMap<Integer, Integer>();

		for (int i = 0; i < clicks.size(); i++) {
			SessionClicks click = clicks.get(i);
			int itemId = click.getItemId();
			String category = click.getCategory();

			// float clickTime = (float) click.getTimestamp().getTime()
			// / (60 * 1000F);
			//
			// double timeNorm;
			// if (clickCount <= 1 || sessionLenth == 0) {
			// timeNorm = 1;
			// } else {
			// timeNorm = (avgSessionLength - clickTime) / sessionLenth;
			//
			// if (timeNorm == 0) {
			// timeNorm = 0.01;
			// }
			// }

			// this.itemsEncoder.addToVector("time:" + String.valueOf(itemId),
			// lastClickTime - clickTime / sessionLength, v);

			// this.itemsEncoder.addToVector("pos:" + String.valueOf(itemId),
			// (0.5 - ((float) clickCount / 2 - i) / clickCount), v);

			Integer clickByIdCount = uniqueClicksInSession.get(itemId);
			if (clickByIdCount == null) {
				clickByIdCount = 0;
			} else {

				// Each item must be counted once!
				if (clickByIdCount == 1) {
					repeatedItems++;
				}
			}

			clickByIdCount++;
			uniqueClicksInSession.put(click.getItemId(), clickByIdCount);

			double itemWeight = itemWeights.getItemWeight(itemId);
			double catWeight = itemWeights.getCategoryWeight(category);

			// this.itemCategoryWeight.addToVector("ic:" + itemId, itemWeight
			// * catWeight, v);

			if (maxItemId < itemId) {
				maxItemId = itemId;
			}

			if (minItemId > itemId) {
				minItemId = itemId;
			}

			// if (i < clicks.size() - 1) {
			// avgDistance += Math.abs(click.getItemId()
			// - clicks.get(i + 1).getItemId());
			// this.itemsEncoder.addToVector(
			// "itemDist:" + i,
			// , v);
			// }
		}

		// if (clickCount > 1) {
		// double avgDistNorm = Math.log(1 + (double) avgDistance
		// / (maxItemId - minItemId));
		//
		// this.avgItemDistance.addToVector((byte[]) null, avgDistNorm, v);
		// }
		//
		// double itemDistanceNorm = Math.log(1 + (double) (maxItemId -
		// minItemId)
		// / avgDistance);
		// this.itemDistance.addToVector((byte[]) null, itemDistanceNorm, v);

		this.repeatedItems.addToVector((byte[]) null, (float) repeatedItems
				/ uniqueClicksInSession.size(), v);

		this.uniqueClicksRatio.addToVector((byte[]) null,
				(float) uniqueClicksInSession.size() / clickCount, v);

		// TFIDF tfidf = new TFIDF();
		double prop = 0;
		for (Entry<Integer, Integer> entry : uniqueClicksInSession.entrySet()) {
			int itemId = entry.getKey();
			int count = entry.getValue();

			double itemWeight = itemWeights.getItemWeight(itemId);
			if (itemWeight > 0) {
				prop += (itemWeight * count);
				// Math.log(1 + tfIdfValue)
				this.itemsEncoder.addToVector(String.valueOf(itemId),
						itemWeight * count, v);
			}
		}

		this.itemsBuyProp.addToVector((byte[]) null, Math.log(1 + prop), v);

		bias.addToVector((byte[]) null, 1, v);

		return v;
	}

	private void testEvalutaion(int actual, Vector v, int k, int correct,
			int estimated) {
		double averageLL = 0.0;
		double averageCorrect = 0.0;

		double step = 0.0;
		int[] bumps = new int[] { 1, 2, 5 };

		double mu = Math.min(k + 1, 200);
		double ll = learningAlgorithm.logLikelihood(actual, v);
		averageLL = averageLL + (ll - averageLL) / mu;

		averageCorrect = averageCorrect + (correct - averageCorrect) / mu;

		int bump = bumps[(int) Math.floor(step) % bumps.length];
		int scale = (int) Math.pow(10, Math.floor(step / bumps.length));
		if (k % (bump * scale) == 0) {
			step += 0.25;

			System.out.printf("%10d %10.3f %10.3f %10.2f %s %s\n", k, ll,
					averageLL, averageCorrect * 100, actual, estimated);
		}
	}

	private boolean test(Vector v, Integer og, int k, boolean extendedOuput) {
		SessionEventType actual = SessionEventType.valueOf(og);

		int estematedInt = this.classify(v);
		SessionEventType estimated = SessionEventType.valueOf(estematedInt);

		boolean correct = (estimated == actual);

		this.mesurer.incSessoinCount(actual);

		if (correct) {
			this.mesurer.incGuessedCount(estimated);
		}

		if (extendedOuput) {
			this.testEvalutaion(og, v, correct ? 1 : 0, k, estimated.ordinal());
		}

		return (correct && estimated == SessionEventType.BuyEvent);
	}

	private int classify(Vector v) {
		Vector p = new DenseVector(2);
		learningAlgorithm.classifyFull(p, v);
		int estimated = p.maxValueIndex();

		return estimated;
	}

	public void train(final List<SessionInfo> trainClicks) throws IOException {
		InfoOutputHelper.printInfo("Starting train phase");

		try {
			int clickCount = trainClicks.size();

			ProgressMesurer progress = new ProgressMesurer(5, clickCount);
			for (SessionInfo session : trainClicks) {
				SessionEventType actual = session.hasBuys();
				Vector v = this.profileToVector(session);

				// Upsampling for minority class. Adding twice minority class
				for (int i = actual == SessionEventType.BuyEvent ? this.builder
						.getUpSamplingRatio() : 1; i > 0; i--) {

					this.mesurer.incSessoinCount(actual);
					learningAlgorithm.train(actual.ordinal(), v);
				}

				progress.stepIt();
			}

			// this.model =
			// learningAlgorithm.getBest().getPayload().getLearner();

			InfoOutputHelper.printInfo("Buys: "
					+ this.mesurer.getBuySessionsCount() + "/OnlyClicks:"
					+ this.mesurer.getClickSessionsCount());
			System.out.println();
		} finally {
			learningAlgorithm.close();
		}
	}

	public void test(List<SessionInfo> testSessions, boolean extendedOuput)
			throws IOException {
		InfoOutputHelper.printInfo("Starting test phase");

		int testSessionSize = testSessions.size();
		this.mesurer.setTestCount(testSessionSize);
		int k = 0;
		for (SessionInfo session : testSessions) {
			Vector v = this.profileToVector(session);
			k++;
			if (this.test(v, session.hasBuys().ordinal(), k, extendedOuput)) {
				buySessions.add(session);
			}
		}

		InfoOutputHelper.printInfo("Buys: "
				+ this.mesurer.getBuySessionsCount() + "/OnlyClicks:"
				+ this.mesurer.getClickSessionsCount());

		InfoOutputHelper.printInfo("Guessed Buys: "
				+ this.mesurer.getGuessedBuys() + "/ Guessed Clicks:"
				+ this.mesurer.getGuessedClicks());

		InfoOutputHelper.printInfo("Guessed percentage: "
				+ this.mesurer.getGuessedPercent() + "% ("
				+ this.mesurer.getGuessed() + "/" + testSessionSize + ")");

		// InfoOutputHelper.printInfo("AUC: " + learningAlgorithm.auc());
		// InfoOutputHelper.printInfo("Correct: " + model.percentCorrect());
		// InfoOutputHelper.printInfo("LogLikehood: " +
		// model.getLogLikelihood());
		// InfoOutputHelper.printInfo("Record: " + model.getRecord());
		// InfoOutputHelper.printInfo("Features: " + model.getNumFeatures());

		System.out.println();
	}

	public void saveResults(String filePath, int testSessionSize)
			throws FileNotFoundException, UnsupportedEncodingException {
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
		String fileName = filePath + "/Classification-" + sdf.format(d)
				+ ".txt";

		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		writer.println("Upsampling on buys: " + builder.getUpSamplingRatio());
		writer.println("BuysCount: " + builder.getBuysCount());
		writer.println("Clicks ratio: " + builder.getClicksRatio());
		writer.println("Interval: " + builder.getInterval());
		writer.println("AvgWindow: " + builder.getAvgWindow());

		writer.println();
		writer.println("Buys: " + this.mesurer.getBuySessionsCount()
				+ "/OnlyClicks:" + this.mesurer.getClickSessionsCount());

		writer.println("Guessed Buys: " + this.mesurer.getGuessedBuys()
				+ "/ Guessed Clicks:" + this.mesurer.getGuessedClicks());

		writer.println("Guessed percentage: "
				+ this.mesurer.getGuessedPercent() + "% ("
				+ this.mesurer.getGuessed() + "/" + testSessionSize + ")");

		// writer.println("AUC: " + model.auc());
		// writer.println("Correct: " + model.percentCorrect());
		// writer.println("LogLikehood: " + model.getLogLikelihood());
		// writer.println("Record: " + model.getRecord());
		// writer.println("Features: " + model.getNumFeatures());

		writer.println();
		writer.println("Incuded Features:");
		for (String feature : this.traceDictionary.keySet()) {
			writer.println(feature);
		}

		writer.println();
		writer.println("Information Gain:");
		for (ModelDissector.Weight w : md.summary(builder.getFeatures())) {
			writer.printf("%s\t%f\t%d\n", w.getFeature(), w.getWeight(),
					w.getMaxImpact());
		}

		writer.close();
	}

	public void dissect(List<SessionInfo> sessions) throws IOException {
		InfoOutputHelper.printInfo("Starting dissect phase");

		md = new ModelDissector();
		Map<String, Set<Integer>> traceDictionary = Maps.newTreeMap();
		this.encoder.setTraceDictionary(traceDictionary);
		this.clickValues.setTraceDictionary(traceDictionary);
		this.bias.setTraceDictionary(traceDictionary);

		this.avgSessionLength.setTraceDictionary(traceDictionary);
		this.clickSessionLength.setTraceDictionary(traceDictionary);
		this.specialOffers.setTraceDictionary(traceDictionary);
		this.notCategorizedItems.setTraceDictionary(traceDictionary);
		this.categoriesCount.setTraceDictionary(traceDictionary);
		this.productCount.setTraceDictionary(traceDictionary);
		this.productsItemsCount.setTraceDictionary(traceDictionary);
		this.itemsEncoder.setTraceDictionary(traceDictionary);

		Random rand = new Random();
		List<SessionInfo> subSessions = sessions;
		Collections.shuffle(subSessions, rand);

		for (SessionInfo session : subSessions) {
			traceDictionary.clear();
			Vector v = this.profileToVector(session);
			md.update(v, traceDictionary, learningAlgorithm);
		}

		for (ModelDissector.Weight w : md.summary(builder.getFeatures())) {
			System.out.printf("%s\t%f\t%d\n", w.getFeature(), w.getWeight(),
					w.getMaxImpact());
		}
	}

	public List<SessionInfo> classify(List<SessionInfo> sessions) {
		InfoOutputHelper.printInfo("Starting classification phase");
		List<SessionInfo> buySessions = new ArrayList<SessionInfo>();

		for (SessionInfo session : sessions) {
			Vector v = this.profileToVector(session);
			int estematedInt = this.classify(v);
			SessionEventType estimated = SessionEventType.valueOf(estematedInt);

			if (estimated == SessionEventType.BuyEvent) {
				buySessions.add(session);
			}
		}

		InfoOutputHelper.printInfo("Buy sessions: " + buySessions.size() + "/"
				+ sessions.size());

		return buySessions;
	}

	public List<SessionInfo> getBuySessions() {
		return buySessions;
	}

	public void saveModel(String path) throws IOException {
		// if (this.learningAlgorithm != null) {
		// ModelSerializer.writeBinary(path + "best.model",
		// this.learningAlgorithm);
		// }
		ModelSerializer.writeBinary(path + "complete.model",
				this.learningAlgorithm);

	}

	public void loadModel(String path) throws FileNotFoundException,
			IOException {
		this.learningAlgorithm = ModelSerializer.readBinary(
				new FileInputStream(path + "complete.model"),
				OnlineLogisticRegression.class);
		// this.model = ModelSerializer.readBinary(new FileInputStream(path
		// + "best.model"), CrossFoldLearner.class);
		this.learningAlgorithm.close();

	}
}