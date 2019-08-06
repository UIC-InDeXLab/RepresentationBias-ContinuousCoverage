package umichdb.coverage2;


public class NDCube {
	private double[] endPointsNDim;
	
	public NDCube(double[] endPoins) {
		endPointsNDim = endPoins;
	}
	
	public NDCube(int dim) {
		endPointsNDim = new double[2*dim];
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
		return (int) (endPointsNDim.length/2);
	}
	
	public double getMaxValAtDim(int d) {
		return endPointsNDim[2*d + 1];
	}
	
	public double getMinValAtDim(int d) {
		return endPointsNDim[2*d];
	}
	
	public void setMaxValAtDim(int d, double val) {
		endPointsNDim[2*d + 1] = val;
	}
	
	public void setMinValAtDim(int d, double val) {
		endPointsNDim[2*d] = val;
	}
	
	public boolean contains(NDPoint p) {
		int dim = p.getDimensions();
		for (int d = 0; d < dim; d++) {
			if (p.valAtNDimension(d) <= this.getMinValAtDim(d) || p.valAtNDimension(d) >= this.getMaxValAtDim(d)) {
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
}
