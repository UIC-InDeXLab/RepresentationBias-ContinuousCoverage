package umichdb.coverage2;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.*;

public class FindCoverageContinuous {


	/**
	 * Randomly generate n d-dimensional points
	 * 
	 * @param n
	 * @param d
	 * @return
	 */
	public static NDPoint[] genRandNDPoint(int n, int d) {
		Random rand = new Random();

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

	public static void visualize(NDPoint[] points, double radius) {
		SwingUtilities.invokeLater(() -> {
			Draw example = new Draw(
					"Find uncovered space for " + points.length + " random points in "
							+ points[0].getDimensions() + "-d space.",
					points, radius);
			example.setSize(800, 800);
			example.setLocationRelativeTo(null);
			example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			example.setVisible(true);
		});
	}

	public static void main(String[] args) {
		int n = 10;
		int d = 2;
		double theta = 0.1;
		int k = 5;
		
		System.out.printf("Create %d cubes of dimension %d\n\t", n, d);
		NDPoint[] randPoints = genRandNDPoint(n, d);
		for (NDPoint p : randPoints) {
			System.out.print(p + " ");
		}
		System.out.println();

		System.out.println(
				"Start finding coverage cube using the naive approach");
		double beginTime = System.currentTimeMillis();
		
		double endTime = System.currentTimeMillis();

		System.out.println("Cube found!");
//		System.out.println(maxCube);
		System.out.printf("Search time: %f ms", endTime - beginTime);

		visualize(randPoints, theta);
	}
}
