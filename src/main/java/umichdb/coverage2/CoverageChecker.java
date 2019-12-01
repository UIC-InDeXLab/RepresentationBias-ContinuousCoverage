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
			Point2D newP = new Point2D(p.valAtNDimension(0), p.valAtNDimension(1));
			point2dList.add(newP);
		}
		this.vd = new VoronoiKOrder(point2dList, k, false);
	}
	

	public static void main(String[] args) {

	}
}
