package umichdb.coverage2;

import org.locationtech.jts.geom.Coordinate;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.*;
import org.locationtech.jts.awt.PolygonShape;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.alg.cycle.CycleDetector;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class PointLocator {
	private Geometry finalTriangle;
	private SimpleDirectedGraph<Geometry, DefaultEdge> dag;
	private HashMap<Set<Coordinate>, Geometry> triangleToPolygon;

	public PointLocator(Polygon[] faces) {
		/* Initialize local variables */
		GeometryFactory fact = new GeometryFactory();
		HashMap<Coordinate, List<Geometry>> coordinateToGeometry = new HashMap<Coordinate, List<Geometry>>();
		HashMap<Coordinate, List<Coordinate>> coordinateToNeighbors = new HashMap<Coordinate, List<Coordinate>>();

		triangleToPolygon = new HashMap<Set<Coordinate>, Geometry>();

		/* Initialize private variables */
		dag = new SimpleDirectedGraph<Geometry, DefaultEdge>(DefaultEdge.class);

		/* Create outer bounding triangle to contain all the regions */
		Coordinate p1 = new Coordinate(-1000000000, -1000000000);
		Coordinate p2 = new Coordinate(0.0, 1000000000);
		Coordinate p3 = new Coordinate(1000000000, -1000000000);

		// Define boundary triangle
		finalTriangle = fact.createPolygon(new Coordinate[] { p1, p2, p3, p1 }).convexHull();
		
		List<Geometry> triangles = new ArrayList<Geometry>();

		// Step 1: Triangulate all faces
//		System.out.println("inner triangles:");
		for (Polygon poly : faces) {
			DelaunayTriangulationBuilder triangualtionBuilder = new DelaunayTriangulationBuilder();
			triangualtionBuilder.setSites(poly);
			Geometry polyTriangles = triangualtionBuilder.getTriangles(fact);
			for (int i = 0; i < polyTriangles.getNumGeometries(); i++) {
				Geometry triangle = polyTriangles.getGeometryN(i);
				triangles.add(triangle);
//				System.out.println(triangle);
				triangleToPolygon.put(new HashSet<>(Arrays.asList(triangle.getCoordinates())), poly);
			}
		}
		

		// Step 2: Triangulate space between boundary points and the convex hull of all faces
		ArrayList<Coordinate> boundaryPoints = new ArrayList<Coordinate>();
		boundaryPoints.add(p1);
		boundaryPoints.add(p2);
		boundaryPoints.add(p3);

		LinearRing shell = fact.createLinearRing(new Coordinate[] {p1,p2,p3,p1});
		LinearRing convexHullOfFaces = fact.createLinearRing(fact.createGeometryCollection(faces).convexHull().getCoordinates());

		Polygon spaceBetweenBoundaryAndConvexHullOfFaces = new Polygon(shell, new LinearRing[]{convexHullOfFaces}, fact);
		EarClipper ec = new EarClipper(spaceBetweenBoundaryAndConvexHullOfFaces);
		Geometry outerTriangles = ec.getResult();
		
//		System.out.println("outer triangles:" );
		for (int i = 0; i < outerTriangles.getNumGeometries(); i++) {
			Geometry triangle = outerTriangles.getGeometryN(i);
//			System.out.println(triangle);
			triangles.add(triangle);
		}

		// Step 3: Create graph that we later use to find independent sets
		SimpleGraph<Coordinate, DefaultEdge> graph = new SimpleGraph<Coordinate, DefaultEdge>(DefaultEdge.class);
		for (Geometry t : triangles) {
			Coordinate[] coordinates = t.getCoordinates();
			graph.addVertex(coordinates[0]);
			graph.addVertex(coordinates[1]);
			graph.addVertex(coordinates[2]);
			graph.addEdge(coordinates[0], coordinates[1]);
			graph.addEdge(coordinates[0], coordinates[2]);
			graph.addEdge(coordinates[1], coordinates[2]);

			/* add triangles to the digraph as vertices for now */
			dag.addVertex(t);

			for (int i = 0; i < 3; i++) {
				List<Geometry> geoList;
				List<Coordinate> neighborList;

				if (coordinateToGeometry.get(coordinates[i]) != null)
					geoList = coordinateToGeometry.get(coordinates[i]);

				else
					geoList = new ArrayList<Geometry>();

				geoList.add(t);
				coordinateToGeometry.put(coordinates[i], geoList);

				if (coordinateToNeighbors.get(coordinates[i]) != null)
					neighborList = coordinateToNeighbors.get(coordinates[i]);

				else
					neighborList = new ArrayList<Coordinate>();

				for (int j = 0; j < 4; j++) {
					Coordinate c = coordinates[j];
					if (c.equals(coordinates[i]))
						continue;
					neighborList.add(c);
				}

				coordinateToNeighbors.put(coordinates[i], neighborList);
			}
		}

		// Step 4: building hierarchies of triangles from bottom up
		while (graph.vertexSet().size() > 3) {

			/*
			 * Create independent set. Use two lists to avoid modifying list we're iterating
			 * over
			 */
			ArrayList<Coordinate> independentSetCandidates = new ArrayList<Coordinate>();
			ArrayList<Coordinate> independentSet = new ArrayList<Coordinate>();

			Set<Coordinate> vertices = graph.vertexSet();
			for (Coordinate p : vertices) {
				if (graph.degreeOf(p) <= 8 && !boundaryPoints.contains(p)) {
					independentSetCandidates.add(p);
				}
			}

			for (Coordinate u : independentSetCandidates) {
				boolean add = true;
				for (Coordinate v : independentSet) {
					if (graph.containsEdge(u, v) || graph.containsEdge(v, u))
						add = false;

					if (v.equals(u))
						add = false;
				}

				if (add)
					independentSet.add(u);
			}

			/*
			 * Create new triangulation using the independentSet
			 * 
			 */
			for (Coordinate p : independentSet) {
				/* update neighbor and affected triangles list */
				DelaunayTriangulationBuilder triangualtionBuilder = new DelaunayTriangulationBuilder();
				triangualtionBuilder.setSites(graph.vertexSet());
				List<Geometry> tempTriangles = new ArrayList<Geometry>();
				Geometry g = triangualtionBuilder.getTriangles(fact);
				for (int i = 0; i < g.getNumGeometries(); i++) {
					tempTriangles.add(g.getGeometryN(i));
				}

				coordinateToNeighbors = new HashMap<Coordinate, List<Coordinate>>();
				coordinateToGeometry = new HashMap<Coordinate, List<Geometry>>();

				for (Geometry t : tempTriangles) {
					List<Geometry> geoList;
					List<Coordinate> neighborList;
					Coordinate[] coordinates = t.getCoordinates();

					for (int i = 0; i < 3; i++) {
						if (coordinateToGeometry.get(coordinates[i]) != null)
							geoList = coordinateToGeometry.get(coordinates[i]);

						else
							geoList = new ArrayList<Geometry>();

						geoList.add(t);
						coordinateToGeometry.put(coordinates[i], geoList);

						if (coordinateToNeighbors.get(coordinates[i]) != null)
							neighborList = coordinateToNeighbors.get(coordinates[i]);

						else
							neighborList = new ArrayList<Coordinate>();

						for (int j = 0; j < 4; j++) {
							Coordinate c = coordinates[j];
							if (c.equals(coordinates[i]))
								continue;
							neighborList.add(c);
						}

						coordinateToNeighbors.put(coordinates[i], neighborList);
					}
				}
				ArrayList<Coordinate> pCollection = new ArrayList<Coordinate>();
				pCollection.add(p);

				Collection<DefaultEdge> edges = graph.edgesOf(p);
				List<Coordinate> neighbors = new ArrayList<Coordinate>();
				List<Geometry> oldTriangles = new ArrayList<Geometry>();
				List<Geometry> newTriangles = new ArrayList<Geometry>();

				neighbors = coordinateToNeighbors.get(p);
				triangualtionBuilder = new DelaunayTriangulationBuilder();
				triangualtionBuilder.setSites(neighbors);
				g = triangualtionBuilder.getTriangles(fact);
				for (int i = 0; i < g.getNumGeometries(); i++) {
					newTriangles.add(g.getGeometryN(i));
				}

				oldTriangles = coordinateToGeometry.get(p);
				oldTriangles.removeAll(newTriangles);

				if (newTriangles.size() == 1)
					finalTriangle = newTriangles.get(0);

				/* Update undirected graph */
				graph.removeVertex(p);

				/* Update DAG */
				for (Geometry newT : newTriangles) {
					dag.addVertex(newT);

					for (Geometry oldT : oldTriangles) {
						dag.addVertex(oldT);
						dag.addEdge(newT, oldT);
					}
				}
			}
		}

	}

	public Geometry lookup(double x, double y) {
		return lookup(new Coordinate(x, y));
	}

	/**
	 * Find the polygon containing the point p
	 * 
	 * @param queryPointCoordinate
	 * @return
	 */
	public Geometry lookup(Coordinate queryPointCoordinate) {
		GeometryFactory fact = new GeometryFactory();
		Point queryPoint = fact.createPoint(queryPointCoordinate);

		Geometry curr = finalTriangle;

		if (!curr.contains(queryPoint)) {
			return null;
		}

		while (!dag.outgoingEdgesOf(curr).isEmpty()) {
			boolean findTriangle = false;
			for (DefaultEdge e : dag.outgoingEdgesOf(curr)) {
				Geometry t = dag.getEdgeTarget(e);
				if (t.covers(queryPoint)) { // Add intersect just in case the point is on
											// the edge of this triangle
//					System.out.println("containing triangle:" + t + " if_original:"
//							+ triangleToPolygon.containsKey(new HashSet<>(Arrays.asList(t.getCoordinates()))));

					curr = t;
					findTriangle = true;
					break;
				}
			}
			if (!findTriangle) {
				System.err.println("ERROR: no triangle found for " + queryPoint + " for " + curr);
			}
		}

		if (triangleToPolygon.containsKey(new HashSet<>(Arrays.asList(curr.getCoordinates())))) {
			return triangleToPolygon.get(new HashSet<>(Arrays.asList(curr.getCoordinates())));
		}

		return null;

	}

	public static void main(String[] args) throws ParseException {


		GeometryFactory geometryFactory = new GeometryFactory();

		Coordinate p1 = new Coordinate(0.0, 0.0);
		Coordinate p2 = new Coordinate(0.0, 1.0);
		Coordinate p3 = new Coordinate(1.0, 2.0);
		Coordinate p4 = new Coordinate(1.0, 0.0);
		
		Coordinate p5 = new Coordinate(2.0, 2.0);
		Coordinate p6 = new Coordinate(2.0, 1.0);
		
		Polygon g1 = geometryFactory.createPolygon(new Coordinate[] { p1, p2, p3, p4, p1 });
		Polygon g2 = geometryFactory.createPolygon(new Coordinate[] { p4, p3, p5, p6, p4 });
		
		PointLocator t = new PointLocator(new Polygon[] { g1, g2 });

		System.out.println("found: " + t.lookup(new Coordinate(0.5, 0.5)));

	}
}
