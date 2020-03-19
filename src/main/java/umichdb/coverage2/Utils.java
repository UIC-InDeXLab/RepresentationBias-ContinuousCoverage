package umichdb.coverage2;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.vector.BaseVector;
import smile.math.distance.EuclideanDistance;

public class Utils {
	final static long seed = 10;
	
	/**
	 * Randomly generate n d-dimensional points
	 * 
	 * @param n
	 * @param d
	 * @return
	 */
	public static DataFrame genRandDataset(int n, int d) {
		Random rand = new Random();
		
		rand.setSeed(seed);


		double[][] data = new double[n][d];

		for (int i = 0; i < n; i++) {
			double[] coords = new double[d];
			for (int dim = 0; dim < d; dim++) {
				data[i][dim] = rand.nextDouble();
			}
		}
		
		DataFrame randPoints = DataFrame.of(data);

		return randPoints;
	}
	
	/**
	 * Get Euclidean distance between two tuples
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double getEuclideanDistance(Tuple v1, Tuple v2) {
		return new EuclideanDistance().d(v1.toArray(), v2.toArray());
	}
	
	public static void main(String[] args) {
//		DataFrame s = genRandDataset(10, 2);
//		System.out.println(s);
	}
}
