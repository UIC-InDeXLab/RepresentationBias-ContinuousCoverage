package umichdb.coverage2;

import java.awt.Polygon;

/*
Higher Order Voronoi Diagrams - Demonstration Applet
written by Andreas Pollak 2007

This program is part of my diploma thesis in computer science at 
FernUniversität Hagen, Lehrgebiet  Kooperative Systeme,
supervised by Dr. Christian Icking.

To find out more about the data structures and algoritm employed,
read my thesis ;)

change history:
	2007-08-04	A. Pollak
			changed VVertex.inCriticalArea to allow for numerical inacuracies
			of a certain relative rather than absoulute size, making it
			independent of the scale of the underlying nodes	

	2019-11-10 A. Pollak	
			converted form Applet to App
			substituted deprecated functions (replaces Integer and Double constructors with valueOf)
*/

import java.util.*;

//KCircle implements common properties of edges and vertices:
//it may be checked if a point is relevant or critical, and 
//it may be chosen if relevant points should be inside (near diagrams)
//or outside (far diagrams) the circle given by the critical points
interface KCircle {
	boolean isCritical(Point2D p);
	boolean isRelevant(Point2D p);

	boolean relevantInside();
	// setRelevantInside should check if the requested value
	// of inside corresponds to the current state. If not, the
	// new set of relevant points can ve computed from the set
	// of all points S as: relevant=S-relevant-critical
	void setRelevantInside(boolean inside, PointSet S);
}

// VEdge represents an edge of a higher order Voronoi diagram.
// The information held by this class is determined by the needs of
// the algorithm implemented in VoronoiKOrder.
// Edges are characterized by two critical and a set of relevant points.
// Usually, they are also connected to two vertices v1 and v2.
// Edges are ordered by their critical and relevant points.
// v1 and v2 are null whilst unconnected. If both are null,
// the variable pointInside must be defined.
// Legal state of VEdge are:
// (1) v1==null, v2==null, pointInside!=null
// (2) v1!=null, v2==null
// (3) v1!=null, v2!=null
// The order of the critical points critical1 and critical2 is important,
// as it defines the direction in which their bisector is to be
// searched for the next vertex (as long as v1 or v2 is null).
// Therefore, this order is switched whenever a new vertex is connected
// or disconnected.
class VEdge implements Comparable<VEdge>, KCircle {
	// the state of the edge:
	public PointSet relevant;
	public VVertex v1, v2;
	public Point2D critical1, critical2;
	public Point2D pointInside;

	private Set<VEdge> edges;
	private TreeMap<VEdge, VEdge> todo;
	private boolean relevantInside;
	
	// constructor: the critical points must be provided at construction time;
	// the sets todo and edges
	// are provided so that an edge can automatically remove or add itself when
	// a vertex is connected or
	// disconnected. After construction, the new edge must be added to todo
	// manually once the relevant
	// points have been added.
	public VEdge(Point2D p1, Point2D p2, TreeMap<VEdge, VEdge> todo,
			Set<VEdge> edges, boolean relevantInside) {
		this.edges = edges;
		this.todo = todo;
		critical1 = p1;
		critical2 = p2;
		v1 = v2 = null;
		relevant = new PointSet();
		pointInside = null;
		this.relevantInside = relevantInside;
	}

	/**
	 * To print a VEdge
	 */
	public String toString() {
		return String.format(
				"Crtitical(%s,%s); End points(%s,%s); Relevant(%s); PtsInside(%s), #edges(%d)",
				this.critical1, this.critical2, this.v1, this.v2, this.relevant,
				this.pointInside, this.edges.size());
	}

