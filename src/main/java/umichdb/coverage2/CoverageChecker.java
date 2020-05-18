package umichdb.coverage2;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.awt.Graphics;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.*;
import org.jfree.ui.RefineryUtilities;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.type.StructType;
import smile.data.vector.BaseVector;
import smile.data.vector.BooleanVector;
import smile.feature.Scaler;
import umichdb.coverage2.CoverageCheckerUI.Uiconfig;
import smile.base.cart.DecisionNode;
import smile.base.cart.Node;
import smile.classification.DecisionTree;
import smile.base.cart.SplitRule;

public class CoverageChecker {
	VoronoiKOrder coverageVoronoiDiagram;
	DecisionTree coverageDecisionTree;
	StructType srcDataSchema;
	DataFrame dataset;
	int d;
	int k;
	double theta;

	/**
	 * Find exact coverage
	 * 
	 * @param dataset
	 * @param k
	 *            (minimum number of data points within theta distance to
	 *            qualify as covered)
	 * @param theta
	 *            (distance)
	 */
	public CoverageChecker(DataFrame dataset, int k, double theta) {
		// Rescaling
		Scaler s = Scaler.fit(dataset);
		this.dataset = s.transform(dataset);
		this.k = k; // k points
		this.theta = theta; // max distance to qualify as adjacent
		this.d = dataset.ncols();

		// Create cache in the form of a Voronoi diagram
		findVoronoi();
		this.coverageDecisionTree = null;
	}

	/**
	 * Find approximate coverage through sampling
	 * 
	 * @param dataset
	 * @param k
	 * @param theta
	 * @param numSamples
	 */
	public CoverageChecker(DataFrame dataset, int k, double theta,
			int numSamples) {
		// Rescaling
		Scaler s = Scaler.fit(dataset);
		this.dataset = s.transform(dataset);

		this.k = k; // k points
		this.theta = theta; // max distance to qualify as adjacent
		this.d = dataset.ncols();

		// Create "s" many samples as observations to build a "decision tree"
		// later
		DataFrame observations = Utils.genRandDataset(numSamples, this.d);
		boolean[] labels = new boolean[observations.nrows()];

		int counter = 0;
		int numCovers = 0;
		int numUncovers = 0;

		for (int i = 0; i < observations.size(); i++) {
			int neighborCount = 0;
			for (int j = 0; j < this.dataset.size(); j++) {
				if (Utils.getEuclideanDistance(observations.get(i),
						this.dataset.get(j)) <= this.theta) {
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
			coverageDecisionTree = DecisionTree.fit(f, labeledObservations,
					SplitRule.GINI, 10, 100, 2);

			// System.out.println((DecisionNode)coverageDecisionTree.ro);

			List<String> lines = new ArrayList<>();
			Node n = coverageDecisionTree.root();

			// // Preparing to draw in 2d space
			// coverageDecisionTree.root().toString(coverageDecisionTree.schema(),
			// labeledObservations.schema().field(labelName), null, 0,
			// BigInteger.ONE, lines);

			coverageDecisionTree.dot();

			System.out.println(
					"STATUS: Decision tree to report coverage is built.");
		} else {
			System.out.println(
					"WARNING: The dataset is all covered/uncovered in the current setting.");
			this.coverageDecisionTree = null;
		}

		this.coverageVoronoiDiagram = null;
	}

	/**
	 * Create k Voronoi diagrams for the given sites.
	 */
	private void findVoronoi() {
		List<Point2D> point2dList = new ArrayList<Point2D>();

		for (int i = 0; i < this.dataset.size(); i++) {
			Tuple r = this.dataset.get(i);
			Point2D newP = new Point2D(r.getDouble(0), r.getDouble(1));
			point2dList.add(newP);
		} ;

		this.coverageVoronoiDiagram = new VoronoiKOrder(point2dList, k, false);
	}

	/**
	 * Visualize the coverage info
	 * 
	 * @param cc
	 * @param delta
	 * @param sampleSize
	 * @param allowInteractive
	 */
	public void view(double delta, int sampleSize,
			Map<Uiconfig, Boolean> viewConfig) {
		String title = String.format(
				"%d points in %d-d space (k=%d, theta=%.2f)",
				this.dataset.size(), this.dataset.ncols(), this.k, this.theta);

		CoverageCheckerUI chart = new CoverageCheckerUI(title, this, delta,
				sampleSize, viewConfig);
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
		if (this.coverageVoronoiDiagram != null) {
			PointSet polygonKeys = getContainingVoronoiPolyKey(x, y);
			if (polygonKeys == null)
				return false;

			// Count the number of adjacent points to the given point (x,y)
			// (distance <= theta)
			return polygonKeys.stream().mapToInt(
					p -> (p.dist2(new Point2D(x, y)) <= theta * theta) ? 1 : 0)
					.sum() >= k;
		} else if (this.coverageDecisionTree != null) {
			return this.coverageDecisionTree
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
		for (VoronoiPolygon p : coverageVoronoiDiagram.getPolygons()) {
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
