/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.recsyschallenge.algorithms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.math3.util.FastMath;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.classifier.df.DFUtils;
import org.apache.mahout.classifier.df.DecisionForest;
import org.apache.mahout.classifier.df.ErrorEstimate;
import org.apache.mahout.classifier.df.builder.DefaultTreeBuilder;
import org.apache.mahout.classifier.df.data.Data;
import org.apache.mahout.classifier.df.data.DataLoader;
import org.apache.mahout.classifier.df.data.Dataset;
import org.apache.mahout.classifier.df.data.DescriptorException;
import org.apache.mahout.classifier.df.ref.SequentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test procedure as described in Breiman's paper.<br>
 * <b>Leo Breiman: Random Forests. Machine Learning 45(1): 5-32 (2001)</b>
 */
public class BreimanExample extends Configured implements Tool {

	private static final Logger log = LoggerFactory
			.getLogger(BreimanExample.class);

	/** sum test error */
	private double sumTestErrM;

	private double sumTestErrOne;

	/** mean time to build a forest with m=log2(M)+1 */
	private long sumTimeM;

	/** mean time to build a forest with m=1 */
	private long sumTimeOne;

	/** mean number of nodes for all the trees grown with m=log2(M)+1 */
	private long numNodesM;

	/** mean number of nodes for all the trees grown with m=1 */
	private long numNodesOne;

	/**
	 * runs one iteration of the procedure.
	 * 
	 * @param rng
	 *            random numbers generator
	 * @param data
	 *            training data
	 * @param m
	 *            number of random variables to select at each tree-node
	 * @param nbtrees
	 *            number of trees to grow
	 */
	private void runIteration(Random rng, Data data, int m, int nbtrees) {

		log.info("Splitting the data");
		Data train = data.clone();
		Data test = train.rsplit(rng, (int) (data.size() * 0.1));

		DefaultTreeBuilder treeBuilder = new DefaultTreeBuilder();

		SequentialBuilder forestBuilder = new SequentialBuilder(rng,
				treeBuilder, train);

		// grow a forest with m = log2(M)+1
		treeBuilder.setM(m);

		long time = System.currentTimeMillis();
		System.out.printf("Growing a forest with m=%d\n", m);
		DecisionForest forestM = forestBuilder.build(nbtrees);
		sumTimeM += System.currentTimeMillis() - time;
		numNodesM += forestM.nbNodes();

		// grow a forest with m=1
		treeBuilder.setM(1);

		time = System.currentTimeMillis();
		System.out.printf("Growing a forest with m=1\n");
		DecisionForest forestOne = forestBuilder.build(nbtrees);
		sumTimeOne += System.currentTimeMillis() - time;
		numNodesOne += forestOne.nbNodes();

		// compute the test set error (Selection Error), and mean tree error
		// (One Tree Error),
		double[] testLabels = test.extractLabels();
		double[][] predictions = new double[test.size()][];

		forestM.classify(test, predictions);
		double[] sumPredictions = new double[test.size()];
		Arrays.fill(sumPredictions, 0.0);
		for (int i = 0; i < predictions.length; i++) {
			for (int j = 0; j < predictions[i].length; j++) {
				sumPredictions[i] += predictions[i][j];
			}
		}
		sumTestErrM += ErrorEstimate.errorRate(testLabels, sumPredictions);

		forestOne.classify(test, predictions);
		Arrays.fill(sumPredictions, 0.0);
		for (int i = 0; i < predictions.length; i++) {
			for (int j = 0; j < predictions[i].length; j++) {
				sumPredictions[i] += predictions[i][j];
			}
		}
		sumTestErrOne += ErrorEstimate.errorRate(testLabels, sumPredictions);
	}

	public static void main(String[] trainData) throws Exception {
		ToolRunner.run(new Configuration(), new BreimanExample(), trainData);
	}

	@Override
	public int run(String[] trainData) throws IOException, DescriptorException {

		int nbTrees = 100;
		int nbIterations = 10;

		// Dataset
		Dataset dataset = DataLoader.generateDataset("N N C L", false,
				trainData);

		// Training data
		Data data = DataLoader.loadData(dataset, trainData);

		// take m to be the first integer less than log2(M) + 1, where M is the
		// number of inputs
		int m = (int) Math.floor(FastMath.log(2.0, data.getDataset()
				.nbAttributes()) + 1);

		Random rng = RandomUtils.getRandom();
		for (int iteration = 0; iteration < nbIterations; iteration++) {
			log.info("Iteration {}", iteration);
			runIteration(rng, data, m, nbTrees);
		}

		System.out.println("********************************************");
		System.out.printf("Random Input Test Error : %f\n", sumTestErrM
				/ nbIterations);
		System.out.printf("Single Input Test Error : %f\n", sumTestErrOne
				/ nbIterations);
//		System.out.printf("Mean Random Input Time : %f\n",
//				DFUtils.elapsedTime(sumTimeM / nbIterations));
//		System.out.printf("Mean Single Input Time : %f\n",
//				DFUtils.elapsedTime(sumTimeOne / nbIterations));
//		System.out.printf("Mean Random Input Num Nodes : %f\n", numNodesM
//				/ nbIterations);
//		System.out.printf("Mean Single Input Num Nodes : %f\n", numNodesOne
//				/ nbIterations);

		return 0;
	}

}
