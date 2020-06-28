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
import umichdb.coverage2.MithraCoverageCheckerUI.Uiconfig;
import smile.base.cart.DecisionNode;
import smile.base.cart.Node;
import smile.classification.DecisionTree;
import smile.base.cart.SplitRule;

public class MithraCoverageChecker implements CoverageChecker {
	VoronoiKOrder coverageVoronoiDiagram;
	DecisionTree coverageDecisionTree;
	StructType srcDataSchema;
	DataFrame dataset;
	Scaler scaler;

	int d;
	int k;
	double rho;

	static final int TAU = 10;

	/**
	 * Find exact coverage
	 * 
	 * @param rawDataset
	 * @param k
	 *            = threshold value
	 * @param rho
	 *            = vicinity value
	 */
	public MithraCoverageChecker(DataFrame rawDataset, int k, double rho) {
		// Rescaling
		scaler = Scaler.fit(rawDataset);
		this.dataset = scaler.transform(rawDataset);

		// Add some random noise to make sure all data points are unique in the
		// dataset so that the voronoi library won't fail
		Noiser noiser = Noiser.fit(this.dataset);
		this.dataset = noiser.transform(this.dataset);

		this.k = k; // k points
		this.rho = rho; // max distance to qualify as adjacent
		this.d = rawDataset.ncols();

		if (this.d != 2) {
			System.err.println(
					"[WARNING] The dimensionality of dataset is not 2. Better try approximate coverage checker");
		}

		// Create cache in the form of a Voronoi diagram
		findVoronoi();

		this.coverageDecisionTree = null;
	}

	/**
	 * Find approximate coverage through sampling
	 * 
	 * @param dataset
	 * @param k
	 *            = threshold value
	 * @param rho
	 *            = vicinity value
	 * @param epsilon
	 *            = error bound
	 * @param phi
	 *            = (1-phi) is the probability of the error bound
	 */
	public MithraCoverageChecker(DataFrame dataset, int k, double rho,
			double epsilon, double phi) {
		// Rescaling
		scaler = Scaler.fit(dataset);
		this.dataset = scaler.transform(dataset);

		this.k = k; // k points
		this.rho = rho; // max distance to qualify as adjacent
		this.d = dataset.ncols();

		// Create "s" many samples as observations to build a "decision tree"
		// later
		int numSamples = getNumSamples(epsilon, phi);
		DataFrame sampleDataset = Utils.genRandDataset(numSamples, this.d);

		// Create training data on the sampled dataset
		boolean[] labels = new boolean[sampleDataset.nrows()];
		int counter = 0;
		int numCovers = 0;
		int numUncovers = 0;

		for (int i = 0; i < sampleDataset.size(); i++) {
			int neighborCount = 0;
			for (int j = 0; j < this.dataset.size(); j++) {
				if (Utils.getEuclideanDistance(sampleDataset.get(i),
						this.dataset.get(j)) <= this.rho) {
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
			srcDataSchema = sampleDataset.schema();

			DataFrame labeledSampleDataset = sampleDataset
					.merge(BooleanVector.of(labelName, labels));
			Formula f = Formula.lhs(labelName);

			// Start building decision tree (default setting of decision tree learning)
			coverageDecisionTree = DecisionTree.fit(f, labeledSampleDataset, SplitRule.GINI , 100, 1000, 1);
//			
//			
//			int wrong = 0;
//			for (int i = 0; i< observations.size(); i++) {
//				if (labels[i] != (coverageDecisionTree.predict(observations.get(i)) == 1 ? true:false)) {
//					wrong+=1;
//				}
//			}
//			System.out.println("[debug] wrong = " + wrong +"/" +observations.size() );
			
		} else {
			if (numCovers == 0) {
				System.out.println(
						"[WARNING] The dataset is all uncovered in the current setting.");
			}
			if (numUncovers == 0) {
				System.out.println(
						"[WARNING] The dataset is all covered in the current setting.");
			}
			this.coverageDecisionTree = null;
		}

		this.coverageVoronoiDiagram = null;
	}

	/**
	 * Get number of samples given epsilon and delta using a fixed constant tau.
	 * 
	 * @param epsilon
	 * @param phi
	 * @return
	 */
	private static int getNumSamples(double epsilon, double phi) {
		return (int) Math.ceil(1 / epsilon * Math.log(1 / phi) * TAU);
	}

	/**
	 * Get number of samples given epsilon and delta, and a constant tau
	 * (times).
	 * 
	 * @param totalCount
	 * @param epsilon
	 * @param phi
	 * @return
	 */
	private static int getNumSamples(double epsilon, double phi, int tau) {
		return (int) Math.ceil(1 / epsilon * Math.log(1 / phi) * tau);
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
		}

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
		String title = String.format("%d points in %d-d space (k=%d, ρ=%.2f)",
				this.dataset.size(), this.dataset.ncols(), this.k, this.rho);

		MithraCoverageCheckerUI chart = new MithraCoverageCheckerUI(title, this,
				delta, sampleSize, viewConfig);
		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setSize(800, 800);
		chart.setVisible(true);
	}

	/**
	 * Check if point (x,y) is covered. Rescaling is not used.
	 * 
	 * @param point
	 * @return
	 */
	@Override
	public boolean ifCovered(double[] point) {
		return ifCovered(point, false);
	}

	/**
	 * Check if point (x,y) is covered
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean ifCovered(double[] point, boolean ifRescale) {

		if (ifRescale) {
			point = scaler.transform(point);
		}

		if (this.coverageVoronoiDiagram != null) {
			PointSet polygonKeys = coverageVoronoiDiagram.locate(point[0],
					point[1]);
			if (polygonKeys == null)
				return false;

			double x = point[0];
			double y = point[1];

			// Count the number of adjacent points to the given point (x,y)
			// (distance <= theta)
			return polygonKeys.stream().mapToInt(
					p -> (p.dist2(new Point2D(x, y)) <= rho * rho) ? 1 : 0)
					.sum() >= k;
		} else if (this.coverageDecisionTree != null) {
			return this.coverageDecisionTree
					.predict(Tuple.of(point, srcDataSchema)) == 0
							? false
							: true;
		}
		return false;
	}

	public static void main(String[] args) {

	}
}
