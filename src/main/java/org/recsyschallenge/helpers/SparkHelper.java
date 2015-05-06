package org.recsyschallenge.helpers;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;


public class SparkHelper {
	static SparkConf conf = new SparkConf().setAppName("recsys")
			.setMaster("local[4]")
			.set("spark.executor.memory", "10g");
	
	public static JavaSparkContext sc = new JavaSparkContext(conf);	
	
	private SparkHelper() {		
		
	}
}
