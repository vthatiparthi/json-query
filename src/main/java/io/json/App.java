package io.json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String args[]) {
		AppParameters parameters = new AppParameters();
		JCommander parameterParser = new JCommander(parameters, args);
		if (parameters.help) {
			parameterParser.usage();
			return;
		}

		long startTime = System.currentTimeMillis();
		final int numThreads = Runtime.getRuntime().availableProcessors();
		logger.info("Total available cores is: " + numThreads);

		int limitSize = parameters.limitSize;
		ExecutorService exec = Executors.newFixedThreadPool(numThreads);
		String fileName = parameters.inputFile;
		File f = new File(fileName);
		long fileSize = f.length();
		long bytesForThread = fileSize / numThreads;
		long startPos = 0;
		long endPos = 0;
		List<Future<Map<String, Long>>> futures = new ArrayList<>();
		for (int i = 0; i < numThreads; i++) {
			startPos = endPos;
			endPos = startPos + bytesForThread;
			logger.info("Submitting task " + (i + 1) + "with start byte " + startPos + " end byte " + endPos);
			Future<Map<String, Long>> future = exec.submit(new VisitorActivites(limitSize, startPos, endPos, fileName));
			futures.add(future);
		}

		Map<String, Long> outputMap = new HashMap<>();
		logger.info("Collecting the results from each task");
		for (Future<Map<String, Long>> future : futures) {
			Map<String, Long> currentMap;
			try {
				currentMap = future.get();
				if (outputMap.size() == 0) {
					outputMap.putAll(currentMap);
				} else {
					for (String pid : currentMap.keySet()) {
						long timestamp = currentMap.get(pid);
						VisitorActivites.updateMap(outputMap, pid, timestamp, limitSize);
					}
				}
			} catch (InterruptedException e) {
				logger.error("While generating report caught Interrupted exception " + e.getMessage(), e);
			} catch (ExecutionException e) {
				logger.error("While generating report caught ExecutionException exception " + e.getMessage(), e);
			}
		}
		exec.shutdown();
		
		logger.warn("Finished collecting results from indvidual tasks, total out put records size :" + outputMap.size());
		csvWriter(outputMap, parameters.outputFile);
		long endtime = System.currentTimeMillis();
		logger.info("Job started at " + startTime + " job end at " + endtime + " took " + ((endtime - startTime) / 1000) + " seconds to complete the job");
		logger.info("Done..!");
	}

	public static void csvWriter(Map<String, Long> outputMap, String outputFileName) {
		LinkedList<Map.Entry<String, Long>> unOrderedList = new LinkedList<>(outputMap.entrySet());
		Collections.sort(unOrderedList, new Comparator<Map.Entry<String, Long>>() {
			@Override
			public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		try (Writer writer = new FileWriter(outputFileName)) {
			for (Map.Entry<String, Long> entry : unOrderedList) {
				writer.append(entry.getKey()).append(',').append(entry.getValue().toString()).append("\n");
			}
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
		}
		logger.info("Finished writing report to : " + outputFileName);
	}
}
