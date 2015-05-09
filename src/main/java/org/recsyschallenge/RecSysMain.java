package org.recsyschallenge;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.spark.api.java.JavaRDD;
import org.recsyschallenge.algorithms.classification.SGDClassification;
import org.recsyschallenge.algorithms.recommender.UserBasedRecomender;
import org.recsyschallenge.helpers.FilesParserHelper;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.helpers.SparkHelper;
import org.recsyschallenge.models.SessionInfo;

public class RecSysMain {

	// -XX:+UseParallelGC -XX:+UseParallelOldGC
	private static final float ratio = (float) 8;
	private static final String TEST_FILE = "yoochoose-test.dat";
	private static final String CLICKS_FILE = "yoochoose-clicks.dat";
	private static final String BUYS_FILE = "yoochoose-buys.dat";
	private static final String OUTPUT_FILE = "yoochoose.output";

	private static SGDClassification getClassifier() {
		return new SGDClassification(8000, 5000);
	}

	private static SGDClassification loadModelFromFile(String path)
			throws FileNotFoundException, IOException {
		SGDClassification classification = getClassifier();
		classification.loadModel(path);

		return classification;
	}

	private SGDClassification trainClassifier(String clicksFile,
			String buysFile, String modelFile) throws ParseException,
			IOException {
		// Max buys 509696
		FilesParserHelper parser = FilesParserHelper.newInstance(clicksFile,
				buysFile, 509696, ratio);

		Map<Integer, SessionInfo> sessions;
		List<SessionInfo> sessionsList;

		try {
			sessions = parser.parseSessions();

			sessionsList = new ArrayList<SessionInfo>(sessions.values());
		} finally {
			parser.dispose();
			parser = null;
		}

		int sessionsSize = sessionsList.size();

		Collections.shuffle(sessionsList, new SecureRandom());

		// 10% of all data
		int testRecords = (int) (sessionsSize * 0.1);
		List<SessionInfo> test = sessionsList.subList(0, testRecords);
		List<SessionInfo> train = sessionsList.subList(testRecords,
				sessionsSize);

		SGDClassification classification = getClassifier();
		classification.train(train);
		testClassifier(classification, test);
		dissectClassifier(classification, sessionsList);
		saveModel(classification, modelFile);

		return classification;
	}

	private static List<SessionInfo> classifySessions(String testPath,
			SGDClassification classification) throws ParseException,
			IOException {

		FilesParserHelper testFileParser = FilesParserHelper.newInstance(
				testPath, "");
		Map<Integer, SessionInfo> sessions = testFileParser.parseSessions();
		List<SessionInfo> sessionsList = new ArrayList<SessionInfo>(
				sessions.values());

		try {

			int sessionsSize = sessionsList.size();
			InfoOutputHelper.printInfo("Parsed sessions: "
					+ String.valueOf(sessionsSize));

			List<SessionInfo> buySessions = classification
					.classify(sessionsList);

			return buySessions;
		} finally {
			testFileParser.dispose();
			testFileParser = null;

			sessions.clear();
			sessions = null;

			sessionsList.clear();
			sessionsList = null;
		}
	}

	private static void testClassifier(SGDClassification classification,
			List<SessionInfo> testSessions) throws IOException {
		classification.test(testSessions, false);
	}

	private static void dissectClassifier(SGDClassification classification,
			List<SessionInfo> sessions) throws IOException {
		classification.dissect(sessions);
	}

	private static UserBasedRecomender getRecommender(
			List<SessionInfo> buySessions, String clicksPath, String buysPath)
			throws IOException, TasteException, ParseException {

		Map<Integer, SessionInfo> trainSessions;
		List<SessionInfo> trainSessionsList;

		FilesParserHelper trainParser = FilesParserHelper.newInstance(
				clicksPath, buysPath, 509696, ratio);

		try {

			trainSessions = trainParser.parseSessions();
			trainSessionsList = new ArrayList<SessionInfo>(
					trainSessions.values());

			// trainSessions.clear();

			int sessionsSize = trainSessionsList.size();
			InfoOutputHelper.printInfo("Parsed sessions: "
					+ String.valueOf(sessionsSize));

			UserBasedRecomender recommender = new UserBasedRecomender(
					trainSessionsList, buySessions);

			return recommender;

		} finally {
			trainParser.dispose();
			trainSessionsList = null;
		}
	}

	private static void saveModel(SGDClassification classification,
			String modelPath) throws IOException {
		classification.saveModel(modelPath);
	}

	private static void initSparkContext(long cores) {
		SparkHelper.initSparkContext(cores);
	}

	@SuppressWarnings("static-access")
	private static CommandLine parseCLI(String[] args) {
		Option dataDir = OptionBuilder.withArgName("dataDir").hasArg()
				.withDescription("use given file for log").create("dataDir");

		Option modelDir = OptionBuilder.withArgName("modelDir").hasArg()
				.withDescription("use given file for log").create("modelDir");

		Option outputDir = OptionBuilder.withArgName("outputDir").hasArg()
				.withDescription("use given file for log").create("outputDir");

		Option cores = OptionBuilder.withArgName("cores").hasArg()
				.withDescription("the class which it to perform " + "logging")
				.create("cores");
		Options options = new Options();

		options.addOption(dataDir);
		options.addOption(modelDir);
		options.addOption(outputDir);
		options.addOption(cores);

		// create the parser
		CommandLineParser parser = new BasicParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			return line;
		} catch (org.apache.commons.cli.ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		CommandLine line = parseCLI(args);
		String dataDir = line.getOptionValue("dataDir");
		String modelDir = line.getOptionValue("modelDir");
		String outputDir = line.getOptionValue("outputDir");
		long cores = Long.parseLong(line.getOptionValue("cores"));
		initSparkContext(cores);

		SGDClassification classification = loadModelFromFile(modelDir);

		List<SessionInfo> buySessions = classifySessions(dataDir + TEST_FILE,
				classification);

		UserBasedRecomender recommender = getRecommender(buySessions, dataDir
				+ CLICKS_FILE, dataDir + BUYS_FILE);

		Map<Integer, List<Integer>> buySessionInfo = recommender
				.getUserIntersect();
		recommender.exportToFile(outputDir + OUTPUT_FILE, buySessionInfo);

		buySessions.clear();
		buySessions = null;
	}
}
