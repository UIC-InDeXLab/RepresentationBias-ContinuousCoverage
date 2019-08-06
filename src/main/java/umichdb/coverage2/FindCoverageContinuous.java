package umichdb.coverage2;

import java.util.Random; 
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.math3.util.*;

public class FindCoverageContinuous 
{
	public static NDCube findMaxVolumeNDCubeNaive(NDPoint[] points) {
		NDCube NDCubeMaxVolume = null;
		
		if (points == null || points.length < 1) {
			return null;
		}
		
		int d = points[0].getDimensions();
		
		// not enough points to formulate a least one hypercube
		if (points.length < 2*d) {
			return null;
		}
		
		// Enumerate all possible hypercubes. Note that each n-dimensional cube (hyper cube is uniquely defined by 2n points.
		Iterator<int[]> it = CombinatoricsUtils.combinationsIterator(points.length, 2*d);
		
		while (it.hasNext()) {
			
			// 2d points
			int[] indexes = it.next();
			NDPoint[] tempPoints = new NDPoint[indexes.length];
			for (int i = 0; i < indexes.length; i++) {
				tempPoints[i] = points[indexes[i]];				
			}
			
			// hypercube defined by 2d points
			NDCube newCube = createNDCubeByNDPoints(tempPoints);
			if (NDCubeMaxVolume == null || newCube.getVolume() > NDCubeMaxVolume.getVolume()) {
				NDCubeMaxVolume = newCube;
			}
		}
		
		// Return the one with the largest volume
		return NDCubeMaxVolume;
	}
	
//	public static NDCube findMaxVolumeNDCube(NDPoint[] points) {
//		
//	}
	
	private static NDCube createNDCubeByNDPoints(NDPoint[] points) {
		if (points == null || points.length < 1) {
			return null;
		}
		
		int d = points[0].getDimensions();
		
		// Dimensionality is wrong return null. The number of points != 2^d.
		if (points.length != (int) 2*d) {
			return null;
		}
		
		double[] newNDCubeEndPoints = new double[2*d];
		
		for (int dim = 0; dim < d; dim++) {
			double maxValAtDim = Double.MIN_VALUE;
			double minValAtDim = Double.MAX_VALUE;
			
			for (NDPoint p : points) {
				double valAtDim = p.valAtNDimension(dim);
				if (valAtDim > maxValAtDim) {
					maxValAtDim = valAtDim;
				}
				else if (valAtDim < minValAtDim) {
					minValAtDim = valAtDim;
				}
				newNDCubeEndPoints[2*dim] = minValAtDim;
				newNDCubeEndPoints[2*dim + 1] = maxValAtDim;
			}
		}
		return new NDCube(newNDCubeEndPoints);
		
	}
	
	/**
	 * Randomly generate n d-dimensional points
	 * @param n
	 * @param d
	 * @return
	 */
	public static NDPoint[] genRandNDPoint(int n, int d) {
		Random rand = new Random(); 
		
		NDPoint[] randPoints = new NDPoint[n];
		Set<NDPoint> existingNDpoints = new HashSet<NDPoint>();
		
		for (int i = 0; i < n; i++) {
			double[] coords = new double[d];
			for (int dim = 0; dim < d; dim++) {
				coords[dim] = rand.nextDouble();
			}
			
			NDPoint newPoint = new NDPoint(coords);
			if (!existingNDpoints.contains(newPoint)) {
				randPoints[i] = newPoint;
				existingNDpoints.add(newPoint);
			}
		}
		
		return randPoints;
	}
	
    public static void main( String[] args )
    {
    		int n = 20;
    		int d = 5;
        System.out.printf( "Create %d cubes of dimension %d \n", n, d );
        NDPoint[] randPoints = genRandNDPoint(n, d);
        
        System.out.println( "Start finding coverage cube using the naive approach");
        double beginTime = System.currentTimeMillis();
        NDCube maxCube = findMaxVolumeNDCubeNaive(randPoints);
        double endTime = System.currentTimeMillis();
        
        System.out.println( "Cube found!");
        System.out.println(maxCube);
        System.out.printf( "Search time: %f ms", endTime - beginTime);
    }
}
