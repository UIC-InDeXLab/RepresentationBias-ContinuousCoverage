package vldb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;

import cli.Cli;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.BaseVector;
import smile.io.CSV;
import umichdb.coverage2.BasicCoverageChecker;
import umichdb.coverage2.MithraCoverageChecker;
import umichdb.coverage2.Utils;
import umichdb.coverage2.MithraCoverageCheckerUI.Uiconfig;

public class EfficiencyTest {
	DataFrame df;
	MithraCoverageChecker mcc;
	BasicCoverageChecker bcc;

	final static String resultDir = "result";

	/**
	 * 
	 * @param dataFileName
	 * @param schemaFileName
	 */
	public EfficiencyTest(String dataFileName, String schemaFileName,
			String[] selectedAttrs) {
		this.df = Utils.loadDataSetFromCSV(dataFileName, schemaFileName);
		this.df = this.df.select(selectedAttrs);
	}

	/**
	 * Construct MithraCoverage checker and measure time (in seconds)
	 * 
	 * @param k
	 * @param rho
	 * @return
	 */
	public double mithraConstructionTime(int k, double rho, int repeatTimes) {
		double constructionBeginTime = System.currentTimeMillis();

		for (int i = 0; i < repeatTimes; i++)
			mcc = new MithraCoverageChecker(df, k, rho);

		double constructionEndTime = System.currentTimeMillis();

		return (constructionEndTime - constructionBeginTime) / 1000.0
				/ repeatTimes;
	}

	/**
	 * Construct approximate MithraCoverage checker and measure time (in
	 * seconds)
	 * 
	 * @param k
	 * @param rho
	 * @param epsilon
	 * @param phi
	 * @param repeatTimes
	 * @return
	 */
	public double mithraConstructionTime(int k, double rho, double epsilon,
			double phi, int repeatTimes) {
		double constructionBeginTime = System.currentTimeMillis();

		for (int i = 0; i < repeatTimes; i++)
			mcc = new MithraCoverageChecker(df, k, rho, epsilon, phi);

		double constructionEndTime = System.currentTimeMillis();

		return (constructionEndTime - constructionBeginTime) / 1000.0
				/ repeatTimes;
	}

	/**
	 * Evaluate MithraCoverage query time (in seconds)
	 * 
	 * @param numQueries
	 * @param d
	 * @return
	 */
	public double mithraQueryTime(int numQueries, int d) {
		DataFrame queryPoints = Utils.genRandDataset(numQueries, d);

		double constructionBeginTime = System.currentTimeMillis();

		for (int i = 0; i < queryPoints.size(); i++) {
			Tuple p = queryPoints.get(i);
			mcc.ifCovered(p.toArray(), false);
		}

		double constructionEndTime = System.currentTimeMillis();
		return (constructionEndTime - constructionBeginTime) / 1000.0;
	}

	/**
	 * Evaluate basicCoverage query time (in seconds)
	 * 
	 * @param k
	 * @param rho
	 * @param numQueries
	 * @param d
	 * @return
	 */
	public double baselineQueryTime(int k, double rho, int numQueries, int d) {
		bcc = new BasicCoverageChecker(df, k, rho);
		DataFrame queryPoints = Utils.genRandDataset(numQueries, d);

		double constructionBeginTime = System.currentTimeMillis();

		for (int i = 0; i < queryPoints.size(); i++) {
			Tuple p = queryPoints.get(i);
			bcc.ifCovered(p.toArray());
		}

		double constructionEndTime = System.currentTimeMillis();
		return (constructionEndTime - constructionBeginTime) / 1000.0;
	}

