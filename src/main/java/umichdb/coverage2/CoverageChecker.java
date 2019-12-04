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

	public CoverageChecker(NDPoint[] points, int k, double theta) {
		this.sites = points;
		this.k = k;
		this.theta = theta;
		this.d = points[0].getDimensions();

		findVoronoi();
	}

	private void findVoronoi() {
		List<Point2D> point2dList = new ArrayList<Point2D>();
		for (NDPoint p : sites) {
			Point2D newP = new Point2D(p.valAtNDimension(0),
					p.valAtNDimension(1));
			point2dList.add(newP);
		}
		this.vd = new VoronoiKOrder(point2dList, k, false);
	}

	public static void view(CoverageChecker cc) {
		CoverageCheckerUI chart = new CoverageCheckerUI(
				String.format(
						"Coverage for %d random points in %d-d space. (k=%d, theta=%.2f)",
						cc.sites.length, cc.sites[0].getDimensions(), cc.k, cc.theta),
				cc);
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
		PointSet sites = getContainingVoronoiPolyKey(x, y);
		if (sites == null)
			return false;
		
		return sites.stream()
				.mapToInt(p -> (p.dist2(new Point2D(x, y)) <= theta*theta) ? 1 : 0)
				.sum() >= k;
	}

	public PointSet getContainingVoronoiPolyKey(double x, double y) {
		for (VoronoiPolygon p : vd.getPolygons()) {
			if (p.contains(x, y))
				return p.regionKey;
		}
		
		System.out.println(String.format("ERROR: (%.2f,%.2f) is not found in any voronoi polygon", x, y));
		return null;
	}

	public static void main(String[] args) {

	}
}