	// for consitency with compareTo
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other instanceof VEdge)
			return compareTo((VEdge) other) == 0;
		return other.equals(this);
	}

	public int compareTo(VEdge other) {
		int c = c1().compareTo(other.c1());
		if (c != 0)
			return c;
		c = c2().compareTo(other.c2());
		if (c != 0)
			return c;
		return relevant.compareTo(other.relevant);
	}

	// these functions return the smaller and bigger of the two critical points
	// they should only be used by compareTo
	public Point2D c1() {
		if (critical1.compareTo(critical2) < 0)
			return critical1;
		return critical2;
	}
	public Point2D c2() {
		if (critical1.compareTo(critical2) < 0)
			return critical2;
		return critical1;
	}

	// the following methods are required by the interface KCircle
	public boolean isCritical(Point2D p) {
		if (p == null)
			return false;
		return (p.compareTo(critical1) == 0) || (p.compareTo(critical2) == 0);
	}
	public boolean isRelevant(Point2D p) {
		return relevant.contains(p);
	}

	public boolean relevantInside() {
		return relevantInside;
	}
	public void setRelevantInside(boolean inside, PointSet S) {
		if (inside == relevantInside)
			return;
		relevantInside = inside;
		PointSet newRelevant = new PointSet();
		for (Iterator<Point2D> i = S.iterator(); i.hasNext();) {
			Point2D p = i.next();
			if (!relevant.contains(p))
				newRelevant.add(p);
		}
		newRelevant.remove(critical1);
		newRelevant.remove(critical2);
		relevant = newRelevant;
	}

	// switch the critical points when a vertex is (dis)connected
	private void flip() {
		Point2D s = critical1;
		critical1 = critical2;
		critical2 = s;
	}

	// inform the edge that a new set of edges should be used:
	public void setEdges(Set<VEdge> edges) {
		this.edges = edges;
	}

	// connect a vertex:
	public void connect(VVertex v) {
		if (v1 == null) {
			v1 = v;
			flip();
		} else {
			v2 = v;
			todo.remove(this);
			edges.add(this);
		}
	}

	// disconnect a vertex:
	public void disconnect(VVertex v) {
		if ((v2 != null) && (v.compareTo(v2) == 0)) {
			v2 = null;
			edges.remove(this);
			todo.put(this, this);
		} else if ((v1 != null) && (v.compareTo(v1) == 0)) {
			if (v2 == null)
				todo.remove(this);
			else {
				v1 = v2;
				v2 = null;
				edges.remove(this);
				todo.put(this, this);
				flip();
			}
		}
	}

	// report the vertex at the other end of the edge; useful for
	// moving through the finished diagram.
	public VVertex getOtherVertex(VVertex v) {
		if ((v1 != null) && (v.compareTo(v1) == 0))
			return v2;
		if ((v2 != null) && (v.compareTo(v2) == 0))
			return v1;
		return null;
	}

	// replace a point in the set of relevant points; this is
	// used when a point is moved; as this affects the ordering
	// the edge must be removed from the respective ordered set
	// before the operation.
	public void replaceRelevant(Point2D po, Point2D pn) {
		if (!relevant.contains(po))
			return;
		if (v2 != null)
			edges.remove(this);
		else
			todo.remove(this);
		relevant.remove(po);
		relevant.add(pn);
		if (v2 != null)
			edges.add(this);
		else
			todo.put(this, this);
	}

}

// VVertex is the a vertex of a higher order Voronoi diagram. It is
// derived from Point2D and inherits the ordering of points. Vertices may
// be located at infinity: the algorithm implemented in VoronoiKOrder
// lets unbounded edges end at such vertices. In each direction, there is only
// one
// vertex at infinity (i.e. diagrams with collinear points are connected, as
// they have exactly two vertices). This feature makes the VVertex class
// slightly more
// complex but spares us some additional considerations elsewhere.
// Creation of a new VVertex takes three steps
// (1) construct an instance
// (2) set up the sets of relevant and critical points by adding points
// (3) invoke the complete(...) method (see below)
class VVertex extends Point2D implements KCircle {

	public VVertex(Point2D p, boolean relevantInside) {
		super(p);
		this.relevantInside = relevantInside;
		critical = new PointSet();
		relevant = new PointSet();
	}

	/**
	 * This method should be called after the vertex has been constructed and
	 * all points have been added to the critical and relevant sets. It checks
	 * the todo set for incident edges (constructing them if they don't exist)
	 * and connects them. The cases of finite and infinite vertices are treated
	 * differently.
	 * 
	 * @param n
	 * @param k
	 * @param todo
	 * @param edges
	 */
	public void complete(int n, int k, TreeMap<VEdge, VEdge> todo,
			Set<VEdge> edges) {
		edgeList = new Vector<VEdge>();

		Point2D orderedCritical[] = new Point2D[critical.size()];
		PointSet sortedCritical;

		// if the vertex is at infinity, the lexicographic order is fine;
		// otherwise
		// sort by angle

		if (isAtInfinity()) {
			sortedCritical = critical;
		} else {
			sortedCritical = new PointSet(new AngleComparator(this));
			sortedCritical.addAll(critical);
		}

		Iterator<Point2D> j = sortedCritical.iterator();

		for (int i = 0; i < critical.size(); i++) {
			orderedCritical[i] = j.next();
		}
		int distance = relevantInside
				? (k - relevant.size())
				: (k - (n - critical.size() - relevant.size()));

		if (isAtInfinity()) {
			double dx = critical.last().getX() - critical.first().getX(),
					dy = critical.last().getY() - critical.first().getY();
			boolean side = getX() * dy - getY() * dx > 0; // side indicates on
															// which side of the
															// line (given by
															// the ordered
															// critical pints)
															// the vertex lies

			for (int i = 0; i < critical.size() - distance; i++) {
				// construct edge
				VEdge newEdge = side
						? new VEdge(orderedCritical[i + distance],
								orderedCritical[i], todo, edges, relevantInside)
						: new VEdge(orderedCritical[i],
								orderedCritical[i + distance], todo, edges,
								relevantInside);
				newEdge.relevant.addAll(relevant);
				if (relevantInside)
					for (int l = 1; l < distance; l++)
						newEdge.relevant.add(orderedCritical[i + l]);
				else
					for (int l = 1; l < critical.size() - distance; l++)
						newEdge.relevant.add(orderedCritical[(i + distance + l)
								% critical.size()]);
				// check if it exists
				VEdge existingEdge = todo.get(newEdge);
				// connect it
				if (existingEdge == null) {
					todo.put(newEdge, newEdge);
					newEdge.connect(this);
					edgeList.add(newEdge);
				} else {
					existingEdge.connect(this);
					edgeList.add(existingEdge);
				}
			}
		} else // vertex not at infinity
		{
			for (int i = 0; i < critical.size(); i++) {
				// construct edge
				VEdge newEdge = new VEdge(
						orderedCritical[(i + distance) % critical.size()],
						orderedCritical[i], todo, edges, relevantInside);
				newEdge.relevant.addAll(relevant);
				if (relevantInside)
					for (int l = 1; l < distance; l++)
						newEdge.relevant.add(
								orderedCritical[(i + l) % critical.size()]);
				else
					for (int l = 1; l < critical.size() - distance; l++)
						newEdge.relevant.add(orderedCritical[(i + distance + l)
								% critical.size()]);
				// check if it exists
				VEdge existingEdge = todo.get(newEdge);
				// connect it
				if (existingEdge == null) {
					todo.put(newEdge, newEdge);
					newEdge.connect(this);
					edgeList.add(newEdge);
				} else {
					existingEdge.connect(this);
					edgeList.add(existingEdge);
				}
			}
		}
	}

