package vldb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class EfficiencyTest {
	DataFrame df;
	MithraCoverageChecker mcc;
	BasicCoverageChecker bcc;

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
	 * Evaluate MithraCoverage construction time (in seconds)
	 * @param k
	 * @param rho
	 * @return
	 */
	public double mithraConstructionTime(int k, double rho) {
		return mithraConstructionTime(k, rho, 1);
	}

	/**
	 * Evaluate average MithraCoverage construction time (in seconds)
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
		constructionResult.add("Dataset,K,Rho,Time");

		for (int k : kValues) {
			for (double rho : rhoValues) {
				// Construction test
				double constructionTime = irisTest.mithraConstructionTime(k,
						rho, repeat);
				constructionResult.add(String.format("%s,%d,%.3f,%.3f",
						datasetFileName, k, rho, constructionTime));

				// Query test
				List<String> queryTimeResult = new ArrayList<String>();
				queryTimeResult.add("Dataset,K,Rho,NumQueries,Dimensions,Time");

				for (int numQueries : numQueriesTested) {
					System.out.println(String
							.format("Efficiency test: file=%s, k=%d, rho=%.3f, numQueries=%d, dim=%d", datasetFileName, k, rho, numQueries, dimensions));
					double queryTime = irisTest.mithraQueryTime(numQueries,
							dimensions);
					queryTimeResult.add(String.format("%s,%d,%.3f,%d,%d,%.3f",
							datasetFileName, k, rho, numQueries, dimensions,
							queryTime));
				}
				for (String row : queryTimeResult)
					System.out.println(row);
			}
		}

		for (String row : constructionResult)
			System.out.println(row);

	}
}
