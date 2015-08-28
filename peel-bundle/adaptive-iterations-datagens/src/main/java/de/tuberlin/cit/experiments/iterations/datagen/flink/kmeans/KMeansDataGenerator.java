package de.tuberlin.cit.experiments.iterations.datagen.flink.kmeans;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

public class KMeansDataGenerator implements Iterator<double[]>, Serializable {

	private MeanGeneratorConfiguration[] configurations;
	private double[] meanProbabilities;

	private int generatedCounter;
	private int numDataPoints;

	private Random rnd;


	public KMeansDataGenerator(MeanGeneratorConfiguration[] configurations, double[] meanProbabilities,
			int numDataPoints) {
		this.configurations = configurations;
		this.meanProbabilities = meanProbabilities;
		this.numDataPoints = numDataPoints;
		this.generatedCounter = 0;
		this.rnd = new Random();
	}

	private MeanGeneratorConfiguration getRandomMean() {
		double meanProb = rnd.nextDouble();

		int selectedMean = meanProbabilities.length - 1;

		double s = 0;
		for (int i = 0; i < meanProbabilities.length; i++) {
			s += meanProbabilities[i];
			if (meanProb < s) {
				selectedMean = i;
				break;
			}
		}

		return configurations[selectedMean];
	}

	private double[] generateRandomPoint(MeanGeneratorConfiguration conf) {
		double[] mean = conf.getMean();
		double stdDev = conf.getStdDev();

		double[] point = new double[mean.length];
		for (int i = 0; i < mean.length; i++) {
			point[i] = rnd.nextGaussian() * stdDev + mean[i];
		}

		return point;
	}

	public void setRandom(Random rnd) {
		this.rnd = rnd;
	}

	@Override
	public double[] next() {
		++generatedCounter;
		return generateRandomPoint(getRandomMean());
	}

	@Override
	public boolean hasNext() {
		return generatedCounter < numDataPoints;
	}
}