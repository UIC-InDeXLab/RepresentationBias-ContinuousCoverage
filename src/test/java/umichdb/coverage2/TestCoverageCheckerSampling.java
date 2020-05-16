package umichdb.coverage2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jfree.ui.RefineryUtilities;

import smile.data.DataFrame;
import smile.data.vector.BaseVector;
import umichdb.coverage2.CoverageCheckerUI.Uiconfig;

public class TestCoverageCheckerSampling {

	public static void main(String[] args) {
		int n = 100;
		int d = 2;
		int samples = 100;
		double theta = 0.05;
		int k = 2;
		double delta = 0.005;

		System.out.printf("STATUS: Create %d points of dimension %d\n", n, d);
		DataFrame randPoints = Utils.genRandDataset(n, d);

		System.out.println(randPoints);
		
		System.out.println("STATUS: Start building coverage graph");

		double beginTime = System.currentTimeMillis();
		CoverageChecker cc = new CoverageChecker(randPoints, k, theta,samples);
		double endTime = System.currentTimeMillis();

		System.out.printf("\tCoverage discovery time: %f ms\n",
				endTime - beginTime);
		
		Map<Uiconfig, Boolean> viewConfig = new HashMap<Uiconfig, Boolean>();
		viewConfig.put(Uiconfig.SHOWVORONOI, false);
		cc.view(delta, 1000, viewConfig);

	}
}
