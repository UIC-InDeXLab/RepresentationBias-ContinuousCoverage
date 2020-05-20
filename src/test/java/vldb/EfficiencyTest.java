package vldb;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;

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
	 * @param fileName
	 * @param schemaFull
	 */
	public EfficiencyTest(String fileName, StructType schemaFull,
			String[] selectedAttrs) {
		CSV csv = new CSV(CSVFormat.DEFAULT);
		csv.schema(schemaFull);

		try {
			// Load data and also filter out unnecessary columns
			df = csv.read(fileName).select(selectedAttrs);
		} catch (Exception ex) {
			System.err.println("Failed to load file: " + ex);
			ex.printStackTrace();
			System.exit(-1);
		}

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

	public static void main(String[] args) {

		// Test IRIS
		String fileName = "data/iris.data";
		StructType schema = DataTypes
				.struct(new StructField("sepalLength", DataTypes.DoubleType),
						new StructField("sepalWidth", DataTypes.DoubleType),
						new StructField("petalLength", DataTypes.DoubleType),
						new StructField("petalWidth", DataTypes.DoubleType),
						new StructField("class", DataTypes.ByteType,
								new NominalScale("Iris-setosa",
										"Iris-versicolour", "Iris-virginica")));
		String[] selectedAttrs = new String[]{"sepalLength", "sepalWidth"};

		EfficiencyTest irisTest = new EfficiencyTest(fileName, schema,
				selectedAttrs);

		List<String> constructionResult = new ArrayList<String>();
		constructionResult.add("Dataset,K,Rho,Time");

		int[] kValues = new int[]{2};
		double[] rhoValues = new double[]{0.05, 0.1, 0.15};

		for (int k : kValues) {
			for (double rho : rhoValues) {
				// Construction test
				System.out.println(String
						.format("Efficiency test: k = %d, rho = %f", k, rho));
				double constructionTime = irisTest.mithraConstructionTime(k,
						rho);
				constructionResult.add(String.format("%s,%d,%.3f,%.3f",
						fileName, k, rho, constructionTime));

				// Query test
				List<String> queryTimeResult = new ArrayList<String>();
				queryTimeResult.add("Dataset,K,Rho,NumQueries,Dimensions,Time");

				int[] numQueriesTested = new int[]{50, 100, 150, 200};
				int dimensions = 2;

				for (int numQueries : numQueriesTested) {
					double queryTime = irisTest.mithraQueryTime(numQueries,
							dimensions);
					queryTimeResult.add(
							String.format("%s,%d,%.3f,%d,%d,%.3f", fileName, k,
									rho, numQueries, dimensions, queryTime));
				}
				for (String row : queryTimeResult)
					System.out.println(row);
			}
		}

		for (String row : constructionResult)
			System.out.println(row);

	}
}
