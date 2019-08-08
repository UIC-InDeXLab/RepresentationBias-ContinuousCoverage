package umichdb.coverage2;

import java.util.Arrays;

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
	
	@Override
    public boolean equals(Object o) { 
  
        // If the object is compared with itself then return true   
        if (o == this) { 
            return true; 
        } 
  
        /* Check if o is an instance of Complex or not 
          "null instanceof [type]" also returns false */
        if (!(o instanceof NDPoint)) { 
            return false; 
        } 
          
        // typecast o to Complex so that we can compare data members  
        NDPoint otherPoint = (NDPoint) o; 
          
        // Compare the data members and return accordingly  
        return Arrays.equals(this.coords, otherPoint.coords); 
    } 
}
