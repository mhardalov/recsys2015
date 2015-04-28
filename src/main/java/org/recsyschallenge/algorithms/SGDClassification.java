package org.recsyschallenge.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;
import org.recsyschallenge.algorithms.enums.SessionEventType;
import org.recsyschallenge.algorithms.helpers.AlgorithmMesurer;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.models.SessionInfo;

import com.google.common.collect.Maps;

public class SGDClassification {
	private static final int FEATURES = 2000;

	Map<String, Set<Integer>> traceDictionary;
	FeatureVectorEncoder bias;
	FeatureVectorEncoder clickValues;
	FeatureVectorEncoder encoder;
	AdaptiveLogisticRegression learningAlgorithm;
	CrossFoldLearner model;

	private List<SessionInfo> buySessions;
	private AlgorithmMesurer mesurer;

	public SGDClassification(int interval, int avgWindow) {
		this.traceDictionary = new TreeMap<String, Set<Integer>>();

		this.encoder = new StaticWordValueEncoder("Strings");
		this.encoder.setProbes(2);
		this.encoder.setTraceDictionary(traceDictionary);

		this.clickValues = new ConstantValueEncoder("Clicks");
		this.bias = new ConstantValueEncoder("Intercept");
		this.bias.setTraceDictionary(traceDictionary);

		learningAlgorithm = new AdaptiveLogisticRegression(2, FEATURES,
				new L1());

		learningAlgorithm.setInterval(interval);
		learningAlgorithm.setAveragingWindow(avgWindow);

		this.buySessions = new ArrayList<SessionInfo>();

		this.mesurer = new AlgorithmMesurer();
	}

	private Vector profileToVector(SessionInfo session) {
		Vector v = new RandomAccessSparseVector(FEATURES);

		// TODO: add features
		int clickCount = session.getClicks().size();
		clickValues.addToVector("ClicksCount", clickCount, v);

		Map<String, Integer> clickedItems = session.getClickedItems();

		for (Entry<String, Integer> item : clickedItems.entrySet()) {
			encoder.addToVector(item.getKey(), (item.getValue() / clickCount),
					v);
		}

		// clickValues.addToVector("SessionLength",
		// Math.log(1 + session.getClickSessionLength()), v);
		//
		// clickValues.addToVector("AvgSessionLength",
		// Math.log(1 + session.getAvgSessionLength()), v);

		bias.addToVector("", 1, v);

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

		Vector p = new DenseVector(2000);
		model.classifyFull(p, v);
		SessionEventType estimated = SessionEventType
				.valueOf(p.maxValueIndex());

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

	public void train(final List<SessionInfo> trainClicks) throws IOException {
		InfoOutputHelper.printInfo("Starting train phase");

		try {
			int i = 0;
			int perc = 5;
			int clickCount = trainClicks.size();
			for (SessionInfo session : trainClicks) {
				Vector v = this.profileToVector(session);
				SessionEventType actual = session.hasBuys();

				this.mesurer.incSessoinCount(actual);
				learningAlgorithm.train(actual.ordinal(), v);
				i++;

				if ((int) (((float) i / clickCount) * 100) == perc) {
					InfoOutputHelper.printInfo(perc + "% done");
					perc += 5;
				}
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

		System.out.println();
	}

	public void dissect(List<SessionInfo> sessions) throws IOException {
		InfoOutputHelper.printInfo("Starting dissect phase");

		ModelDissector md = new ModelDissector();
		Map<String, Set<Integer>> traceDictionary = Maps.newTreeMap();
		encoder.setTraceDictionary(traceDictionary);
		clickValues.setTraceDictionary(traceDictionary);
		bias.setTraceDictionary(traceDictionary);

		Random rand = new Random();
		List<SessionInfo> subSessions = sessions;
		Collections.shuffle(subSessions, rand);

		for (SessionInfo session : subSessions) {
			// traceDictionary.clear();
			Vector v = this.profileToVector(session);
			md.update(v, traceDictionary, model);
		}

		for (ModelDissector.Weight w : md.summary(1000)) {
			System.out.printf("%s\t%f\t%d\n", w.getFeature(), w.getWeight(),
					w.getMaxImpact());
		}
	}

	public int classify(SessionInfo session) throws IOException {
		Vector v = this.profileToVector(session);

		Vector p = new DenseVector(2000);
		model.classifyFull(p, v);
		int estimated = p.maxValueIndex();

		return estimated;
	}

	public List<SessionInfo> getBuySessions() {
		return buySessions;
	}
}