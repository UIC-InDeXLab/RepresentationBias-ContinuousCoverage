package umichdb.coverage2;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jfree.ui.RefineryUtilities;

public class TestCoverageChecker {
	
	
	
	
	
	
	public static void main(String[] args) {
		int n = 100;
		int d = 2;
		double theta = 0.1;
		int k = 2;
		double delta = 0.005;
		
		System.out.printf("Create %d points of dimension %d\n", n, d);
		NDPoint[] randPoints = Utils.genRandNDPoint(n, d);
		for (int i = 0; i <  Math.min(randPoints.length, 10); i++) {
			System.out.println("\t" + randPoints[i]);
		}
		System.out.println("\t...");

		System.out.println(
				"Start building coverage graph");

		double beginTime = System.currentTimeMillis();
		CoverageChecker cc = new CoverageChecker(randPoints, k, theta);
		double endTime = System.currentTimeMillis();
		

		System.out.printf("Coverage discovery time: %f ms\n", endTime - beginTime);
				
		CoverageChecker.View(cc, delta, 1000, true);

	}
}
