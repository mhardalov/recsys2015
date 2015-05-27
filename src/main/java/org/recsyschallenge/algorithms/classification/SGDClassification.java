package org.recsyschallenge.algorithms.classification;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;
import org.recsyschallenge.algorithms.enums.SessionEventType;
import org.recsyschallenge.algorithms.helpers.AlgorithmMesurer;
import org.recsyschallenge.algorithms.helpers.ProgressMesurer;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.models.SessionClicks;
import org.recsyschallenge.models.SessionInfo;

import com.google.common.collect.Maps;

public class SGDClassification {
	private static final int FEATURES = 30;

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

	AdaptiveLogisticRegression learningAlgorithm;
	CrossFoldLearner model;

	private List<SessionInfo> buySessions;
	private AlgorithmMesurer mesurer;
	public Map<String, Integer> boughtByCat;

	public SGDClassification(int interval, int avgWindow) {
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

		this.bias = new ConstantValueEncoder("Intercept");
		this.bias.setTraceDictionary(traceDictionary);

		learningAlgorithm = new AdaptiveLogisticRegression(2, FEATURES,
				new L1());

		learningAlgorithm.setInterval(interval);
		learningAlgorithm.setAveragingWindow(avgWindow);
		learningAlgorithm.setThreadCount(100);

		this.buySessions = new ArrayList<SessionInfo>();

		this.mesurer = new AlgorithmMesurer();
		
		this.boughtByCat = new HashMap<String, Integer>();
	}

	private Vector profileToVector(SessionInfo session) {
		Vector v = new RandomAccessSparseVector(FEATURES);

		// TODO: add features
		int clickCount = session.getClicks().size();
		clickValues.addToVector((byte[]) null, clickCount * 0.248301, v);

		this.clickSessionLength.addToVector((byte[]) null,
				session.getClickSessionLength() * 0.056515, v);

		this.avgSessionLength.addToVector((byte[]) null,
				session.getAvgSessionLength() * 0.211162, v);

		for (SessionClicks click : session.getClicks()) {

			if (session.isItemBought(click.getItemId())) {
				String cat = click.getCategory();
				Integer count = this.boughtByCat.get(cat);
				
				if (count == null) {
					count = 0;
 				}
				
				count++;
				this.boughtByCat.put(cat, count);
			}
		}

		Map<String, Integer> clickedItems = session.getClickedItems();

		long productsCount = 0;
		int productItemsCount = 0;
		for (Entry<String, Integer> item : clickedItems.entrySet()) {
			String category = item.getKey();
			int itemCount = item.getValue();
			float normItemCount = (float) itemCount / clickCount;

			if (category.length() > 3) {
				productsCount++;
				productItemsCount += itemCount;
				encoder.addToVector("product", normItemCount * 0.474446, v);
			} else {
				float booster = 1.0f;
				switch (category) {
				case "0":
					this.notCategorizedItems.addToVector((byte[]) null,
							normItemCount * 2, v);
					booster = 2f;
					break;
				case "1":
					booster = 1.6f;
					break;
				case "2":
					booster = 1.4f;
					break;
				case "3":
					booster = 1.2f;
					break;
				case "4":
					booster = 0.8f;
					break;
				case "5":
					booster = 1.3f;
					break;
				case "6":
					booster = 0.6f;
					break;
				case "7":
					booster = 0.7f;
					break;
				case "8":
					booster = 0.3f;
					break;
				case "9":
					booster = 0.2f;
					break;
				case "10":
					booster = 0.4f;
					break;
				case "11":
					booster = 0.1f;
					break;
				case "12":
					booster = 0.05f;
					break;
				case "S":
					this.specialOffers.addToVector((byte[]) null,
							normItemCount * 1.8, v);
					booster = 1.8f;
					break;
				}

				encoder.addToVector(category, normItemCount * booster, v);
			}
		}

		this.categoriesCount.addToVector((byte[]) null,
				clickedItems.size() * 0.314772, v);
		this.productCount.addToVector((byte[]) null,
				((float) productsCount / clickedItems.size()) * 0.314772, v);
		this.productsItemsCount.addToVector((byte[]) null,
				((float) productItemsCount / clickCount) * 1.053067, v);

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
		double ll = model.logLikelihood(actual, v);
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
		Vector p = new DenseVector(FEATURES);
		model.classifyFull(p, v);
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

				// Upsampling for minority class. Adding twice minority class
				for (int i = actual == SessionEventType.BuyEvent ? 3 : 1; i > 0; i--) {
					Vector v = this.profileToVector(session);

					this.mesurer.incSessoinCount(actual);
					learningAlgorithm.train(actual.ordinal(), v);
				}

				progress.stepIt();
			}

			this.model = learningAlgorithm.getBest().getPayload().getLearner();

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

		InfoOutputHelper.printInfo("AUC: " + model.auc());
		InfoOutputHelper.printInfo("Correct: " + model.percentCorrect());
		InfoOutputHelper.printInfo("LogLikehood: " + model.getLogLikelihood());
		InfoOutputHelper.printInfo("Record: " + model.getRecord());
		InfoOutputHelper.printInfo("Features: " + model.getNumFeatures());

		System.out.println();
	}

	public void dissect(List<SessionInfo> sessions) throws IOException {
		InfoOutputHelper.printInfo("Starting dissect phase");

		ModelDissector md = new ModelDissector();
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

		Random rand = new Random();
		List<SessionInfo> subSessions = sessions;
		Collections.shuffle(subSessions, rand);

		for (SessionInfo session : subSessions) {
			traceDictionary.clear();
			Vector v = this.profileToVector(session);
			md.update(v, traceDictionary, model);
		}

		for (ModelDissector.Weight w : md.summary(1000)) {
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
		if (this.learningAlgorithm.getBest() != null) {
			ModelSerializer.writeBinary(path + "best.model",
					this.learningAlgorithm.getBest().getPayload().getLearner());
		}
		ModelSerializer.writeBinary(path + "complete.model",
				this.learningAlgorithm);

	}

	public void loadModel(String path) throws FileNotFoundException,
			IOException {
		this.learningAlgorithm = ModelSerializer.readBinary(
				new FileInputStream(path + "complete.model"),
				AdaptiveLogisticRegression.class);
		this.model = ModelSerializer.readBinary(new FileInputStream(path
				+ "best.model"), CrossFoldLearner.class);
		this.learningAlgorithm.close();

	}
}