package vldb;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;

import smile.data.DataFrame;
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

	/**
	 * 
	 * @param fileName
	 * @param schema
	 */
	public AccuracyTest(String fileName, StructType schema) {
		CSV csv = new CSV(CSVFormat.DEFAULT);
		csv.schema(schema);

		try {
			df = csv.read(fileName);
		} catch (Exception ex) {
			System.err.println("Failed to load file: " + ex);
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Check accuracy ((tp+tn)/total) of approximate coverage checker.
	 */
	public double testAccuracy(int k, double rho, int numQueries, int d,
			double epsilon, double delta) {
		bcc = new BasicCoverageChecker(df, k, rho);
		approximateMCC = new MithraCoverageChecker(df, k, rho, epsilon, delta);

		DataFrame queryPoints = Utils.genRandDataset(numQueries, d);

		double truePositiveCount = 0;
		for (BaseVector p : queryPoints) {
			if (approximateMCC.ifCovered(p.toDoubleArray(), false) == bcc
					.ifCovered(p.toDoubleArray()))
				truePositiveCount++;
		}

		return truePositiveCount / numQueries;
	}

	public static void main(String[] args) {

		// Test IRIS
		String fileName = "data/iris.data";
		String[] selectedAttrs = new String[]{"Iris Setosa", "Iris Versicolour",
				"Iris Virginica"};

		StructType schema = DataTypes.struct(
				new StructField("sepalLength", DataTypes.DoubleType),
				new StructField("sepalWidth", DataTypes.DoubleType),
				new StructField("petalLength", DataTypes.DoubleType),
				new StructField("petalWidth", DataTypes.DoubleType),
				new StructField("class", DataTypes.ByteType,
						new NominalScale(selectedAttrs)));
		AccuracyTest irisTest = new AccuracyTest(fileName, schema);
		List<String> accuracyTestResult = new ArrayList<String>();
		accuracyTestResult.add("Dataset,K,Rho,NumQueries,Dimensions,Epsilon,Delta,Accuracy");

		int[] kValues = new int[]{2};
		double[] rhoValues = new double[]{0.05, 0.1, 0.15};
		int[] numQueriesTested = new int[]{50, 100, 150, 200};
		double[] epsilonValues = new double[]{0.01, 0.05, 0.1};
		double[] deltaValues = new double[]{0.01, 0.05, 0.1};
		
		for (int k : kValues) {
			for (double rho : rhoValues) {
				for (int numQueries : numQueriesTested) {
					for (double epsilon : epsilonValues) {
						for (double delta : deltaValues) {
							double accuracy = irisTest.testAccuracy(k, rho,
									numQueries, selectedAttrs.length, epsilon, delta);
							accuracyTestResult.add(String.format(
									"%s,%d,%.3f,%d,%d,%.3f,%.3f,%.3f", fileName, k, rho,
									numQueries, selectedAttrs.length, epsilon, delta, accuracy));
						}
					}
				}
			}
		}

	}
}
