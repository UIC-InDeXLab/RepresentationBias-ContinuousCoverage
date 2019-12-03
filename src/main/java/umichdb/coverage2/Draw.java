package umichdb.coverage2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

public class Draw extends ApplicationFrame {
	private static final long serialVersionUID = 6294689542092367723L;

	private final Color backgroundColor = new Color(255, 228, 196);
	private final Color siteColor = Color.BLACK;
	private final Color circleColor = Color.RED;
	private final Color voronoiColor = Color.BLUE;

	private final Stroke circleStroke = new BasicStroke();
	private final Stroke voronoiStroke = new BasicStroke();

	public Draw(String title, NDPoint[] points, double radius,
			VoronoiKOrder v) {
		super(title);

		// Create dataset
		XYDataset pointsSet = createPointDataset(points);

		// Plot all points
		JFreeChart chart = ChartFactory.createScatterPlot("coverage", "x", "y",
				pointsSet, PlotOrientation.HORIZONTAL, false, false, false);

		// Change some configurations
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(backgroundColor);
		XYItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0, siteColor);

		ValueAxis domainAxis = plot.getDomainAxis();
		ValueAxis rangeAxis = plot.getRangeAxis();

		domainAxis.setRange(0.0, 1.0);
		rangeAxis.setRange(0.0, 1.0);

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
			plot.addAnnotation(drawVoronoiEdge(e));
		});

		// Create Panel
		ChartPanel panel = new ChartPanel(chart);
		setContentPane(panel);
	}

	/**
	 * Draw a Voronoi edge
	 * @param e
	 * @return
	 */
	private XYAnnotation drawVoronoiEdge(VEdge e) {
		double i1 = e.v1.getX(), i2 = e.v1.getY(), i3 = e.v2.getX(),
				i4 = e.v2.getY();
		double x1, y1, x2, y2;

		if (e.v1.isAtInfinity() && e.v2.isAtInfinity()) {
			double dx = (i2 - i4) * 5000;
			double dy = (i3 - i1) * 5000;
			x1 = (i1 + i3) / 2 + dx;
			y1 = (i2 + i4) / 2 + dy;
			x2 = (i1 + i3) / 2 - dx;
			y2 = (i2 + i4) / 2 - dy;
		} else if (e.v1.isAtInfinity()) {
			x2 = i1;
			y2 = i2;
			x1 = i1 + i3 * 5000;
			y1 = i2 + i4 * 5000;
		} else if (e.v2.isAtInfinity()) {
			x1 = i1;
			y1 = i2;
			x2 = i1 + i3 * 5000;
			y2 = i2 + i4 * 5000;
		} else {
			// Type 0 in VoroKOrder. Do nothing
			x1 = i1;
			y1 = i2;
			x2 = i3;
			y2 = i4;
		}
		return new XYLineAnnotation(x1, y1, x2, y2, voronoiStroke,
				voronoiColor);
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