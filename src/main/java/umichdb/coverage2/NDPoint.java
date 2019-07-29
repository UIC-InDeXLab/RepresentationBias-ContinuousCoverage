package umichdb.coverage2;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

public class NDPoint {
	private double[] coords;
	public NDPoint(double[] coords){
		this.coords = coords;
    }
	
	public double euclideanDistance(NDPoint p2) {
		return eclideanDistance(this, p2);
	}
	
	public static double eclideanDistance(NDPoint p1, NDPoint p2) {
		EuclideanDistance s = new EuclideanDistance();
		return s.compute(p1.coords, p2.coords);
	}
}
