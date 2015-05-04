package org.recsyschallenge.helpers;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.csv.CSVParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

public class HDFSParserHelper {
	public static class TokenizerMapper implements
			Mapper<LongWritable, Text, Text, IntWritable> {

		@Override
		public void configure(JobConf job) {
			// TODO Auto-generated method stub

		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			if (key.get() > 0) {

			}

		}
	}

	public static class IntSumReducer implements
			Reducer<Text, IntWritable, Text, IntWritable> {

		private IntWritable result = new IntWritable();
		private int sum;

		@Override
		public void configure(JobConf job) {
			// TODO Auto-generated method stub

		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			sum = 0;

		}
	}

	public HDFSParserHelper() throws IOException {
		Configuration conf = new Configuration();
		conf.addResource(new Path(
				"/home/momchil/Desktop/cluster/hadoop-2.4.1/hadoop/etc/core-site.xml"));
		conf.addResource(new Path(
				"/home/momchil/Desktop/cluster/hadoop-2.4.1/hadoop/etc/hdfs-site.xml"));

		FileSystem fs = FileSystem.get(conf);
//		FSDataInputStream inputStream = fs.open(new Path(
//				"/recsys/yoochoose-buys.dat"));

//		conf.setBoolean("mapreduce.input.fileinputformat.input.dir.recursive", true);
		JobConf job = new JobConf(conf);
		job.setMapperClass(TokenizerMapper.class);
		job.setCombinerClass(IntSumReducer.class);
		job.setReducerClass(IntSumReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		FileInputFormat.addInputPath(job,
				new Path("hdfs://localhost:9000/recsys/RecSys."));
		FileOutputFormat.setOutputPath(job,
				new Path("hdfs://localhost:9000/recsys/RecSys/tmp"));
		
		JobClient.runJob(job);
	}

}
