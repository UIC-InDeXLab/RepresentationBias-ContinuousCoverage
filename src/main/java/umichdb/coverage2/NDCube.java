package umichdb.coverage2;

import java.util.Arrays;

public class NDCube {
	private double[] minAtDim;
	private double[] maxAtDim;

	
	public NDCube(int dim) {
		minAtDim = new double[dim];
		maxAtDim = new double[dim];
	}
	
	public double getVolume() {
		int dim = this.getDimensionality();
		double volume = 1;
		
		for (int d = 0; d < dim; d++) {
			volume *= this.getMaxValAtDim(d) - this.getMinValAtDim(d);
		}
		return volume;
		
	}
	
	public int getDimensionality() {
		return minAtDim.length;
	}
	
	public double getMaxValAtDim(int d) {
		return maxAtDim[d];
	}
	
	public double getMinValAtDim(int d) {
		return minAtDim[d];
	}
	
	public void setMaxValAtDim(int d, double val) {
		maxAtDim[d] = val;
	}
	
	public void setMinValAtDim(int d, double val) {
		minAtDim[d] = val;
	}
	
	public boolean contains(NDPoint p) {
		int dim = p.getDimensions();
		for (int d = 0; d < dim; d++) {
			if (p.getValueAt(d) <= this.getMinValAtDim(d) || p.getValueAt(d) >= this.getMaxValAtDim(d)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
    public String toString() { 
        String s = "";
        int dim = getDimensionality();
        s += "Dimensions: " + dim + "\n";
        s += "Volumne: " + getVolume() + "\n";
        for (int d = 0; d < dim; d++) {
        		s += "\t d" + (d+1) +": [" + this.getMinValAtDim(d) + "," + this.getMaxValAtDim(d) + "]\n";
        }
        return s;
    }
	
	@Override
    public boolean equals(Object o) { 
  
        // If the object is compared with itself then return true   
        if (o == this) { 
            return true; 
        } 
  
        /* Check if o is an instance of Complex or not 
          "null instanceof [type]" also returns false */
        if (!(o instanceof NDCube)) { 
            return false; 
        } 
          
        // typecast o to Complex so that we can compare data members  
        NDCube otherCube = (NDCube) o; 
          
        // Compare the data members and return accordingly  
        return Arrays.equals(this.minAtDim, otherCube.minAtDim) && Arrays.equals(this.maxAtDim, otherCube.maxAtDim); 
    }
}
