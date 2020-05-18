package vldb;

import org.apache.commons.csv.CSVFormat;

import smile.data.DataFrame;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.io.CSV;
import umichdb.coverage2.CoverageChecker;

public class EfficiencyTest {
	DataFrame df;

	/**
	 * 
	 * @param fileName
	 * @param schemaFull
	 */
	public EfficiencyTest(String fileName, StructType schemaFull,
			String[] selectedCols) {
		CSV csv = new CSV(CSVFormat.DEFAULT);
		csv.schema(schemaFull);

		try {
			df = csv.read(fileName).select(selectedCols);
		} catch (Exception ex) {
			System.err.println("Failed to load file: " + ex);
			ex.printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 * 
	 */
	public void test(int k, double theta) {
		CoverageChecker cc = new CoverageChecker(df, k, theta);
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
		EfficiencyTest irisTest = new EfficiencyTest(fileName, schema,
				new String[]{"sepalLength", "sepalWidth", "petalLength",
						"petalWidth"});

		irisTest.test(2, 0.1);
		System.out.println("Done");

	}
}
