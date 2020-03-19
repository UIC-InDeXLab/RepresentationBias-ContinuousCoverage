package umichdb.coverage2;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.*;
import org.jfree.ui.RefineryUtilities;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.type.StructType;
import smile.data.vector.BaseVector;
import smile.data.vector.BooleanVector;
import smile.classification.DecisionTree;

public class CoverageChecker {
	VoronoiKOrder vd;
	DecisionTree coverageTree;
	StructType srcDataSchema;
	DataFrame sites;
	int d;
	int k;
	double theta;

	/**
	 * Find exact coverage
	 * 
	 * @param points
	 * @param k
	 * @param theta
	 */
	public CoverageChecker(DataFrame points, int k, double theta) {
		this.sites = points;
		this.k = k; // k points
		this.theta = theta; // max distance to qualify as adjacent
		this.d = points.ncols();

		// Create cache in the form of a Voronoi diagram
		findVoronoi();
		this.coverageTree = null;
	}

	/**
	 * Find approximate coverage through sampling
	 * 
	 * @param points
	 * @param k
	 * @param theta
	 * @param numSamples
	 */
	public CoverageChecker(DataFrame points, int k, double theta, int s) {
		this.sites = points;
		this.k = k; // k points
		this.theta = theta; // max distance to qualify as adjacent
		this.d = points.ncols();

		// Create "s" many samples as observations to build a "decision tree"
		// later
		DataFrame observations = Utils.genRandDataset(s, this.d);
		boolean[] labels = new boolean[observations.nrows()];

		int counter = 0;
		int numCovers = 0;
		int numUncovers = 0;

		for (int i = 0; i < observations.size(); i++) {
			int neighborCount = 0;
			for (int j = 0; j < this.sites.size(); j++) {
				if (Utils.getEuclideanDistance(observations.get(i),
						this.sites.get(j)) <= this.theta) {
					neighborCount++;
					if (neighborCount >= k) {
						break;
					}
				}
			}
			// This is covered
			if (neighborCount >= k) {
				labels[counter++] = true;
				numCovers++;
			} else { // not covered
				labels[counter++] = false;
				numUncovers++;
			}
		}

		if (numCovers != 0 && numUncovers != 0) {

			String labelName = "ifCovered";
			srcDataSchema = observations.schema();
			DataFrame labeledObservations = observations
					.merge(BooleanVector.of(labelName, labels));
			Formula f = Formula.lhs(labelName);

			// Start building decision tree
			coverageTree = DecisionTree.fit(f, labeledObservations);

			System.out.println(
					"STATUS: Decision tree to report coverage is built.");
		} else {
			System.out.println(
					"WARNING: The dataset is all covered in the current setting");
			this.coverageTree = null;
		}

		this.vd = null;
	}

	/**
	 * Create k Voronoi diagrams for the given sites.
	 */
	private void findVoronoi() {
		List<Point2D> point2dList = new ArrayList<Point2D>();

		for (int i = 0; i < this.sites.size(); i++) {
			Tuple r = this.sites.get(i);
			Point2D newP = new Point2D(r.getDouble(0), r.getDouble(1));
			point2dList.add(newP);
		} ;

		this.vd = new VoronoiKOrder(point2dList, k, false);
	}

	/**
	 * Visualize the coverage info
	 * 
	 * @param cc
	 * @param delta
	 * @param sampleSize
	 * @param allowInteractive
	 */
	public static void View(CoverageChecker cc, double delta, int sampleSize,
			boolean allowInteractive) {
		CoverageCheckerUI chart = new CoverageCheckerUI(
				String.format(
						"Coverage for %d random points in %d-d space. (k=%d, theta=%.2f)",
						cc.sites.size(), cc.sites.ncols(), cc.k, cc.theta),
				cc, delta, sampleSize, allowInteractive);
		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setSize(800, 800);
		chart.setVisible(true);
	}

	/**
	 * Check if point (x,y) is covered
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean ifCovered(double x, double y) {
		if (this.vd != null) {
			PointSet polygonKeys = getContainingVoronoiPolyKey(x, y);
			if (polygonKeys == null)
				return false;

			// Count the number of adjacent points to the given point (x,y)
			// (distance <= theta)
			return polygonKeys.stream().mapToInt(
					p -> (p.dist2(new Point2D(x, y)) <= theta * theta) ? 1 : 0)
					.sum() >= k;
		} else if (this.coverageTree != null) {
			return this.coverageTree
					.predict(Tuple.of(new double[]{x, y}, srcDataSchema)) == 0
							? false
							: true;
		}
		return false;
	}

	/**
	 * Get the Voronoi polygon (keys) that contains point (x,y)
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public PointSet getContainingVoronoiPolyKey(double x, double y) {
		for (VoronoiPolygon p : vd.getPolygons()) {
			if (p.contains(x, y))
				return p.regionKey;
		}

		System.out.println(String.format(
				"ERROR: (%.2f,%.2f) is not found in any voronoi polygon", x,
				y));
		return null;
	}

	public static void main(String[] args) {

	}
}
