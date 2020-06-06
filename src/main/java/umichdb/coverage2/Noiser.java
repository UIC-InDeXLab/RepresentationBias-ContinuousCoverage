package umichdb.coverage2;

import java.util.stream.DoubleStream;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.BaseVector;
import smile.data.vector.DoubleVector;
import smile.feature.FeatureTransform;
import smile.math.MathEx;

/**
 * This class imitates the Scaler in Smile Libarary to addd tiny noise to dataframes to make sure all data points are unique in the dataset so that the voronoi library won't fail
 * @author markjin1990
 *
 */
public class Noiser implements FeatureTransform {
	private static final long serialVersionUID = 2L;
	
	/**
     * The schema of data.
     */
    StructType schema;
    /**
     * Lower bound.
     */
    double[] lo;
    /**
     * Upper bound.
     */
    double[] hi;
    
    final static double noiseRatio = 0.0001;
    
    /**
     * Constructor.
     * @param schema the schema of data.
     * @param lo the lower bound.
     * @param hi the upper bound.
     */
    public Noiser(StructType schema, double[] lo, double[] hi) {
        if (schema.length() != lo.length || lo.length != hi.length) {
            throw new IllegalArgumentException("Schema and scaling factor size don't match");
        }

        this.schema = schema;
        this.lo = lo;
        this.hi = hi;

        for (int i = 0; i < lo.length; i++) {
            hi[i] -= lo[i];
            if (MathEx.isZero(hi[i])) {
                hi[i] = 1.0;
            }
        }
    }

	
	
    /**
     * Learns transformation parameters from a dataset.
     * @param data The training data.
     */
    public static Noiser fit(DataFrame data) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Empty data frame");
        }

        StructType schema = data.schema();
        double[] lo = new double[schema.length()];
        double[] hi = new double[schema.length()];

        for (int i = 0; i < lo.length; i++) {
            if (schema.field(i).isNumeric()) {
                lo[i] = data.doubleVector(i).stream().min().getAsDouble();
                hi[i] = data.doubleVector(i).stream().max().getAsDouble();
            }
        }

        return new Noiser(schema, lo, hi);
    }
    

    /** Scales a value with i-th column parameters. */
    private double addSomeNoise(double x, int i) {
        double y = (x - lo[i]) / hi[i];
        return x + x * Utils.randDouble(y * noiseRatio);
    }


	@Override
	public double[] transform(double[] values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tuple transform(Tuple t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataFrame transform(DataFrame data) {
        if (!schema.equals(data.schema())) {
            throw new IllegalArgumentException(String.format("Invalid schema %s, expected %s", data.schema(), schema));
        }

        BaseVector[] vectors = new BaseVector[schema.length()];
        for (int i = 0; i < lo.length; i++) {
            StructField field = schema.field(i);
            if (field.isNumeric()) {
                final int col = i;
                DoubleStream stream = data.stream().mapToDouble(t -> addSomeNoise(t.getDouble(col), col));
                vectors[i] = DoubleVector.of(field, stream);
            } else {
                vectors[i] = data.column(i);
            }
        }
        return DataFrame.of(vectors);
	}

}
