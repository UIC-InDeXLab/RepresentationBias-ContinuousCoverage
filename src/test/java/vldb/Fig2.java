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
import umichdb.coverage2.MithraCoverageChecker;
import umichdb.coverage2.MithraCoverageCheckerUI;
import umichdb.coverage2.MithraCoverageCheckerUI.Uiconfig;
import umichdb.coverage2.Utils;

public class Fig2 {

	public static void main(String[] args) {
		int n = 100;
		int d = 2;
		double rho = 0.1;
		int k = 1;
		double delta = 0.005;

		System.out.printf("Create %d points of dimension %d\n", n, d);
		DataFrame randPoints = Utils.genRandDataset(n, d);

		System.out.println(randPoints);
		
		System.out.println("Start building coverage graph");

		double beginTime = System.currentTimeMillis();
		MithraCoverageChecker cc = new MithraCoverageChecker(randPoints, k, rho);
		double endTime = System.currentTimeMillis();

		System.out.printf("Coverage discovery time: %f ms\n",
				endTime - beginTime);
		
		Map<Uiconfig, Boolean> config = new HashMap<Uiconfig, Boolean>();
		config.put(Uiconfig.SHOWCOLOR, false);
		config.put(Uiconfig.SHOWCIRCLE, false);


		cc.view(delta, -1, config);

	}
}