	public static void main(String[] args) throws ParseException {
		// Parse command line arguments and set specs
		Cli cmd = new Cli(args);

		String datasetFileName = cmd.getArgValue(Cli.ARG_INPUT);
		String schemaFileName = cmd.getArgValue(Cli.ARG_SCHEMA);
		int[] kValues = Arrays.stream(cmd.getArgValues(Cli.ARG_K))
				.mapToInt(Integer::parseInt).toArray();
		double[] rhoValues = Arrays.stream(cmd.getArgValues(Cli.ARG_RHO))
				.mapToDouble(Double::parseDouble).toArray();
		int[] numQueriesTested = Arrays
				.stream(cmd.getArgValues(Cli.ARG_NUM_QUERIES))
				.mapToInt(Integer::parseInt).toArray();
		String[] selectedAttrs = cmd.getArgValues(Cli.ARG_ATTRS);
		int repeat = Integer.parseInt(cmd.getArgValue(Cli.ARG_REPEAT));
		int dimensions = selectedAttrs.length;

		// Start test
		EfficiencyTest irisTest = new EfficiencyTest(datasetFileName,
				schemaFileName, selectedAttrs);

		List<String> constructionResult = new ArrayList<String>();
		if (!cmd.hasOption(Cli.ARG_EPSILON)) {
			constructionResult.add("Dataset,K,Rho,Time");
		} else {
			constructionResult.add("Dataset,K,Rho,Epsilon,Phi,Time");
		}

		List<String> queryTimeResult = new ArrayList<String>();
		if (!cmd.hasOption(Cli.ARG_EPSILON)) {

			queryTimeResult.add("Dataset,K,Rho,NumQueries,Dimensions,Time");
		} else {
			queryTimeResult.add(
					"Dataset,K,Rho,Rho,Epsilon,NumQueries,Dimensions,Time");

		}

		for (int k : kValues) {
			for (double rho : rhoValues) {
				if (!cmd.hasOption(Cli.ARG_EPSILON)) {
					// Construction test
					double constructionTime = irisTest.mithraConstructionTime(k,
							rho, repeat);
					constructionResult.add(String.format("%s,%d,%.3f,%.3f",
							datasetFileName, k, rho, constructionTime));

					// Query test
					for (int numQueries : numQueriesTested) {
						System.out.println(String.format(
								"[INFO] Efficiency test: file=%s, k=%d, rho=%.3f, numQueries=%d, dim=%d",
								datasetFileName, k, rho, numQueries,
								dimensions));
						double queryTime = irisTest.mithraQueryTime(numQueries,
								dimensions);
						queryTimeResult.add(String.format(
								"%s,%d,%.3f,%d,%d,%.3f", datasetFileName, k,
								rho, numQueries, dimensions, queryTime));
					}
				} else {
					double[] epsilonValues = Arrays
							.stream(cmd.getArgValues(Cli.ARG_EPSILON))
							.mapToDouble(Double::parseDouble).toArray();
					double[] phiValues = Arrays
							.stream(cmd.getArgValues(Cli.ARG_PHI))
							.mapToDouble(Double::parseDouble).toArray();

					// Construction test
					for (double epsilon : epsilonValues) {
						for (double phi : phiValues) {
							double constructionTime = irisTest
									.mithraConstructionTime(k, rho, epsilon,
											phi, repeat);
							constructionResult.add(
									String.format("%s,%d,%.3f,%.3f,%.3f,%.3f",
											datasetFileName, k, rho, epsilon,
											phi, constructionTime));

							// Query test
							for (int numQueries : numQueriesTested) {
								System.out.println(String.format(
										"[INFO] Efficiency test: file=%s, k=%d, rho=%.3f, epsilon=%.3f, phi=%.3f, numQueries=%d, dim=%d",
										datasetFileName, k, rho, epsilon, phi,
										numQueries, dimensions));
								double queryTime = irisTest.mithraQueryTime(
										numQueries, dimensions);
								queryTimeResult.add(String.format(
										"%s,%d,%.3f,%.3f,%.3f,%d,%d,%.3f",
										datasetFileName, k, rho, epsilon, phi,
										numQueries, dimensions, queryTime));
							}
						}
					}
				}
			}
		}

		// Output result
		if (cmd.hasOption(Cli.ARG_OUTPUT)) {
			System.out.println(
					"[RESULT] SAVE_TO_FILE=" + cmd.hasOption(Cli.ARG_OUTPUT));

			LocalDateTime datetimeObj = LocalDateTime.now();
			DateTimeFormatter formatObj = DateTimeFormatter
					.ofPattern("MM_dd_HH_mm_ss");
			String dataTimeStr = datetimeObj.format(formatObj);

			// Output config
			String cmdConfigFileName = String.format("%s/%s_%s.config.txt",
					resultDir, datasetFileName.replaceAll("[^0-9a-zA-Z]", "_"),
					dataTimeStr);

			try {
				FileWriter myWriter = new FileWriter(cmdConfigFileName);
				myWriter.write(cmd.toString());
				myWriter.close();
				System.out.println(String.format(
						"[RESULT] Successfully wrote config to the file %s.",
						cmdConfigFileName));
			} catch (IOException e) {
				System.out.println(String.format(
						"[ERROR] Fail to write config to the file %s.",
						cmdConfigFileName));
				e.printStackTrace();
			}

			// Output construction time result
			String constructionResultFileName = String.format(
					"%s/%s_%s.construction.csv", resultDir,
					datasetFileName.replaceAll("[^0-9a-zA-Z]", "_"),
					dataTimeStr);

			try {
				FileWriter myWriter = new FileWriter(
						constructionResultFileName);
				for (String row : constructionResult) {
					myWriter.write(row + "\n");
				}
				myWriter.close();
				System.out.println(String.format(
						"[RESULT] Successfully wrote construction time to the file %s.",
						constructionResultFileName));
			} catch (IOException e) {
				System.out.println(
						String.format("[ERROR] Fail to write to the file %s.",
								constructionResultFileName));
				e.printStackTrace();
			}

			// Output query time result
			String queryResultFileName = String.format("%s/%s_%s.query.csv",
					resultDir, datasetFileName.replaceAll("[^0-9a-zA-Z]", "_"),
					dataTimeStr);
			try {
				FileWriter myWriter = new FileWriter(queryResultFileName);
				for (String row : queryTimeResult) {
					myWriter.write(row + "\n");
				}
				myWriter.close();
				System.out.println(String.format(
						"[RESULT] Successfully wrote query time to the file %s.",
						queryResultFileName));
			} catch (IOException e) {
				System.out.println(
						String.format("[ERROR] Fail to write to the file %s.",
								queryResultFileName));
				e.printStackTrace();
			}

		} else {
			System.out.println(
					"[RESULT] SAVE_TO_FILE=" + cmd.hasOption(Cli.ARG_OUTPUT));
			// Print final output
			for (String row : constructionResult)
				System.out.println(row);

			for (String row : queryTimeResult)
				System.out.println(row);

		}

	}
}