	// these methods implement the KCircle interface:
	public boolean isCritical(Point2D p) {
		return critical.contains(p);
	}
	public boolean isRelevant(Point2D p) {
		return relevant.contains(p);
	}
	public boolean relevantInside() {
		return relevantInside;
	}
	public void setRelevantInside(boolean inside, PointSet S) {
		if (inside == relevantInside)
			return;
		relevantInside = inside;
		PointSet newRelevant = new PointSet();
		for (Iterator<Point2D> i = S.iterator(); i.hasNext();) {
			Point2D p = i.next();
			if (!relevant.contains(p) && !critical.contains(p))
				newRelevant.add(p);
		}
		relevant = newRelevant;
	}

	// check wheter a new point (not yet in the set S) is in the relevant or
	// critical area:
	public boolean inCriticalArea(Point2D p) {
		// This method allows for numerical inaccuracies to really make
		// sure that a vertex is destroyed when a new critical node is inserted.
		if (isAtInfinity())
			return Math.abs(VoronoiKOrder.distance(critical.first(),
					critical.last(),
					p)) <= 10E-10 * ((Math.abs(critical.first().getY())
							+ Math.abs(critical.last().getY()))
							* (Math.abs(p.getX())
									+ Math.abs(critical.first().getX()))
							+ (Math.abs(critical.first().getX())
									+ Math.abs(critical.last().getX()))
									* (Math.abs(p.getY()) + Math
											.abs(critical.first().getY())));
		double d1 = dist2(critical.first()), d2 = dist2(p);
		return Math.abs(d1 - d2) <= 10E-10 * (d1 + d2);
	}
	public boolean inRelevantArea(Point2D p) {
		if (isAtInfinity()) {
			double side = VoronoiKOrder.distance(critical.first(),
					critical.last(), p)
					* VoronoiKOrder.distance(critical.first(), critical.last(),
							new Point2D(critical.first().getX() + getX(),
									critical.first().getY() + getY()));
			if (relevantInside)
				return side > 0;
			return side < 0;
		}
		if (relevantInside)
			return dist2(critical.first()) > dist2(p);
		return dist2(critical.first()) < dist2(p);
	}

	public Point2D getP() {
		return this;
	}

	// return the sets of critical and relevant points; these sets
	// are filled by the function that created the vertex before
	// calling complete()
	public PointSet getCritical() {
		return critical;
	}
	public PointSet getRelevant() {
		return relevant;
	}

	// the list of connected edges; available after complete() has been called
	public Vector<VEdge> getEdgeList() {
		return edgeList;
	}

	@Override
	public String toString() {
		return String.format("[%f,%f]", this.getX(), this.getY());
	}

	// the private variables:
	PointSet critical, relevant;
	boolean relevantInside;

	Vector<VEdge> edgeList;
}

// A polygon class for Voronoi diagram
class VoronoiPolygon extends Polygon2D {
	PointSet regionKey;
	public VoronoiPolygon() {
		super();
		regionKey = new PointSet();
	}
	public VoronoiPolygon(PointSet points) {
		super();
		regionKey = points;
	}

	public String toString() {
		return "Key: " + regionKey + " Vertex: " + super.toString();
	}
}

