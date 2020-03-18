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

public class CoverageChecker {
	VoronoiKOrder vd;
	NDPoint[] sites;
	int d;
	int k;
	double theta;

	/**
	 * Find exact coverage
	 * @param points
	 * @param k
	 * @param theta
	 */
	public CoverageChecker(NDPoint[] points, int k, double theta) {
		this.sites = points;
		this.k = k; // k points
		this.theta = theta; // max distance to qualify as adjacent
		this.d = points[0].getDimensions();
		
		// Create cache in the form of a Voronoi diagram
		findVoronoi();
	}

	/**
	 * Find approximate coverage through sampling
	 * @param points
	 * @param k
	 * @param theta
	 * @param numSamples
	 */
	public CoverageChecker(NDPoint[] points, int k, double theta,
			int s) {
		this.sites = points;
		this.k = k; // k points
		this.theta = theta; // max distance to qualify as adjacent
		this.d = points[0].getDimensions();
		
		
		//Create "s" many samples as observations to build a "decision tree" later
		NDPoint[] observations = Utils.genRandNDPoint(s, this.d);
		boolean[] labels = new boolean[observations.length];
		
		int counter = 0;
		for(NDPoint p1 : observations) {
			int neighborCount = 0;
			for (NDPoint p2 : this.sites) {
				if (NDPoint.eclideanDistance(p1, p2) <= this.theta) {
					neighborCount++;
					if (neighborCount >= k) {
						break;
					}
				}
			}
			// This is covered
			if (neighborCount >= k) {
				labels[counter++] = true;
			}
			else{ // not covered
				labels[counter++] = false;
			}
		}
		
		// Start building decision tree
		
		
	}

	/**
	 * Create k Voronoi diagrams for the given sites. 
	 */
	private void findVoronoi() {
		List<Point2D> point2dList = new ArrayList<Point2D>();
		for (NDPoint p : sites) {
			Point2D newP = new Point2D(p.getValueAt(0), p.getValueAt(1));
			point2dList.add(newP);
		}
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
		CoverageCheckerUI chart = new CoverageCheckerUI(String.format(
				"Coverage for %d random points in %d-d space. (k=%d, theta=%.2f)",
				cc.sites.length, cc.sites[0].getDimensions(), cc.k, cc.theta),
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
		PointSet polygonKeys = getContainingVoronoiPolyKey(x, y);
		if (polygonKeys == null)
			return false;

		// Count the number of adjacent points to the given point (x,y)
		// (distance <= theta)
		return polygonKeys.stream().mapToInt(
				p -> (p.dist2(new Point2D(x, y)) <= theta * theta) ? 1 : 0)
				.sum() >= k;
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
