package umichdb.coverage2;

import java.util.Random; 
import java.util.Set;
import java.util.HashSet;

public class FindCoverageContinuous 
{
	public static NDCube findMaxVolumeNDCubeNaive(NDPoint[] vertices) {
		// Enumerate all possible hypercubes
		
		// Find the one with the largest volume
		
	}
	
	public static NDCube findMaxVolumeNDCube(NDPoint[] vertices) {
		
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
        System.out.println( "Hello World!" );
    }
}