// VoronoiKOrder contains the data structures and methods to
// create and describe (higher order) Voronoi diagrams. This includes
// the set of points S. Methods are provided to
// - add a point (addPoint)
// - remove a point (removePoint)
// - change the position of a point (movePoint)
// - remove all points (clear)
// - change the order of the diagram (changeOrder)
// - switch between far and near mode (setMaintainedOrder)
// All these methods update the diagram automatically.
// To completely recompute the diagram, one may also call
// changeOrder. Adding and removing points works differently in near and
// far mode: in near mode, the order k is held constant (if possible), whereas
// in far mode it is increased when a point is added and decreased when a point
// is removed.
// The algorithm is described in my thesis; I'd just like to note that it
// constructs
// a diagram from scratch in O(k(n-k)nlogn), but is usually faster
// adding/removing/
// moving points.
// VoronoiKOrder contains four important sets:
// - S contains the underlying points
// - edges and vertices contain the elements of the finished diagram; note that
// by the
// ordering of Point2D (and thus VVertex), the vertices at infinity come first,
// so
// iterating over the vertices set is a convenient way of obtaining the
// unbounded edges
// - todo contains edges that have not yet been connected to two vertices; this
// set only contains objects during the construction of a diagram
class VoronoiKOrder {
	// these variables contain all relevant information about the diagram
	int k;
	boolean kIsConstant;
	PointSet sites;
	TreeSet<VEdge> edges;
	PointSet vertices;

	// Add polygons dictated by key point set
	HashMap<PointSet, VoronoiPolygon> polygonKeyToPolygon;

	// todo is only used during the construction
	TreeMap<VEdge, VEdge> todo;

	// create an empty diagram:
	public VoronoiKOrder() {
		kIsConstant = true;
		k = 1;
		sites = new PointSet();
		edges = new TreeSet<VEdge>();
		vertices = new PointSet();

		todo = new TreeMap<VEdge, VEdge>();

		polygonKeyToPolygon = new HashMap<PointSet, VoronoiPolygon>();
	}

	// create diagram for the points in S:
	public VoronoiKOrder(Collection<Point2D> S, int k, boolean nMinusK) {
		kIsConstant = !nMinusK;
		this.k = k;
		if (this.k <= 0)
			this.k = 1;
		else if (this.k >= S.size())
			this.k = S.size() - 1;
		this.sites = new PointSet();
		this.sites.addAll(S);
		edges = new TreeSet<VEdge>();
		vertices = new PointSet();

		polygonKeyToPolygon = new HashMap<PointSet, VoronoiPolygon>();

		todo = new TreeMap<VEdge, VEdge>();

		createGraph();
	}

	// this function returns the center of the circle through the three points;
	// it may be at infinity
	// if the points are collinear
	private Point2D circleCenter(Point2D p1, Point2D p2, Point2D p3) {
		double w1 = p2.getY() - p1.getY(), w2 = p1.getX() - p2.getX(),
				v1 = p3.getY() - p2.getY(), v2 = p2.getX() - p3.getX();
		double det = v2 * w1 - v1 * w2;
		if (det == 0)
			return new Point2D(w1, w2, true);
		double n1 = (p1.getX() + p2.getX()) / 2,
				n2 = (p1.getY() + p2.getY()) / 2,
				m1 = (p3.getX() + p2.getX()) / 2,
				m2 = (p3.getY() + p2.getY()) / 2;
		double f = (v2 * (m1 - n1) - v1 * (m2 - n2)) / det;
		return new Point2D(n1 + f * w1, n2 + f * w2);
	}

	// a signed measure of the distance of x from the line through p1 and p2;
	// WARNING: the scale
	// depends on p1 and p2!
	public static double distance(Point2D p1, Point2D p2, Point2D x) {
		return (p1.getY() - p2.getY()) * (x.getX() - p1.getX())
				- (p1.getX() - p2.getX()) * (x.getY() - p1.getY());
	}

	// complete a vertex and add it to the set vertices
	private void addVertex(VVertex v) {
		v.complete(sites.size(), k, todo, edges);
		vertices.add(v);
	}

