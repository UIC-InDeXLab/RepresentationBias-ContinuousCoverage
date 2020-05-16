package vldb;

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
import umichdb.coverage2.CoverageChecker;
import umichdb.coverage2.CoverageCheckerUI;
import umichdb.coverage2.CoverageCheckerUI.Uiconfig;
import umichdb.coverage2.Utils;

public class Fig3 {

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
		CoverageChecker cc = new CoverageChecker(randPoints, k, theta);
		double endTime = System.currentTimeMillis();

		System.out.printf("Coverage discovery time: %f ms\n",
				endTime - beginTime);
		
		Map<Uiconfig, Boolean> config = new HashMap<Uiconfig, Boolean>();
		config.put(Uiconfig.SHOWCOLOR, false);
		config.put(Uiconfig.SHOWCIRCLE, false);


		cc.view(delta, -1, config);

	}
}
