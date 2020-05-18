package vldb;

import smile.io.CSV;
import smile.util.Paths;

import java.io.File;

import org.apache.commons.csv.CSVFormat;

import smile.data.DataFrame;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;

public class EffectivenessTest {
	DataFrame df;
	
	/**
	 * 
	 * @param fileName
	 * @param schema
	 */
	public EffectivenessTest(String fileName, StructType schema) {
		CSV csv = new CSV(CSVFormat.DEFAULT);
        csv.schema(schema);

        try {
            df = csv.read(fileName);
        } catch(Exception ex) {
            System.err.println("Failed to load file: " + ex);
            ex.printStackTrace();
            System.exit(-1);
        }
	}
	
	/**
	 * 
	 */
	public void test() {
		
	}	

	public static void main(String[] args) {
		
		// Test IRIS
		String fileName = "data/iris.data";
        StructType schema = DataTypes.struct(            
            new StructField("sepalLength", DataTypes.DoubleType),
            new StructField("sepalWidth", DataTypes.DoubleType),
            new StructField("petalLength", DataTypes.DoubleType),
            new StructField("petalWidth", DataTypes.DoubleType),
            new StructField("class", DataTypes.ByteType, new NominalScale("Iris Setosa", "Iris Versicolour", "Iris Virginica"))
        );
        EffectivenessTest irisTest = new EffectivenessTest(fileName, schema);
        irisTest.test();
	}
}
