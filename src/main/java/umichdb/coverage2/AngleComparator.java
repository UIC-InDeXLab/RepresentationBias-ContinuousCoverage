package umichdb.coverage2;

import java.util.Comparator;

/**
 * This class is used to sort the critical points by their position on the
 * circle around the vertex. If angles are equal, we check the absolute value.
 * 
 * @author markjin1990
 *
 */
public class AngleComparator implements Comparator<Point2D> {
	public AngleComparator(Point2D center) {
		this.center = center;
	}
	public int compare(Point2D p1, Point2D p2) {
		double dx1 = p1.getX() - center.getX(), dy1 = p1.getY() - center.getY(),
				dx2 = p2.getX() - center.getX(),
				dy2 = p2.getY() - center.getY();

		//// Original: a problem with this is that when this comparator is used
		//// to sort a set of points in a Treeset, points with the same angle
		//// for a given center will be seen as duplicates and removed.
		// return Point2D.compareAngle(dx1, dy1, dx2, dy2);

		// Added by Mark
		int angle = Point2D.compareAngle(dx1, dy1, dx2, dy2);
		if (angle != 0) {
			return angle;
		}
		if (p1.getX() != p2.getX()) {
			if (p1.getX() < p2.getX()) {
				return -1;
			} else {
				return 1;
			}
		}

		if (p1.getY() != p2.getY()) {
			if (p1.getY() < p2.getY()) {
				return -1;
			} else {
				return 1;
			}
		}

		return 0;
	}
	Point2D center;
}