	// this method finds a first edge (or vertex) that is needed to
	// get the algorithm started and adds it to todo. O(n) time.
	private void findStartingEdge() {
		if (k + 1 > sites.size())
			return; // nothing to do...

		// the method proceeds in four steps:
		// (1) find a point "center" that is not in S and sort all points in
		// S by their distance to this point:
		TreeMap<Double, PointSet> dist = new TreeMap<Double, PointSet>();
		Iterator<Point2D> i = sites.iterator();
		Point2D center;
		do {
			center = new Point2D(0.5, Math.random());
		} while (sites.contains(center));
		while (i.hasNext()) {
			Point2D next = i.next();
			Double newKey = Double.valueOf(center.dist2(next));
			if (!dist.containsKey(newKey))
				dist.put(newKey, new PointSet());
			dist.get(newKey).add(next);
		}

		// (2) Using this information "inflate" a circle around "center"
		// that has at least on point on its border and k+1 on its border
		// and inside; points on the border are stored in the set C, those
		// inside in H
		PointSet H = new PointSet(), C = new PointSet();
		Iterator<PointSet> j = dist.values().iterator();
		do {
			H.addAll(C);
			C.clear();
			C.addAll(j.next());
		} while (H.size() + C.size() < k + 1);

		Point2D circleCenter = center;

		// (3) If C contains at least 2 points, "center" already lies on an
		// edge or vertex and we're fine. If not, "deflate" the circle
		// until there are at least 2 points on its border.
		if (C.size() < 2) {
			Point2D onBorder = C.first();
			C.clear();
			PointSet toCheck = new PointSet();
			toCheck.addAll(H);
			H.clear();

			double maxDist = 0;
			double w1 = center.getX() - onBorder.getX(),
					w2 = center.getY() - onBorder.getY();
			double n1 = onBorder.getX(), n2 = onBorder.getY();
			double f;
			for (i = toCheck.iterator(); i.hasNext();) {
				Point2D current = i.next();

				double v1 = current.getY() - onBorder.getY(),
						v2 = onBorder.getX() - current.getX();
				double det = (v2 * w1 - v1 * w2);
				double m1 = (current.getX() + onBorder.getX()) / 2,
						m2 = (current.getY() + onBorder.getY()) / 2;
				f = (v2 * (m1 - n1) - v1 * (m2 - n2)) / det;
				Point2D currentCircleCenter = new Point2D(n1 + f * w1,
						n2 + f * w2);
				f = f * f;
				if (f > maxDist) {
					H.addAll(C);
					C.clear();
					C.add(current);
					maxDist = f;
					circleCenter = currentCircleCenter;
				} else if (f < maxDist)
					H.add(current);
				else
					C.add(current);
			}
			C.add(onBorder);
		}

		// (4) Check if we have fond an edge (2 points on the border)
		// or a vertex (more then 2 points); add the respective object.
		if (C.size() > 2) {
			VVertex firstVertex = new VVertex(circleCenter, true);
			firstVertex.getCritical().addAll(C);
			firstVertex.getRelevant().addAll(H);
			firstVertex.setRelevantInside(kIsConstant, sites);
			addVertex(firstVertex);
		} else {
			VEdge firstEdge = new VEdge(C.first(), C.last(), todo, edges, true);
			firstEdge.relevant.addAll(H);
			firstEdge.setRelevantInside(kIsConstant, sites);
			firstEdge.pointInside = circleCenter;
			todo.put(firstEdge, firstEdge);
		}
	}

	// Check if vertex v is affected by removing point po from S and adding pn
	// Either po or pn may be null.
	private boolean isAffected(VVertex v, Point2D po, Point2D pn) {
		if ((po != null) && (v.isCritical(po) || v.isRelevant(po)))
			return true;
		if ((pn != null) && (v.inRelevantArea(pn) || v.inCriticalArea(pn)))
			return true;
		return false;
	}

	// minorChange checks if replaceing po by pn has a structural effect on
	// the vertex v, or if it's enough to replace the point in the "relevant"
	// set.
	private boolean minorChange(VVertex v, Point2D po, Point2D pn) {
		if ((po == null) || (pn == null))
			return false;
		if (v.isRelevant(po) && v.inRelevantArea(pn))
			return true;
		return false;
	}

