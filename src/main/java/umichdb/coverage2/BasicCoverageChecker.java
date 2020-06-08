package umichdb.coverage2;

import java.util.Arrays;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.vector.BaseVector;
import smile.feature.Scaler;

public class BasicCoverageChecker implements CoverageChecker {
	DataFrame dataset;
	int k;
	double rho;
	Scaler scaler;


	public BasicCoverageChecker(DataFrame rawDataset, int k, double rho) {
		// Rescaling
		scaler = Scaler.fit(rawDataset);
		this.dataset = scaler.transform(rawDataset);

		this.k = k;
		this.rho = rho;
	}

	/**
	 * Check if a point is covered
	 */
	@Override
	public boolean ifCovered(double[] point) {
		int closeNeighborsCount = 0;
		for (int i = 0; i < dataset.size(); i++) {
			Tuple p = dataset.get(i);
			if (Utils.getEuclideanDistance(p.toArray() , point) <= rho) {
				if (++closeNeighborsCount >= k)
					return true;
			}
		}

		return false;
	}

}
