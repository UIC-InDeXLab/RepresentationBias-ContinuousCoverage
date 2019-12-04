package umichdb.coverage2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jfree.ui.RefineryUtilities;

public class TestCoverageChecker {
	
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
	
	
	
	public static void main(String[] args) {
		int n = 20;
		int d = 2;
		double theta = 0.1;
		int k = 2;
		
		System.out.printf("Create %d points of dimension %d\n", n, d);
		NDPoint[] randPoints = genRandNDPoint(n, d);
		for (int i = 0; i <  Math.min(randPoints.length, 10); i++) {
			System.out.println("\t" + randPoints[i]);
		}
		System.out.println("\t...");

		System.out.println(
				"Start building coverage graph");

		double beginTime = System.currentTimeMillis();
		CoverageChecker cc = new CoverageChecker(randPoints, k, theta);
		double endTime = System.currentTimeMillis();
		

		System.out.printf("Coverage discovery time: %f ms\n", endTime - beginTime);
				
		CoverageChecker.view(cc);

	}
}