	// removeGraph deletes the part of the diagram that must be redrawn when
	// po is removed and/or pn added. During the removel process, patly
	// connected
	// edges are automatically added to the "todo" list, so the algoritm that
	// constructs the new diagram can start right away.
	private void removeGraph(Point2D po, Point2D pn) {
		// The following two lines have been commented out; if you leave them
		// in,
		// the diagram will always be fully recomputed.
		// edges.clear();
		// vertices.clear();

		if (vertices.size() == 0)
			return;

		// Try to find at least one vertex that is affected by (1) removing po
		// (2) adding pn
		boolean findPo = po != null, findPn = pn != null;

		// start by checking the vertices at infinity; there are not many of
		// them
		// add affected vertices to the "checkMe" list.
		// Check the thesis to find out why this is a clever thing to do.
		TreeSet<VVertex> checkMe = new TreeSet<VVertex>();
		boolean done = false;
		for (Iterator<Point2D> i = vertices.iterator(); i.hasNext() && !done;) {
			VVertex v = (VVertex) i.next();
			if (!v.isAtInfinity())
				done = true;
			else {
				if ((po != null) && (v.isCritical(po) || v.isRelevant(po))) {
					findPo = false;
					checkMe.add(v);
				}
				if ((pn != null)
						&& (v.inRelevantArea(pn) || v.inCriticalArea(pn))) {
					findPn = false;
					checkMe.add(v);
				}
			}
		}

		// If the required vertices have not been found, walk through the
		// diagram towards po/pn
		// starting from a vertex at infinity. Doing this, we will always find a
		// vertex that is affected
		// by the change if one exists. The algorithm used here is not efficient
		// if there is no such
		// vertex (it may visit most or all vertices!), but it's ok for this
		// purpose.
		while (findPo || findPn) {
			// what are we searching for?
			Point2D target;
			boolean searchingForPo = false, searchingForPn = false;
			if (findPo) {
				target = po;
				searchingForPo = true;
			} else {
				target = pn;
				searchingForPn = true;
			}

			// strategy: at each vertex, consider all edges that lead in the
			// right direction; add the
			// vertex at their other end to a stack; get your next vertex from
			// the top of this stack;
			// continue until you've found wahat your're looking for or the
			// stack is empty.
			VVertex starting = (VVertex) vertices.first();
			TreeSet<VVertex> visited = new TreeSet<VVertex>();
			Vector<VVertex> otherWays = new Vector<VVertex>();
			while ((starting != null) && ((findPo && searchingForPo)
					|| (findPn && searchingForPn))) {
				visited.add(starting);
				Vector<VEdge> edgeList = starting.getEdgeList();
				for (int i = 0; i < edgeList.size(); i++) {
					VVertex newVertex = edgeList.get(i)
							.getOtherVertex(starting);
					if (!newVertex.isAtInfinity()
							&& !visited.contains(newVertex)
							&& (starting.isAtInfinity() || ((newVertex.getX()
									- starting.getX())
									* (target.getX()
											- starting.getX())
									+ (newVertex.getY() - starting.getY())
											* (target.getY()
													- starting.getY()) >= 0)))
						otherWays.add(newVertex);
				}
				starting = (otherWays.size() > 0)
						? otherWays.remove(otherWays.size() - 1)
						: null;
				if (starting != null) {
					if ((po != null) && (starting.isCritical(po)
							|| starting.isRelevant(po))) {
						findPo = false;
						checkMe.add(starting);
					}
					if ((pn != null) && (starting.inRelevantArea(pn)
							|| starting.inCriticalArea(pn))) {
						findPn = false;
						checkMe.add(starting);
					}
				}
			}

			// even if we haven't found anything, we do not want to search
			// again:
			findPo &= !searchingForPo;
			findPn &= !searchingForPn;
		}

		// Now we have at least one vertex from every part of the diagram that
		// must be destroyed in
		// the "checkMe" set. We now consider each of them:
		// (1) if the vertex must be removed, it is disconnected and destroyed
		// (2) if it's enough to replace a point in its "relevant" set (po by
		// pn),
		// this is done
		// (3) in any case, all neighbouring vertices are added to "checkMe"
		TreeSet<VVertex> checked = new TreeSet<VVertex>();
		while (checkMe.size() > 0) {
			VVertex next = checkMe.first();
			checkMe.remove(next);
			if (isAffected(next, po, pn)) {
				if (minorChange(next, po, pn)) // only change the "relevant" set
				{
					next.getRelevant().remove(po);
					next.getRelevant().add(pn);
					Vector<VEdge> edgeList = next.getEdgeList();
					for (int i = 0; i < edgeList.size(); i++) {
						VVertex other = edgeList.get(i).getOtherVertex(next);
						if ((other != null) && !checked.contains(other))
							checkMe.add(other);
						edgeList.get(i).replaceRelevant(po, pn);
					}
				} else // remove it completely
				{
					vertices.remove(next);
					Vector<VEdge> edgeList = next.getEdgeList();
					for (int i = 0; i < edgeList.size(); i++) {
						VVertex other = edgeList.get(i).getOtherVertex(next);
						if ((other != null) && !checked.contains(other))
							checkMe.add(other);
						edgeList.get(i).disconnect(next);
					}
				}
			}
			checked.add(next);
		}
	}

	// this is only to improve error robustness
	public void createGraph() {
		createGraph(false);
	}

