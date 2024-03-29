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
//import org.locationtech.geowave.analytic.GeometryHullTool;
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
	private SimpleDirectedGraph<Geometry, DefaultEdge> triangleHierarchy;
	private HashMap<Set<Coordinate>, Geometry> triangleToPolygon;

	public PointLocator(Polygon[] faces) {

		
		
		/* Initialize local variables */
		GeometryFactory fact = new GeometryFactory();
		
		HashMap<Coordinate, List<Geometry>> coordinateToRegions = new HashMap<Coordinate, List<Geometry>>();

		triangleToPolygon = new HashMap<Set<Coordinate>, Geometry>();

		/* Initialize private variables */
		triangleHierarchy = new SimpleDirectedGraph<Geometry, DefaultEdge>(DefaultEdge.class);

		/* Create outer bounding triangle to contain all the regions */
		Coordinate p1 = new Coordinate(-1000000000, -1000000000);
		Coordinate p2 = new Coordinate(0.0, 1000000000);
		Coordinate p3 = new Coordinate(1000000000, -1000000000);
		finalTriangle = fact.createPolygon(new Coordinate[] { p1, p2, p3, p1 }).convexHull();
		
		
		
		List<Geometry> triangles = new ArrayList<Geometry>();

		// Step 1: Triangulate all faces
		for (Polygon poly : faces) {
			DelaunayTriangulationBuilder triangualtionBuilder = new DelaunayTriangulationBuilder();
			triangualtionBuilder.setSites(poly);
			Geometry polyTriangles = triangualtionBuilder.getTriangles(fact);
			for (int i = 0; i < polyTriangles.getNumGeometries(); i++) {
				Geometry innerTriangle = polyTriangles.getGeometryN(i);
				triangles.add(innerTriangle);
				triangleToPolygon.put(new HashSet<>(Arrays.asList(innerTriangle.getCoordinates())), poly);
			}
		}
		

		// Step 2: Triangulate space between boundary triangle and the convex hull of all faces
		ArrayList<Coordinate> boundaryPoints = new ArrayList<Coordinate>();
		boundaryPoints.add(p1);
		boundaryPoints.add(p2);
		boundaryPoints.add(p3);

		LinearRing shell = fact.createLinearRing(new Coordinate[] {p1,p2,p3,p1});
		LinearRing convexHullOfFaces = fact.createLinearRing(fact.createGeometryCollection(faces).convexHull().getCoordinates());

		Polygon spaceBetweenBoundaryAndConvexHullOfFaces = new Polygon(shell, new LinearRing[]{convexHullOfFaces}, fact);
		EarClipper ec = new EarClipper(spaceBetweenBoundaryAndConvexHullOfFaces);
		Geometry outerTriangles = ec.getResult();
		
		for (int i = 0; i < outerTriangles.getNumGeometries(); i++) {
			Geometry outerTriangle = outerTriangles.getGeometryN(i);
			triangles.add(outerTriangle);
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
			triangleHierarchy.addVertex(t);

			for (int i = 0; i < 3; i++) {
				List<Geometry> affectedRegions;

				affectedRegions = coordinateToRegions.getOrDefault(coordinates[i], new ArrayList<Geometry>());
				affectedRegions.add(t);
				coordinateToRegions.put(coordinates[i], affectedRegions);
			}
		}

		// Step 4: building hierarchies of triangles from bottom up
		do {

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
			
			System.out.println("independentSet:"+independentSet);

			/*
			 * Create new triangulation using the independentSet to formulate a new layer in the hierarchy
			 * 
			 */
			for (Coordinate p : independentSet) {
				List<Coordinate> neighbors = new ArrayList<Coordinate>();
				List<Geometry> oldTriangles = new ArrayList<Geometry>();
				List<Geometry> newTriangles = new ArrayList<Geometry>();
				
				

				
				Polygon newHole = findBoundingPolygon(p, coordinateToRegions.get(p));
				
				System.out.println("remove=" + p);
				System.out.println("newHole=" + newHole);
				
				EarClipper ec2 = new EarClipper(newHole);
				Geometry trianglesOfNewHole = ec2.getResult();
				
				// Triangulate the resulting hole
				for (int i = 0; i < trianglesOfNewHole.getNumGeometries(); i++) {
					newTriangles.add(trianglesOfNewHole.getGeometryN(i));
				}
				
				System.out.println("new trangles " + trianglesOfNewHole);

				oldTriangles = new ArrayList<Geometry>(coordinateToRegions.get(p));
				oldTriangles.removeAll(newTriangles);

				if (newTriangles.size() == 1) {
					finalTriangle = newTriangles.get(0);
					System.out.println("update final triangle: " + newTriangles.get(0));
				}

				/* Update undirected graph */
				graph.removeVertex(p);
				System.out.println("remaining=" + graph.vertexSet());


				/* Update DAG */
				for (Geometry newT : newTriangles) {
					triangleHierarchy.addVertex(newT);

					for (Geometry oldT : oldTriangles) {
						triangleHierarchy.addVertex(oldT);
						triangleHierarchy.addEdge(newT, oldT);
					}
				}
				
				
				
				/* update neighbor and affected triangles list */
				for (Geometry oldTriangle : oldTriangles) {
					for (Coordinate c : oldTriangle.getCoordinates()) {
//						System.out.println(c + " " + coordinateToGeometry.get(c));
						if (coordinateToRegions.containsKey(c))
						coordinateToRegions.get(c).remove(oldTriangle);
					}
				}
				
				for (Geometry newTriangle : newTriangles) {
					for (Coordinate c : newTriangle.getCoordinates()) {
						List<Geometry> temp = coordinateToRegions.getOrDefault(c, new ArrayList<Geometry>()); 
						temp.add(newTriangle);
					}
				}
				
				
				coordinateToRegions.remove(p);
				
//				System.out.println("coordinateToNeighbors:" + coordinateToNeighbors + "\n");
			}
			
		} while (graph.vertexSet().size() > 3);

	}
	
	
	/**
	 * Find a bounding polygon if point p is removed from affected regions
	 * @param p
	 * @param affectedRegions
	 * @return
	 */
	public Polygon findBoundingPolygon(Coordinate p, List<Geometry> affectedRegions) {
		// TODO: write a function that finds a bounding polygon
		GeometryFactory fact = new GeometryFactory();		
		Geometry regions = fact.buildGeometry(affectedRegions);
		// TODO:
		bound <- a concave hull of regions
		LinearRing shell = fact.createLinearRing(coordsToLinearRingCoords(bound.getCoordinates()));

		return new Polygon(shell, null, fact);
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
		
		System.out.println("final trangle");
		System.out.println(finalTriangle);

		if (!curr.contains(queryPoint)) {
			return null;
		}

		while (!triangleHierarchy.outgoingEdgesOf(curr).isEmpty()) {
			boolean findTriangle = false;
			for (DefaultEdge e : triangleHierarchy.outgoingEdgesOf(curr)) {
				Geometry t = triangleHierarchy.getEdgeTarget(e);
				if (t.covers(queryPoint)) { // Add intersect just in case the point is on
											// the edge of this triangle
					System.out.println("containing triangle:" + t + " if_original:"
							+ triangleToPolygon.containsKey(new HashSet<>(Arrays.asList(t.getCoordinates()))));

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
		
		System.out.println(queryPointCoordinate + " not found");

		return null;

	}
	
	private Coordinate[] coordsToLinearRingCoords(Coordinate[] origCoords) {
		Coordinate[] linearRingCoords = new Coordinate[origCoords.length];
		System.arraycopy(origCoords, 0, linearRingCoords, 0, origCoords.length);
		linearRingCoords[linearRingCoords.length - 1] = origCoords[0];
		return linearRingCoords;
	}

	public static void main(String[] args) throws ParseException {


		GeometryFactory geometryFactory = new GeometryFactory();

//		Coordinate p1 = new Coordinate(0.0, 0.0);
//		Coordinate p2 = new Coordinate(0.0, 1.0);
//		Coordinate p3 = new Coordinate(1.0, 2.0);
//		Coordinate p4 = new Coordinate(1.0, 0.0);
//		
//		Coordinate p5 = new Coordinate(2.0, 2.0);
//		Coordinate p6 = new Coordinate(2.0, 0.0);
		
		Coordinate p1 = new Coordinate(0.0, 0.0);
		Coordinate p2 = new Coordinate(0.5, 1.0);
		Coordinate p3 = new Coordinate(1.5, 1.0);
		Coordinate p4 = new Coordinate(1.0, 0.0);
		
		Polygon g1 = geometryFactory.createPolygon(new Coordinate[] { p1, p2, p3, p4, p1 });
//		Polygon g2 = geometryFactory.createPolygon(new Coordinate[] { p4, p3, p5, p6, p4 });
		
		PointLocator t = new PointLocator(new Polygon[] { g1});

		System.out.println("found: " + t.lookup(new Coordinate(0.4, 0.5)));

	}
}
