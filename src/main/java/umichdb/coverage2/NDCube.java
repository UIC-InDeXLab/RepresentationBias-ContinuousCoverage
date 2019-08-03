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
	
}
