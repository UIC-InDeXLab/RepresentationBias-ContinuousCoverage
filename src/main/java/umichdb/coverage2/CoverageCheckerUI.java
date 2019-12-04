package umichdb.coverage2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
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

public class CoverageCheckerUI extends ApplicationFrame {
	private static final long serialVersionUID = 6294689542092367723L;

	private final Color backgroundColor = new Color(255, 228, 196);
	private final Color siteColor = Color.BLACK;
	private final Color circleColor = Color.RED;
	private final Color voronoiColor = Color.BLUE;
	private final Color polyColor = Color.GREEN;

	private final Stroke circleStroke = new BasicStroke();
	private final Stroke voronoiStroke = new BasicStroke();
	private final static float dash1[] = {10.0f};
	private final Stroke polyStroke = new BasicStroke(1.0f,
			BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);

	private double minRange = 0.0;
	private double maxRange = 1.0;
	
	private CoverageChecker cc;

	public CoverageCheckerUI(String title, CoverageChecker cc) {
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

//		// Add site labels
//		for (NDPoint p : points) {
//			plot.addAnnotation(
//					new XYShapeAnnotation(
//							new Ellipse2D.Double(p.valAtNDimension(0) - radius,
//									p.valAtNDimension(1) - radius,
//									radius + radius, radius + radius),
//							circleStroke, this.circleColor));
//
//			Point2D pp = new Point2D(p.valAtNDimension(0),
//					p.valAtNDimension(1));
//			XYTextAnnotation label = new XYTextAnnotation(pp.toString(),
//					p.valAtNDimension(0), p.valAtNDimension(1) + 0.01);
//			plot.addAnnotation(label);
//		}

		// Add circles
		for (NDPoint p : points) {
			plot.addAnnotation(
					new XYShapeAnnotation(
							new Ellipse2D.Double(p.valAtNDimension(0) - radius,
									p.valAtNDimension(1) - radius,
									radius + radius, radius + radius),
							circleStroke, this.circleColor));
		}

		// Add Voronoi edges
		v.getEdges().stream().forEach(e -> {
			XYLineAnnotation edge = getVoronoiEdge(e);
			Point2D labelLoc = getEdgeLabelLocation(e);
			plot.addAnnotation(edge);
		});

//		// Label Voronoi polygons
//		v.getPolygons().forEach(poly -> {
//			double xMean = poly.regionKey.stream().mapToDouble(p -> p.getX())
//					.average().orElse(0);
//			double yMean = poly.regionKey.stream().mapToDouble(p -> p.getY())
//					.average().orElse(0);
//
//			XYTextAnnotation label = new XYTextAnnotation(
//					"Order-" + v.k + " vCell: " + poly.regionKey.toString(),
//					xMean, yMean);
//			plot.addAnnotation(label);
//		});

		// Create Panel
		ChartPanel panel = new ChartPanel(chart);

		// Add moust event listener
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
			        double x = xAxis.java2DToValue(cme.getTrigger().getX(), dataArea, 
			                RectangleEdge.BOTTOM);
			        
			        ValueAxis yAxis = plot.getRangeAxis();
			        double y = yAxis.java2DToValue(cme.getTrigger().getY(), dataArea, 
			                RectangleEdge.LEFT );
			        System.out.println("Click on "+ new Point2D(x,y) + ". If covered: " +cc.ifCovered(x, y));
			}
		});

		setContentPane(panel);
	}

	/**
	 * Draw polygon
	 * 
	 * @param vp
	 * @return
	 */
	private XYPolygonAnnotation getVoronoiPolygon(VoronoiPolygon vp) {
		double[] polyVertex = new double[vp.npoints * 2];
		for (int i = 0; i < vp.npoints; i++) {
			polyVertex[i * 2] = vp.xpoints[i];
			polyVertex[i * 2 + 1] = vp.ypoints[i];
		}
		return new XYPolygonAnnotation(polyVertex, polyStroke, polyColor);
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
			series1.add(p.valAtNDimension(0), p.valAtNDimension(1));
		}
		dataset.addSeries(series1);

		return dataset;
	}
}