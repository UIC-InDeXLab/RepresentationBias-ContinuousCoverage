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
	
	public int getDimensions() {
		return coords.length;
	}
	
	public double valAtNDimension(int d) {
		return coords[d];
	}
	
	@Override
    public String toString() { 
	  StringBuffer buf = new StringBuffer();
	    buf.append("(");
	    for (int i = 0; i < coords.length; i++) {
	    		if (i < coords.length - 1) {
	    			buf.append(coords[i] + ",");
	    		}
	    		else {
	    			buf.append(coords[i]);
	    		}		    		
	    }
	    buf.append(")");
	    return buf.toString();
	}
}
