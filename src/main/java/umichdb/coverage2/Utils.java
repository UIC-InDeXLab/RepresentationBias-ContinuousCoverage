package umichdb.coverage2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVFormat;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.BaseVector;
import smile.io.CSV;
import smile.math.distance.EuclideanDistance;

public class Utils {
//	final static long seed = 10;
	


	/**
	 * Randomly generate a double number with max value
	 * @param max
	 * @return
	 */
	public static double randDouble(double max) {
	    Random rand = new Random();
	    return rand.nextDouble() * max;
	}
	

	/**
	 * Randomly generate n d-dimensional points
	 * 
	 * @param n
	 * @param d
	 * @return
	 */
	public static DataFrame genRandDataset(int n, int d) {
		Random rand = new Random();

		double[][] data = new double[n][d];

		for (int i = 0; i < n; i++) {
			for (int dim = 0; dim < d; dim++) {
				data[i][dim] = rand.nextDouble();
			}
		}

		DataFrame randPoints = DataFrame.of(data);

		return randPoints;
	}


	static final String SCHEMA_NAME_COL = "Name";
	static final String SCHEMA_TYPE_COL = "Type";
	static final String SCHEMA_VALS_COL = "Values";

	static final String SCHEMA_TYPE_CONTINUOUS = "continuous";
	static final String SCHEMA_TYPE_NOMINAL = "nominal";

	/**
	 * Load dataset from CSV file (using the schema information)
	 * 
	 * @param dataFileName
	 * @param schemaFileName
	 * @return
	 */
	public static DataFrame loadDataSetFromCSV(String dataFileName,
			String schemaFileName) {
		// Load schema
		List<StructField> schemaFields = new ArrayList<StructField>();
		schemaFields
				.add(new StructField(SCHEMA_NAME_COL, DataTypes.StringType));
		schemaFields
				.add(new StructField(SCHEMA_TYPE_COL, DataTypes.StringType));
		schemaFields
				.add(new StructField(SCHEMA_VALS_COL, DataTypes.StringType));

		CSV schemaCsv = new CSV(CSVFormat.DEFAULT);
		schemaCsv.schema(new StructType(schemaFields));

		// Build schema 
		List<StructField> dataFields = new ArrayList<StructField>();
		try {
			DataFrame schemaDf = schemaCsv.read(schemaFileName);
			for (int i = 0; i < schemaDf.size(); i++) {
				String attrName = schemaDf.getString(i, SCHEMA_NAME_COL);
				String typeName = schemaDf.getString(i, SCHEMA_TYPE_COL);
				String values = schemaDf.getString(i, SCHEMA_VALS_COL);
				switch (typeName) {
					case SCHEMA_TYPE_CONTINUOUS : {
						dataFields.add(new StructField(attrName,
								DataTypes.DoubleType));
						break;
					}
					case SCHEMA_TYPE_NOMINAL : {
						dataFields.add(
								new StructField(attrName, DataTypes.ByteType,
										new NominalScale(values.split(","))));
						break;
					}
				}
			}

		} catch (Exception ex) {
			System.err.println("Failed to load file: " + ex);
			ex.printStackTrace();
			System.exit(-1);
		}

		// Load data
		CSV dataCsv = new CSV(CSVFormat.DEFAULT);
		dataCsv.schema(new StructType(dataFields));

		try {
			return dataCsv.read(dataFileName);
		} catch (Exception ex) {
			System.err.println("Failed to load file: " + ex);
			ex.printStackTrace();
			System.exit(-1);
		}

		return null;

	}

	/**
	 * Get Euclidean distance between two tuples
	 * 
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double getEuclideanDistance(Tuple v1, Tuple v2) {
		return new EuclideanDistance().d(v1.toArray(), v2.toArray());
	}

	/**
	 * Get Euclidean distance between two double arrays
	 * 
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double getEuclideanDistance(double[] v1, double[] v2) {
		return new EuclideanDistance().d(v1, v2);
	}

	public static void main(String[] args) {
		// DataFrame s = genRandDataset(10, 2);
		// System.out.println(s);
	}
}
