package umichdb.coverage2;


public class NDCube {
	private double[] endPointsNDim;
	private double volume;
	
	public NDCube(double[] endPoins) {
		endPointsNDim = endPoins;
		volume = getVolume();
	}
	
	public double getVolume() {
		double volume = 1;
		for (int i = 0; i < endPointsNDim.length/2; i++) {
			volume *= endPointsNDim[i+1] - endPointsNDim[i];
		}
		return volume;
		
	}
	
	public int getDimensionality() {
		return (int) (endPointsNDim.length/2);
	}
	
	@Override
    public String toString() { 
        String s = "";
        int d = getDimensionality();
        s += "Dimensions: " + d + "\n";
        s += "Volumne: " + getVolume() + "\n";
        for (int dim = 0; dim < d; dim++) {
        		s += "\t d" + (dim+1) +": [" + endPointsNDim[2*dim] + "," + endPointsNDim[2*dim + 1] + "]\n";
        }
        return s;
    }
}
