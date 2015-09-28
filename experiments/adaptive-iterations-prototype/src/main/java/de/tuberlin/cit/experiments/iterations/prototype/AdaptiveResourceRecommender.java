package de.tuberlin.cit.experiments.iterations.prototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.flink.api.common.JobExecutionResult;

public class AdaptiveResourceRecommender {

	// based on a few experiments with k-means it looks like (for k-means on wally) an average cpu utilization of
	// around 25% for each of the workers is best, so we'll scale up or down more or less aggressively to this
	// target utilization
	private static final Double TARGET_UTILIZATION = 0.75;

	private List<JobExecutionResult> iterationsHistory = new ArrayList<>();

	public int computeNewParallelism(JobExecutionResult lastResult) {

		System.out.println("AI -- COMPUTING NEW PARALLELISM -- ");

		Map<String,Object> accumulatorResults = lastResult.getAllAccumulatorResults();
		lastResult.getNetRuntime();

		// retrieve Job data from accumulators
		Integer currentParallelism = retrieveJobDop(accumulatorResults);
		Integer totalNumberOfSlots = retrieveTotalNumberOfSlots(accumulatorResults);
		List<List<Double>> cpuStatistics = retrieveValidCpuHistories(accumulatorResults);

		if (currentParallelism == -1 || totalNumberOfSlots == -1 || cpuStatistics.isEmpty()) {
			return -1; // not enough information from Flink -> use default parallelism
		}

		System.out.println("AI - last parallelism: " + currentParallelism);
		System.out.println("AI - last runtime: " + lastResult.getNetRuntime() + " ms");

		Double currentUtilization = getAverageUtilization(cpuStatistics);

		System.out.println("AI - ca # of CPU statistics per worker: " + cpuStatistics.get(0).size());
		System.out.println("AI - current average CPU utilization: " + currentUtilization);

		int newParallelism = (int) ((currentUtilization / TARGET_UTILIZATION) * currentParallelism);

		if (newParallelism > totalNumberOfSlots) {
			newParallelism = totalNumberOfSlots;
		}
		if (newParallelism < 1) {
			newParallelism = 1;
		}

		System.out.println("AI - new parallelism: " + newParallelism);
		return newParallelism;
	}

	public void addIterationResultToHistory(JobExecutionResult result) {
		iterationsHistory.add(result);
	}

	public void printExecutionSummary() {
		System.out.println("AI - execution summary for " + iterationsHistory.size() + " iterations");

		int i = 1;

		for (JobExecutionResult result : iterationsHistory) {
			System.out.println("AI - " + i++ + ". iteration: " + result.getNetRuntime() + " ms, " +
							"DoP of " + retrieveJobDop(result.getAllAccumulatorResults()) + ", " +
					getAverageUtilization(retrieveValidCpuHistories(result.getAllAccumulatorResults())) +
					" CPU utilization average");
		}
	}

	private Double getAverageUtilization(List<List<Double>> cpuStatistics) {
		List<Double> cpuAverages = computeCpuAverages(cpuStatistics);

		Double accumulatedAverages = 0.0;
		for (Double avg : cpuAverages) {
			accumulatedAverages += avg;
		}
		return accumulatedAverages / cpuAverages.size();
	}

	private List<Double> computeCpuAverages(List<List<Double>> cpuStatistics) {
		List<Double> cpuAverages = new ArrayList<>();
		for (List<Double> cpuHistory : cpuStatistics) {
			Double aggregatedUtilization = 0.0;
			for (Double utilization : cpuHistory) {
				aggregatedUtilization += utilization;
			}
			cpuAverages.add(aggregatedUtilization / cpuHistory.size());
		}
		return cpuAverages;
	}

	public int retrieveJobDop(Map<String, Object> allAccumulatorResults) {
		for (Map.Entry<String,Object> accumulatorResult : allAccumulatorResults.entrySet()) {
			if (accumulatorResult.getKey().contains("jobDop")) {
				return (Integer)accumulatorResult.getValue();
			}
		}
		return -1;
	}

	public int retrieveTotalNumberOfSlots(Map<String, Object> allAccumulatorResults) {
		for (Map.Entry<String,Object> accumulatorResult : allAccumulatorResults.entrySet()) {
			if (accumulatorResult.getKey().contains("totalNumberOfSlots")) {
				return (Integer)accumulatorResult.getValue();
			}
		}
		return -1;
	}

	private List<List<Double>> retrieveValidCpuHistories(Map<String, Object> allAccumulatorResults) {
		List<List<Double>> cpuStatistics = new ArrayList<>();
		for (Map.Entry<String,Object> accumulatorResult : allAccumulatorResults.entrySet()) {
			if (accumulatorResult.getKey().contains("cpu utilization")) {

				List<Double> cpuHistory = (List<Double>)accumulatorResult.getValue();
				if (!cpuHistory.isEmpty()) {
					cpuStatistics.add(cpuHistory);
				}
			}
		}
		return cpuStatistics;
	}
}