	// This is where the actual construction takes place
	private void createGraph(boolean error) {
		// if there is nothing in the "todo" list, try to find a starting edge
		if ((todo.size() == 0) && (sites.size() > k) && (edges.size() == 0))
			findStartingEdge();

		// This is to prevent the algorithm from hanging if something goes
		// wrong.
		// emergencyStop is the maximum number of iterations allowed until the
		// loop is forcefully stopped. To jump without parachute, set
		// emergencyStop=-1.
		int emergencyStop = (sites.size() * (sites.size() - k) + 100) * 3;

		while (todo.size() > 0) {
			// Again, the emergency stop. If the maximum number of iterations is
			// reached, we give it a second try, but this time we re-compute the
			// diagram
			// from scratch.
			// If this fails as well, we return an empty diagram. So, if you
			// move a point around
			// and suddenly the whole diagram disappears, you've probably found
			// a bug.
			if (--emergencyStop == 0) {
				todo.clear();
				edges.clear();
				vertices.clear();
				System.out.println("Vironoi diagram construction ERROR");
				if (!error)
					createGraph(true);
				return;
			}

			// take an arbitrary ege from the "todo" list
			VEdge current = todo.get(todo.firstKey());
			PointSet C = new PointSet();

			// if we don't find anything, our next vertex will be at infinity:
			Point2D circleCenter = new Point2D(
					current.critical1.getY() - current.critical2.getY(),
					current.critical2.getX() - current.critical1.getX(), true);
			double minDist = Double.POSITIVE_INFINITY;
			// cutOffPoint and cutOffDist will tell us where to start searching
			// on the bisector of current.critical1 and current.critical2
			// excludeCritical is used to acoid checking critical points of the
			// current edge (or the vertex from which we start to search);
			// this is important for numerical reasons and spares us from
			// detecting serious vertices
			Point2D cutOffPoint;
			KCircle excludeCritical;
			if (current.v1 == null) {
				cutOffPoint = current.pointInside;
				excludeCritical = current;
			} else {
				cutOffPoint = current.v1.getP();
				excludeCritical = (current.v1.isAtInfinity())
						? current
						: current.v1;
			}
			double cutOffDist = cutOffPoint.isAtInfinity()
					? Double.NEGATIVE_INFINITY
					: distance(current.critical1, current.critical2,
							cutOffPoint);

			// Now we check all points in S (except those identified by
			// "excludeCritical") to find the next vertex on our edge.
			// That's O(n) of course, so check my thesis to find out about
			// improving this critical part of the algorithm
			// making use of a first order Voronoi diagram.
			for (Iterator<Point2D> i = sites.iterator(); i.hasNext();) {
				Point2D testPoint = i.next();
				if (!excludeCritical.isCritical(testPoint)) // should the point
															// be checked?
				{
					Point2D currentCC = circleCenter(current.critical1,
							current.critical2, testPoint);
					// check if the vertex implied by current.critical1,
					// current.critical2, and currentCC
					// is the closest one to our starting point "cutOffPoint" at
					// "cutOffDist" so far:
					if (currentCC.isAtInfinity()) {
						if (minDist == Double.POSITIVE_INFINITY)
							C.add(testPoint);
					} else {
						double currentDistance = distance(current.critical1,
								current.critical2, currentCC);
						if (currentDistance == minDist)
							C.add(testPoint);
						else if ((currentDistance < minDist)
								&& (currentDistance > cutOffDist)) {
							C.clear();
							C.add(testPoint);
							minDist = currentDistance;
							circleCenter = currentCC;
						}
					}
				}
			}

			// That was it. Simply create the new Vertex, and we're done!
			VVertex newVertex = new VVertex(circleCenter, kIsConstant);
			newVertex.getRelevant().addAll(current.relevant);
			for (Iterator<Point2D> i = C.iterator(); i.hasNext();) {
				Point2D c = i.next();
				newVertex.getRelevant().remove(c);
				newVertex.getCritical().add(c);
			}
			newVertex.getCritical().add(current.critical1);
			newVertex.getCritical().add(current.critical2);

			addVertex(newVertex);
		}
	}

	// the public interface of this class begins here.

	// add a point to S and recompute the diagram
	public void addPoint(Point2D p) {
		if (sites.contains(p))
			return;
		removeGraph(null, p);
		sites.add(p);
		if (!kIsConstant && (k < sites.size() - 1))
			k++;
		createGraph();
	}

	// remove a point from S and recompute the diagram
	public void removePoint(Point2D p) {
		if (!sites.contains(p))
			return;
		removeGraph(p, null);
		sites.remove(p);
		if (!kIsConstant && (k > 1))
			k--;
		createGraph();
	}

	// move a point and recompute the diagram
	public void movePoint(Point2D po, Point2D pn) {
		if (!sites.contains(po) || sites.contains(pn))
			return;
		removeGraph(po, pn);
		sites.remove(po);
		sites.add(pn);
		createGraph();
	}

	// clear S and the diagram
	public void clear() {
		sites.clear();
		edges.clear();
		vertices.clear();
		k = 1;
	}

	// switch between near and far mode
	public void setMaintainedOrder(boolean nMinusK) {
		if (kIsConstant != nMinusK)
			return;
		kIsConstant = !nMinusK;
		TreeSet<VEdge> newEdges = new TreeSet<VEdge>();
		while (edges.size() > 0) {
			VEdge e = edges.first();
			edges.remove(e);
			e.setRelevantInside(kIsConstant, sites);
			e.setEdges(newEdges);
			newEdges.add(e);
		}
		edges = newEdges;
		for (Iterator<Point2D> i = vertices.iterator(); i.hasNext();)
			((VVertex) (i.next())).setRelevantInside(kIsConstant, sites);
	}

