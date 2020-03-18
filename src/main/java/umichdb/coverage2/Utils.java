package umichdb.coverage2;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Utils {
	final static long seed = 10;
	
	/**
	 * Randomly generate n d-dimensional points
	 * 
	 * @param n
	 * @param d
	 * @return
	 */
	public static NDPoint[] genRandNDPoint(int n, int d) {
		Random rand = new Random();
		
		rand.setSeed(seed);

		NDPoint[] randPoints = new NDPoint[n];
		Set<NDPoint> existingNDpoints = new HashSet<NDPoint>();

		for (int i = 0; i < n; i++) {
			double[] coords = new double[d];
			for (int dim = 0; dim < d; dim++) {
				coords[dim] = rand.nextDouble();
			}

			NDPoint newPoint = new NDPoint(coords);
			if (!existingNDpoints.contains(newPoint)) {
				randPoints[i] = newPoint;
				existingNDpoints.add(newPoint);
			}
		}

		return randPoints;
	}
}
