package org.recsyschallenge.helpers;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

public class SparkHelper {
	static SparkConf conf;

	public static JavaSparkContext sc;

	private SparkHelper() {

	}

	public static void initSparkContext(long cores) {
		conf = new SparkConf().setAppName("recsys")
				.setMaster("local[" + cores + "]")
				.set("spark.executor.memory", "10g");

		sc = new JavaSparkContext(conf);
	}
}