	// change the order of the diagram and recompute it
	public void changeOrder(int newK) {
		k = newK;
		if ((k > 1) && (k > sites.size() - 1)) {
			k = Math.max(1, sites.size() - 1);
			System.out.print("WARNING: " + newK + ">" + (sites.size() - 1)
					+ "; k is set to " + k);
		}
		if (k < 1)
			k = 1;
		edges.clear();
		vertices.clear();
		createGraph();
	}

	/**
	 * Discover all polygons in the Voronoi graph Each edge will create two
	 * polygons. We take one critical point plus all relevant points as the key
	 * to each polygon.
	 * 
	 * Mark created
	 */
	void findPolygons() {
		if (this.polygonKeyToPolygon.isEmpty()) {
			HashMap<PointSet, Set<Point2D>> tempPolyVertex = new HashMap<PointSet, Set<Point2D>>();

			for (VEdge e : this.edges) {

				double i1 = e.v1.getX(), i2 = e.v1.getY(), i3 = e.v2.getX(),
						i4 = e.v2.getY();
				double x1 = i1, y1 = i2, x2 = i3, y2 = i4;

				if (e.v1.isAtInfinity() && e.v2.isAtInfinity()) {
					double dx = (i2 - i4) * 5000;
					double dy = (i3 - i1) * 5000;
					x1 = (i1 + i3) / 2 + dx;
					y1 = (i2 + i4) / 2 + dy;
					x2 = (i1 + i3) / 2 - dx;
					y2 = (i2 + i4) / 2 - dy;
				} else if (e.v1.isAtInfinity()) {
					x1 = i3;
					y1 = i4;
					x2 = i3 + i1 * 5000;
					y2 = i4 + i2 * 5000;
				} else if (e.v2.isAtInfinity()) {
					x1 = i1;
					y1 = i2;
					x2 = i1 + i3 * 5000;
					y2 = i2 + i4 * 5000;
				} else {
					x1 = i1;
					y1 = i2;
					x2 = i3;
					y2 = i4;
				}

				PointSet polygon1Key = new PointSet();
				polygon1Key.add(e.critical1);
				polygon1Key.addAll(e.relevant);
				Set<Point2D> poly1 = tempPolyVertex.getOrDefault(polygon1Key,
						new HashSet<Point2D>());

				poly1.add(new Point2D(x1, y1));
				poly1.add(new Point2D(x2, y2));
				tempPolyVertex.put(polygon1Key, poly1);

				PointSet polygon2Key = new PointSet();
				polygon2Key.add(e.critical2);
				polygon2Key.addAll(e.relevant);
				Set<Point2D> poly2 = tempPolyVertex.getOrDefault(polygon2Key,
						new HashSet<Point2D>());
				poly2.add(new Point2D(x1, y1));
				poly2.add(new Point2D(x2, y2));
				tempPolyVertex.put(polygon2Key, poly2);
			}

			// Add polygons
			for (Map.Entry<PointSet, Set<Point2D>> e : tempPolyVertex
					.entrySet()) {

				VoronoiPolygon poly = new VoronoiPolygon(e.getKey());
				List<Point2D> vertices = new ArrayList<Point2D>(e.getValue());
				// Sort vertices clockwise
				double xMean = vertices.stream().mapToDouble(v -> v.getX())
						.average().orElse(0);
				double yMean = vertices.stream().mapToDouble(v -> v.getY())
						.average().orElse(0);
				Point2D center = new Point2D(xMean, yMean);

				Collections.sort(vertices, (a, b) -> {

					double angle0 = angleToX(center.getX(), center.getY(),
							a.getX(), a.getY());
					double angle1 = angleToX(center.getX(), center.getY(),
							b.getX(), b.getY());
					return Double.compare(angle1, angle0);
				});

				// Sort vertices (end)
				vertices.stream()
						.forEach(v -> poly.addPoint(v.getX(), v.getY()));

				this.polygonKeyToPolygon.put(e.getKey(), poly);
			}
		}
	}

	private static double angleToX(double x0, double y0, double x1, double y1) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		double angleRad = Math.atan2(dy, dx);
		return angleRad;
	}

	/**
	 * Get all polygons in this Voronoi diagram
	 * 
	 * @return
	 */
	public Collection<VoronoiPolygon> getPolygons() {
		findPolygons();
		return this.polygonKeyToPolygon.values();
	}

	// return the size of S and the order k
	public int getN() {
		return sites.size();
	}
	public int getK() {
		return k;
	}

	// grant access to the elements of the diagram:
	public PointSet getS() {
		return sites;
	}
	public Set<VEdge> getEdges() {
		return edges;
	}
	public PointSet getVertices() {
		return vertices;
	}

}
