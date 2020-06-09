package vldb;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class AccuracyTest {
	DataFrame df;
	MithraCoverageChecker approximateMCC;
	BasicCoverageChecker bcc;

	final static String resultDir = "result";

	/**
	 * 
	 * @param fileName
	 * @param schema
	 */
	public AccuracyTest(String dataFileName, String schemaFileName,
			String[] selectedAttrs) {
		this.df = Utils.loadDataSetFromCSV(dataFileName, schemaFileName);
		this.df = this.df.select(selectedAttrs);
	}

	/**
	 * Check accuracy ((tp+tn)/total) of approximate coverage checker.
	 */
	public double testAccuracy(int k, double rho, double epsilon, double phi,
			int numQueryPts) {
		bcc = new BasicCoverageChecker(df, k, rho);
		approximateMCC = new MithraCoverageChecker(df, k, rho, epsilon, phi);
		
		DataFrame queryPoints = Utils.genRandDataset(numQueryPts, df.schema().length());

		double truePositiveCount = 0;
		
		for (int i = 0; i < queryPoints.size(); i++) {
			Tuple p = queryPoints.get(i);
			if (approximateMCC.ifCovered(p.toArray(), false) == bcc
					.ifCovered(p.toArray())) {
				truePositiveCount++;
//				System.out.println("[debug]" + approximateMCC.ifCovered(p.toArray(), false)  + " " + bcc
//						.ifCovered(p.toArray()));
			}
			else{
//				System.out.println("[debug] estimate=" + approximateMCC.ifCovered(p.toArray(), false)  + " actual=" + bcc
//					.ifCovered(p.toArray()) + " " + p);
			}
		}

		return truePositiveCount / numQueryPts;
	}

	public static void main(String[] args) {

		// Parse command line arguments and set specs
		Cli cmd = new Cli(args);

		if (!cmd.hasOption(Cli.ARG_EPSILON)) {
			System.out.println("[ERROR] no epsilon provided.");
			System.exit(0);
		}

		if (!cmd.hasOption(Cli.ARG_PHI)) {
			System.out.println("[ERROR] no phi provided.");
			System.exit(0);
		}

		String datasetFileName = cmd.getArgValue(Cli.ARG_INPUT);
		String schemaFileName = cmd.getArgValue(Cli.ARG_SCHEMA);
		int[] kValues = Arrays.stream(cmd.getArgValues(Cli.ARG_K))
				.mapToInt(Integer::parseInt).toArray();
		double[] rhoValues = Arrays.stream(cmd.getArgValues(Cli.ARG_RHO))
				.mapToDouble(Double::parseDouble).toArray();
		int[] numQueryPtsVals = Arrays
				.stream(cmd.getArgValues(Cli.ARG_NUM_QUERIES))
				.mapToInt(Integer::parseInt).toArray();
		String[] selectedAttrs = cmd.getArgValues(Cli.ARG_ATTRS);
		int repeat = Integer.parseInt(cmd.getArgValue(Cli.ARG_REPEAT));
		int dimensions = selectedAttrs.length;

		// Start test
		AccuracyTest irisTest = new AccuracyTest(datasetFileName,
				schemaFileName, selectedAttrs);

		List<String> accuracyResult = new ArrayList<String>();
		accuracyResult.add(
				"Dataset,K,Rho,Epsilon,Phi,numQueryPts,Dimensions,Accuracy");

		for (int k : kValues) {
			for (double rho : rhoValues) {
				double[] epsilonValues = Arrays
						.stream(cmd.getArgValues(Cli.ARG_EPSILON))
						.mapToDouble(Double::parseDouble).toArray();
				double[] phiValues = Arrays
						.stream(cmd.getArgValues(Cli.ARG_PHI))
						.mapToDouble(Double::parseDouble).toArray();

				// Construction test
				for (double epsilon : epsilonValues) {
					for (double phi : phiValues) {
						// Query test
						for (int numQueryPts : numQueryPtsVals) {
							System.out.println(String.format(
									"[INFO] Efficiency test: file=%s, k=%d, rho=%.3f, epsilon=%.3f, phi=%.3f, numQueryPts=%d, dim=%d, repeat=%d",
									datasetFileName, k, rho, epsilon, phi,
									numQueryPts, dimensions, repeat));
							List<Double> accuracies = new ArrayList<Double>();
							for (int i = 0; i < repeat; i++) {
								accuracies.add(irisTest.testAccuracy(k, rho,
										epsilon, phi, numQueryPts));
							}
							accuracyResult.add(String
									.format("%s,%d,%.3f,%.3f,%.3f,%d,%d,%.3f",
											datasetFileName, k, rho, epsilon,
											phi, numQueryPts, dimensions,
											accuracies.stream()
													.mapToDouble(d -> d)
													.average().orElse(0.0)));
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
			String cmdConfigFileName = String.format(
					"%s/accuracy_%s_%s.config.txt", resultDir,
					datasetFileName.replaceAll("[^0-9a-zA-Z]", "_"),
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

			// Output accuracy result
			String accuracyResultFileName = String.format(
					"%s/accuracy_%s_%s.query.csv", resultDir,
					datasetFileName.replaceAll("[^0-9a-zA-Z]", "_"),
					dataTimeStr);
			try {
				FileWriter myWriter = new FileWriter(accuracyResultFileName);
				for (String row : accuracyResult) {
					myWriter.write(row + "\n");
				}
				myWriter.close();
				System.out.println(String.format(
						"[RESULT] Successfully wrote accuracies to the file %s.",
						accuracyResultFileName));
			} catch (IOException e) {
				System.out.println(
						String.format("[ERROR] Fail to write to the file %s.",
								accuracyResultFileName));
				e.printStackTrace();
			}

		} else {
			System.out.println(
					"[RESULT] SAVE_TO_FILE=" + cmd.hasOption(Cli.ARG_OUTPUT));
			// Print final output
			for (String row : accuracyResult)
				System.out.println(row);

		}
	}
}
