package random;

import umichdb.coverage2.Polygon2D;

public class TestPolygon {
	public static void main(String[] args) {
		Polygon2D p = new Polygon2D();
		p.addPoint(0, 0);


		p.addPoint(0, 1);
		p.addPoint(1, 1);
		p.addPoint(1, 0);

//		p.addPoint(1, 1);
//		p.addPoint(0,0);

		
		System.out.println(p.npoints);
		System.out.println(p.contains(0.6,0.5));
	}
}
