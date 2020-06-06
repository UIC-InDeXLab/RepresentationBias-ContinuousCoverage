package umichdb.coverage2;

/*
Higher Order Voronoi Diagrams - Demonstration Applet
written by Andreas Pollak 2007

This program is part of my diploma thesis in computer science at 
FernUniversität Hagen, Lehrgebiet  Kooperative Systeme,
supervised by Dr. Christian Icking.

To find out more about the data structures and algoritm employed,
read my thesis ;)
*/

import java.util.*;

//Point2D is generalized point that may be
//-	in the 2-dimensional plane; in this case,
//	getX() and getY() return its coordinates, isAtInfinity() returns false;
//-	"at infinity"; in this case, getX() and getY() define
//	a vector pointing in the direction of this point; the length of this vector
//	is irrelevant, but must be positive; isAtInfinity() returns true.
//Points are orderd as follows:
//	- infinite points come first and are orderd by angle
//- then come the finite points, ordered lexicographically 
class Point2D implements Comparable<Point2D> {
	// just for consistency with compareTo
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other instanceof Point2D)
			return compareTo((Point2D) other) == 0;
		return other.equals(this);
	}
	
	/**
	 * Mark added a hashCode override here.
	 */
	@Override
    public int hashCode() {
		return Objects.hash(this.getX(), this.getY());
	}

	// required for keeping points in ordered sets and maps
	public int compareTo(Point2D other) {
		if (isAtInfinity()) {
			if (!other.isAtInfinity())
				return -1;
			return compareAngle(getX(), getY(), other.getX(), other.getY());
		}
		if (other.isAtInfinity())
			return 1;

		if (getX() < other.getX())
			return -1;
		if (getX() > other.getX())
			return 1;
		if (getY() < other.getY())
			return -1;
		if (getY() > other.getY())
			return 1;

		return 0;
	}

	// the first constructor allows for points at infinity
	public Point2D(double x, double y, boolean inf) {
		this.x = x;
		this.y = y;
		this.inf = inf;
	}
	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
		inf = false;
	}
	public Point2D(Point2D p) {
		x = p.getX();
		y = p.getY();
		inf = p.isAtInfinity();
	}

	// find out about the location...
	public double getX() {
		return x;
	}
	public double getY() {
		return y;
	}
	public boolean isAtInfinity() {
		return inf;
	}

	// the squared Euclidian distance from another point
	public double dist2(Point2D p) {
		if (isAtInfinity() || p.isAtInfinity())
			return Double.POSITIVE_INFINITY;
		return (getX() - p.getX()) * (getX() - p.getX())
				+ (getY() - p.getY()) * (getY() - p.getY());
	}

	// this static method compares the angles of two rays (dx1,dy1) and
	// (dx2,dy2)
	// it is used to to order points at infinity.
	public static int compareAngle(double dx1, double dy1, double dx2,
			double dy2) {
		/*
		 * double a1=Math.atan(dy1/dx1)+(dx1>0?4:0); double
		 * a2=Math.atan(dy2/dx2)+(dx2>0?4:0); if (a1>a2) return -1; if (a1<a2)
		 * return 1; return 0;
		 */
		int q1, q2;
		if ((dx1 >= 0) && (dy1 > 0))
			q1 = 0;
		else if ((dx1 > 0) && (dy1 <= 0))
			q1 = 1;
		else if ((dx1 <= 0) && (dy1 < 0))
			q1 = 2;
		else
			q1 = 3;
		if ((dx2 >= 0) && (dy2 > 0))
			q2 = 0;
		else if ((dx2 > 0) && (dy2 <= 0))
			q2 = 1;
		else if ((dx2 <= 0) && (dy2 < 0))
			q2 = 2;
		else
			q2 = 3;

		if (q1 < q2)
			return -1;
		if (q1 > q2)
			return 1;

		// if ((dx1==0) && (dx2==0)) return 0;
		// double
		// sl1=(dx1==0)?Double.POSITIVE_INFINITY:dy1/dx1,sl2=(dx2==0)?Double.POSITIVE_INFINITY:dy2/dx2;
		double sl1 = dy1 * dx2, sl2 = dy2 * dx1;
		if (sl1 > sl2)
			return -1;
		if (sl1 < sl2)
			return 1;
		return 0;
	}

	@Override
	public String toString() {
		return "(" + String.format("%.3f", this.getX())  + "," + String.format("%.3f", this.getY()) + ")";
	}

	// the actual information:
	private double x, y;
	private boolean inf;
}

// PointSet is an ordered set op Point2D (implemented as a
// TreeSet) with two important features:
// - PointSets are ordered by size and contents
// - they provide a convenient method to find a neighbor
class PointSet extends TreeSet<Point2D> implements Comparable<PointSet> {
	// just for consistency with compareTo
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other instanceof PointSet)
			return compareTo((PointSet) other) == 0;
		return other.equals(this);
	}

	public int compareTo(PointSet other) {
		if (size() < other.size())
			return -1;
		if (size() > other.size())
			return 1;
		for (Iterator<Point2D> i = iterator(), j = other.iterator(); i
				.hasNext();) {
			int c = i.next().compareTo(j.next());
			if (c != 0)
				return c;
		}
		return 0;
	}

	// standard constructor
	public PointSet() {
		super();
	}
	// use an alternative ordering of Point2D
	public PointSet(Comparator<Point2D> c) {
		super(c);
	}

	// finds the closest point in this set to (x,y) that is no further
	// than maxDist away from (x,y); not perfectly efficient, but reasobly
	// fast for small maxDist. Warning: O(n) for large maxDist!
	public Point2D findNeighbour(double x, double y, double maxDist) {
		SortedSet<Point2D> ss = subSet(
				new Point2D(x - maxDist, Double.NEGATIVE_INFINITY),
				new Point2D(x + maxDist, Double.POSITIVE_INFINITY));
		double maxDistSquare = maxDist * maxDist,
				closestDistSquare = Double.POSITIVE_INFINITY;
		Point2D closest = null;
		for (Iterator<Point2D> i = ss.iterator(); i.hasNext();) {
			Point2D current = i.next();
			double currentDistSquare = (current.getX() - x)
					* (current.getX() - x)
					+ (current.getY() - y) * (current.getY() - y);
			if ((currentDistSquare <= maxDistSquare)
					&& (currentDistSquare < closestDistSquare)) {
				closestDistSquare = currentDistSquare;
				closest = current;
			}
		}
		return closest;
	}
}
