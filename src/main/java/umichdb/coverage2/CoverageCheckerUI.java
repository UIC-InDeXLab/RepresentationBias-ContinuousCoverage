package umichdb.coverage2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYPolygonAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleEdge;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;

public class CoverageCheckerUI extends ApplicationFrame {
	private static final long serialVersionUID = 6294689542092367723L;

	private final Color backgroundColor = new Color(255, 228, 196);
	private final Color siteColor = Color.BLACK;
	private final Color circleColor = Color.GRAY;
	private final Color voronoiColor = Color.GRAY;
	private final Color coveredColor = Color.GREEN;
	private final Color uncoveredColor = Color.RED;

	private final Stroke circleStroke = new BasicStroke();
	private final Stroke voronoiStroke = new BasicStroke();
	private final static float dash1[] = {10.0f};
	private final Stroke polyStroke = new BasicStroke(1.0f,
			BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);

	private double minRange = 0.0;
	private double maxRange = 1.0;

	static final int seed = 19;

	private CoverageChecker cc;

	public CoverageCheckerUI(String title, CoverageChecker cc, double delta,
			int sampleSize, boolean allowInteractive) {
		super(title);

		title = String.format(
				"Coverage for %d random points in %d-d space. (k=%d, theta=%.2f)",
				cc.sites.length, cc.sites[0].getDimensions(), cc.k, cc.theta);
		NDPoint[] points = cc.sites;
		double radius = cc.theta;
		VoronoiKOrder v = cc.vd;
		this.cc = cc;

		// Create dataset
		XYDataset pointsSet = createPointDataset(points);

		// Plot all points
		JFreeChart chart = ChartFactory.createScatterPlot(title, "x", "y",
				pointsSet, PlotOrientation.HORIZONTAL, false, false, false);

		// Change some configurations
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setOrientation(PlotOrientation.VERTICAL);
		plot.setBackgroundPaint(backgroundColor);
		XYItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0, siteColor);

		ValueAxis domainAxis = plot.getDomainAxis();
		ValueAxis rangeAxis = plot.getRangeAxis();

		domainAxis.setRange(minRange, maxRange);
		rangeAxis.setRange(minRange, maxRange);

		// Add circles
		for (NDPoint p : points) {
			plot.addAnnotation(
					new XYShapeAnnotation(
							new Ellipse2D.Double(p.getValueAt(0) - radius,
									p.getValueAt(1) - radius, radius + radius,
									radius + radius),
							circleStroke, this.circleColor));
		}

		// Add Voronoi edges
		v.getEdges().stream().forEach(e -> {
			XYLineAnnotation edge = getVoronoiEdge(e);
			plot.addAnnotation(edge);
		});

		// Add covered/uncovered points
		if (sampleSize < 0) {
			for (double x = 0; x <= 1; x += delta) {
				for (double y = 0; y <= 1; y += delta) {
					if (cc.ifCovered(x, y)) {
						plot.addAnnotation(new XYShapeAnnotation(
								new Ellipse2D.Double(x, y, delta / 5,
										delta / 5),
								circleStroke, this.coveredColor,
								this.coveredColor));
					} else {
						plot.addAnnotation(new XYShapeAnnotation(
								new Ellipse2D.Double(x, y, delta / 5,
										delta / 5),
								circleStroke, this.uncoveredColor,
								this.uncoveredColor));
					}

				}
			}
		} else {
			// Get "sampleSize" number of random uncovered points
			List<NDPoint> randUncoveredPoints = sampleUncoveredPoints(cc,
					sampleSize, cc.d);

			List<DoublePoint> randUncoveredDPs = randUncoveredPoints.stream()
					.map(p -> new DoublePoint(
							new double[]{p.getValueAt(0), p.getValueAt(1)}))
					.collect(Collectors.toList());

			// Cluster these points using DBSCAN clustering algorithm
			DBSCANClusterer s = new DBSCANClusterer<DoublePoint>(0.1, 10);
			List<Cluster> clusters = s.cluster(randUncoveredDPs);

			List<Color> clusterColors = getRandomColors(clusters.size());

			for (int i = 0; i < clusters.size(); i++) {

				// Plot all points by cluster
				Cluster<DoublePoint> c = clusters.get(i);
				for (DoublePoint point : c.getPoints()) {
					plot.addAnnotation(new XYShapeAnnotation(
							new Ellipse2D.Double(point.getPoint()[0],
									point.getPoint()[1], 0.005, 0.005),
							circleStroke, clusterColors.get(i),
							clusterColors.get(i)));
				}

				// Plot centroid of each cluster
				double x = c.getPoints().stream()
						.mapToDouble(p -> p.getPoint()[0]).average()
						.orElse(Double.NaN);
				double y = c.getPoints().stream()
						.mapToDouble(p -> p.getPoint()[1]).average()
						.orElse(Double.NaN);
				plot.addAnnotation(new XYBoxAnnotation(x - 0.01, y - 0.01, x + 0.01, y + 0.01, circleStroke,
						clusterColors.get(i), clusterColors.get(i)));

			}

		}

		// Create Panel
		ChartPanel panel = new ChartPanel(chart);

