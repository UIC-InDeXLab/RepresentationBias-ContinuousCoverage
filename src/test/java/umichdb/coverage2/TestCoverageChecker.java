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
import umichdb.coverage2.MithraCoverageCheckerUI.Uiconfig;

public class TestCoverageChecker {

	public static void main(String[] args) {
		int n = 100;
		int d = 2;
		double theta = 0.1;
		int k = 2;
		double delta = 0.005;

		System.out.printf("Create %d points of dimension %d\n", n, d);
		DataFrame randPoints = Utils.genRandDataset(n, d);

		System.out.println(randPoints);
		
		System.out.println("Start building coverage graph");

		double beginTime = System.currentTimeMillis();
		MithraCoverageChecker cc = new MithraCoverageChecker(randPoints, k, theta);
		double endTime = System.currentTimeMillis();

		System.out.printf("Coverage discovery time: %f ms\n",
				endTime - beginTime);
		
		Map<Uiconfig, Boolean> viewConfig = new HashMap<Uiconfig, Boolean>();
		viewConfig.put(Uiconfig.SHOWVORONOI, false);

		cc.view(delta, -1, viewConfig);

	}
}
