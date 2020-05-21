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
	 * 
	 * @param k
	 * @param rho
	 * @return
	 */
	public double mithraConstructionTime(int k, double rho) {
		double constructionBeginTime = System.currentTimeMillis();

		mcc = new MithraCoverageChecker(df, k, rho);

		double constructionEndTime = System.currentTimeMillis();
		return (constructionEndTime - constructionBeginTime) / 1000.0;
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

		for (BaseVector p : queryPoints) {
			mcc.ifCovered(p.toDoubleArray(), false);
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

		for (BaseVector p : queryPoints) {
			bcc.ifCovered(p.toDoubleArray());
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
		int dimensions = selectedAttrs.length;

		// Start test
		EfficiencyTest irisTest = new EfficiencyTest(datasetFileName,
				schemaFileName, selectedAttrs);

		List<String> constructionResult = new ArrayList<String>();
		constructionResult.add("Dataset,K,Rho,Time");

		for (int k : kValues) {
			for (double rho : rhoValues) {
				// Construction test
				System.out.println(String
						.format("Efficiency test: k = %d, rho = %f", k, rho));
				double constructionTime = irisTest.mithraConstructionTime(k,
						rho);
				constructionResult.add(String.format("%s,%d,%.3f,%.3f",
						datasetFileName, k, rho, constructionTime));

				// Query test
				List<String> queryTimeResult = new ArrayList<String>();
				queryTimeResult.add("Dataset,K,Rho,NumQueries,Dimensions,Time");

				for (int numQueries : numQueriesTested) {
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