		// Add moust event listener
		if (allowInteractive) {
			panel.addChartMouseListener(new ChartMouseListener() {
				@Override
				public void chartMouseClicked(ChartMouseEvent cme) {
					report(cme);
				}

				@Override
				public void chartMouseMoved(ChartMouseEvent cme) {
					// report(cme);
				}

				private void report(ChartMouseEvent cme) {
					Rectangle2D dataArea = panel.getScreenDataArea();
					JFreeChart chart = cme.getChart();
					XYPlot plot = (XYPlot) chart.getPlot();
					ValueAxis xAxis = plot.getDomainAxis();
					double x = xAxis.java2DToValue(cme.getTrigger().getX(),
							dataArea, RectangleEdge.BOTTOM);

					ValueAxis yAxis = plot.getRangeAxis();
					double y = yAxis.java2DToValue(cme.getTrigger().getY(),
							dataArea, RectangleEdge.LEFT);
					System.out.println("Click on " + new Point2D(x, y)
							+ ". If covered: " + cc.ifCovered(x, y));
				}
			});
		}

		setContentPane(panel);
	}

	/**
	 * Get a random color
	 * 
	 * @return
	 */
	private static List<Color> getRandomColors(int num) {
		Random randomGenerator = new Random();
		List<Color> colors = new ArrayList<Color>();
		randomGenerator.setSeed(seed - 1);
		for (int i = 0; i < num; i++) {
			int red = randomGenerator.nextInt(200) + 25;
			int green = randomGenerator.nextInt(200) + 25;
			int blue = randomGenerator.nextInt(200) + 25;
			colors.add(new Color(red, green, blue));
		}

		return colors;
	}

	/**
	 * Sample uncovered points
	 * 
	 * @param cc
	 * @param sampleSize
	 * @param d
	 * @return
	 */
	private static List<NDPoint> sampleUncoveredPoints(CoverageChecker cc,
			int sampleSize, int d) {
		Random rand = new Random();
		rand.setSeed(seed);

		List<NDPoint> uncoveredPoints = new ArrayList<NDPoint>();
		while (uncoveredPoints.size() < sampleSize) {
			double[] coords = new double[d];
			for (int dim = 0; dim < d; dim++) {
				coords[dim] = rand.nextDouble();
			}

			NDPoint newPoint = new NDPoint(coords);
			if (!uncoveredPoints.contains(newPoint)
					&& !cc.ifCovered(newPoint.getValueAt(0),
							newPoint.getValueAt(1))) {
				uncoveredPoints.add(newPoint);
			}
		}
		return uncoveredPoints;
	}

	/**
	 * Draw a Voronoi edge
	 * 
	 * @param e
	 * @return
	 */
	private XYLineAnnotation getVoronoiEdge(VEdge e) {
		double i1 = e.v1.getX(), i2 = e.v1.getY(), i3 = e.v2.getX(),
				i4 = e.v2.getY();
		double x1, y1, x2, y2;

		int type = 0;

		if (e.v1.isAtInfinity() && e.v2.isAtInfinity()) {
			double dx = (i2 - i4) * 5000;
			double dy = (i3 - i1) * 5000;
			x1 = (i1 + i3) / 2 + dx;
			y1 = (i2 + i4) / 2 + dy;
			x2 = (i1 + i3) / 2 - dx;
			y2 = (i2 + i4) / 2 - dy;
			type = 3;
		} else if (e.v1.isAtInfinity()) {
			x1 = i3;
			y1 = i4;
			x2 = i3 + i1 * 5000;
			y2 = i4 + i2 * 5000;
			type = 2;
		} else if (e.v2.isAtInfinity()) {
			x1 = i1;
			y1 = i2;
			x2 = i1 + i3 * 5000;
			y2 = i2 + i4 * 5000;
			type = 1;
		} else {
			// Type 0 in VoroKOrder. Do nothing
			x1 = i1;
			y1 = i2;
			x2 = i3;
			y2 = i4;
			type = 0;
		}

		return new XYLineAnnotation(x1, y1, x2, y2, voronoiStroke,
				voronoiColor);
	}

	/**
	 * Get edge location
	 * 
	 * @param e
	 * @return
	 */
	private Point2D getEdgeLabelLocation(VEdge e) {
		double i1 = e.v1.getX(), i2 = e.v1.getY(), i3 = e.v2.getX(),
				i4 = e.v2.getY();
		double x1, y1, x2, y2;

		int type = 0;

		if (e.v1.isAtInfinity() && e.v2.isAtInfinity()) {
			double dx = (i2 - i4) * 5000;
			double dy = (i3 - i1) * 5000;
			x1 = (i1 + i3) / 2 + dx;
			y1 = (i2 + i4) / 2 + dy;
			x2 = (i1 + i3) / 2 - dx;
			y2 = (i2 + i4) / 2 - dy;
			type = 3;
		} else if (e.v1.isAtInfinity()) {
			x1 = i3;
			y1 = i4;
			x2 = i3 + i1 * 5000;
			y2 = i4 + i2 * 5000;
			type = 2;
		} else if (e.v2.isAtInfinity()) {
			x1 = i1;
			y1 = i2;
			x2 = i1 + i3 * 5000;
			y2 = i2 + i4 * 5000;
			type = 1;
		} else {
			// Type 0 in VoroKOrder. Do nothing
			x1 = i1;
			y1 = i2;
			x2 = i3;
			y2 = i4;
			type = 0;
		}

		return new Point2D((x1 + x2) / 2, (y1 + y2) / 2);
	}

	private XYDataset createPointDataset(NDPoint[] points) {
		XYSeriesCollection dataset = new XYSeriesCollection();

		// Boys (Age,weight) series
		XYSeries series1 = new XYSeries("NDPoints");
		for (NDPoint p : points) {
			series1.add(p.getValueAt(0), p.getValueAt(1));
		}
		dataset.addSeries(series1);

		return dataset;
	}
}