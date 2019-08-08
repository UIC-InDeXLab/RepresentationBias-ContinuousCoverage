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
	 * A naive approach to discover the uncovered space (N-dimensional cube).
	 * 
	 * @param points
	 * @return
	 */
	public static NDCube findMaxVolumeNDCubeNaive(NDPoint[] points) {
		NDCube NDCubeMaxVolume = null;

		if (points == null || points.length < 1) {
			return null;
		}

		int dim = points[0].getDimensions();

		// not enough points to formulate a least one hypercube
		if (points.length < 2) {
			return null;
		}

		// Enumerate all possible hypercubes. Note that each n-dimensional cube
		// (hyper cube is uniquely defined by 2...2n points.
		for (int numVertices = 2; numVertices <= 2 * dim; numVertices++) {
			if (numVertices > points.length) {
				break;
			}

			Iterator<int[]> it = CombinatoricsUtils
					.combinationsIterator(points.length, numVertices);

			while (it.hasNext()) {
				// new set of vertices
				int[] indexes = it.next();
				NDPoint[] tempPoints = new NDPoint[indexes.length];
				for (int i = 0; i < indexes.length; i++) {
					tempPoints[i] = points[indexes[i]];
				}

				// hypercube defined by 2d points
				NDCube newCube = createNDCubeByNDPoints(tempPoints);

				if (newCube == null) {
					continue;
				}

				// Check if the hypercube is totally empty.
				Set<Integer> indexSet = new HashSet<Integer>();
				for (int idx : indexes) {
					indexSet.add(idx);
				}

				boolean ifEmptyCube = true;
				for (int i = 0; i < points.length; i++) {
					if (!indexSet.contains(i)) {
						NDPoint otherPoint = points[i];
						if (newCube.contains(otherPoint)) {
							ifEmptyCube = false;
							break;
						}
					}
				}

				// Check if it has max volume
				if (ifEmptyCube && (NDCubeMaxVolume == null
						|| newCube.getVolume() > NDCubeMaxVolume.getVolume())) {
					NDCubeMaxVolume = newCube;
				}
			}
		}

		// Return the one with the largest volume
		return NDCubeMaxVolume;
	}

	/**
	 * Proposed approach to discover the uncovered space
	 * @param points
	 * @return
	 */
	public static NDCube findMaxVolumeNDCube(NDPoint[] points) {
		NDCube NDCubeMaxVolume = null;

		if (points == null || points.length < 1) {
			return null;
		}

		int d = points[0].getDimensions();

		// not enough points to formulate a least one hypercube
		if (points.length < 2) {
			return null;
		}

		// Enumerate all possible pairs of points
		Iterator<int[]> it = CombinatoricsUtils
				.combinationsIterator(points.length, 2);

		while (it.hasNext()) {
			// new set of vertices
			int[] indexes = it.next();
			NDPoint[] tempPoints = new NDPoint[indexes.length];
			for (int i = 0; i < indexes.length; i++) {
				tempPoints[i] = points[indexes[i]];
			}
		}
		
		// Return the one with the largest volume
		return NDCubeMaxVolume;
	}

	private static NDCube createNDCubeByNDPoints(NDPoint[] points) {
		if (points == null || points.length < 1) {
			return null;
		}

		int dim = points[0].getDimensions();

		// Dimensionality is wrong return null. The number of points < 2.
		if (points.length < 2) {
			return null;
		}

		NDCube newCube = new NDCube(dim);

		for (int d = 0; d < dim; d++) {
			double maxValAtDim = Double.MIN_VALUE;
			double minValAtDim = Double.MAX_VALUE;

			for (NDPoint p : points) {
				double valAtDim = p.valAtNDimension(d);
				if (valAtDim > maxValAtDim) {
					maxValAtDim = valAtDim;
				}
				if (valAtDim < minValAtDim) {
					minValAtDim = valAtDim;
				}
				newCube.setMinValAtDim(d, minValAtDim);
				newCube.setMaxValAtDim(d, maxValAtDim);
			}
		}

		// Check if every point is on the edge. If not, this is not a good cube,
		// we ignore it.
		for (NDPoint p : points) {
			boolean ifOnEdge = false;
			for (int d = 0; d < dim; d++) {
				if (p.valAtNDimension(d) <= newCube.getMinValAtDim(d)
						|| p.valAtNDimension(d) >= newCube.getMaxValAtDim(d)) {
					ifOnEdge = true;
					break;
				}
			}
			if (!ifOnEdge) {
				return null;
			}
		}

		return newCube;

	}

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

	public static void visualize(NDPoint[] points, NDCube coverageCube) {
		SwingUtilities.invokeLater(() -> {
			Draw example = new Draw(
					"Find uncovered space for " + points.length + " random points in "
							+ points[0].getDimensions() + "-d space.",
					points, coverageCube);
			example.setSize(800, 800);
			example.setLocationRelativeTo(null);
			example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			example.setVisible(true);
		});
	}

	public static void main(String[] args) {
		int n = 50;
		int d = 2;
		System.out.printf("Create %d cubes of dimension %d\n\t", n, d);
		NDPoint[] randPoints = genRandNDPoint(n, d);
		for (NDPoint p : randPoints) {
			System.out.print(p + " ");
		}
		System.out.println();

		System.out.println(
				"Start finding coverage cube using the naive approach");
		double beginTime = System.currentTimeMillis();
		NDCube maxCube = findMaxVolumeNDCubeNaive(randPoints);
		double endTime = System.currentTimeMillis();

		System.out.println("Cube found!");
		System.out.println(maxCube);
		System.out.printf("Search time: %f ms", endTime - beginTime);

		visualize(randPoints, maxCube);
	}
}
