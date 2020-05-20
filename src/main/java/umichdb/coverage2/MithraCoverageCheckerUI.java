package umichdb.coverage2;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

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
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleEdge;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.vector.BaseVector;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;

public class MithraCoverageCheckerUI extends ApplicationFrame {
	private static final long serialVersionUID = 1L;

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

	private final int tickFontSize = 20;
	private final int labelFontSize = 25;
	private final int titleFontSize = 30;

	static final int seed = 19;

	private MithraCoverageChecker cc;

	public enum Uiconfig {
		SHOWVORONOI, SHOWCIRCLE, SHOWCOLOR, INTERACTIVE, SHOWTITLE,
	}
	// public static final

	public MithraCoverageCheckerUI(String title, MithraCoverageChecker cc, double delta,
			int sampleSize, Map<Uiconfig, Boolean> viewConfig) {
		super(title);

		DataFrame points = cc.dataset;
		double radius = cc.rho;
		VoronoiKOrder v = cc.coverageVoronoiDiagram;
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

		Font titleFont = new Font("Dialog", Font.PLAIN, titleFontSize);
		chart.setTitle(new TextTitle(title, titleFont));

		Font plotFont = new Font("Dialog", Font.PLAIN, labelFontSize);
		domainAxis.setLabelFont(plotFont);
		rangeAxis.setLabelFont(plotFont);

		Font tickFont = new Font("Dialog", Font.PLAIN, tickFontSize);
		domainAxis.setTickLabelFont(tickFont);
		rangeAxis.setTickLabelFont(tickFont);

		// CategoryPlot plot = chart.getCategoryPlot();
		//
		// plot.getDomainAxis().setLabelFont(plotFont);
		// plot.getRangeAxis().setLabelFont(plotFont);

		// Add circles
		if (viewConfig.getOrDefault(Uiconfig.SHOWCIRCLE, true)) {
			for (int i = 0; i < points.size(); i++) {
				Tuple r = points.get(i);
				plot.addAnnotation(new XYShapeAnnotation(
						new Ellipse2D.Double(r.getDouble(0) - radius,
								r.getDouble(1) - radius, radius + radius,
								radius + radius),
						circleStroke, this.circleColor));
			}
		}

		// Add Voronoi edges
		if (viewConfig.getOrDefault(Uiconfig.SHOWVORONOI, true)
				&& cc.coverageVoronoiDiagram != null) {
			v.getEdges().stream().forEach(e -> {
				XYLineAnnotation edge = getVoronoiEdge(e);
				plot.addAnnotation(edge);
			});
		}

		// Add covered/uncovered points
		if (viewConfig.getOrDefault(Uiconfig.SHOWCOLOR, true)) {
			if (sampleSize < 0) {
				for (double x = 0; x <= 1; x += delta) {
					for (double y = 0; y <= 1; y += delta) {
						if (cc.ifCovered(new double[]{x, y}, false)) {
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
				DataFrame randUncoveredPoints = sampleUncoveredPoints(cc,
						sampleSize, cc.d);

				List<DoublePoint> randUncoveredDPs = randUncoveredPoints
						.stream()
						.map(p -> new DoublePoint(
								new double[]{p.getDouble(0), p.getDouble(1)}))
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
					plot.addAnnotation(new XYBoxAnnotation(x - 0.01, y - 0.01,
							x + 0.01, y + 0.01, circleStroke,
							clusterColors.get(i), clusterColors.get(i)));

				}

			}
		}

		// Create Panel
		ChartPanel panel = new ChartPanel(chart);

		// Add moust event listener
		if (viewConfig.getOrDefault(Uiconfig.INTERACTIVE, true)) {
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
					System.out.println(
							"Click on " + new Point2D(x, y) + ". If covered: "
									+ cc.ifCovered(new double[]{x, y}, false));
				}
			});
		}

		setContentPane(panel);

		if (cc.coverageDecisionTree != null) {
			try {
				MutableGraph g = new Parser()
						.read(cc.coverageDecisionTree.dot());
				DisplayDecisionTree(
						Graphviz.fromGraph(g).render(Format.PNG).toImage());
			} catch (Exception e) {

			}
		}
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
	private static DataFrame sampleUncoveredPoints(MithraCoverageChecker cc,
			int sampleSize, int d) {
		Random rand = new Random();
		rand.setSeed(seed);

		double[][] vectors = new double[sampleSize][d];

		int count = 0;
		while (count < sampleSize) {
			double[] vector = new double[d];
			for (int dim = 0; dim < d; dim++) {
				vector[dim] = rand.nextDouble();
			}

			if (!cc.ifCovered(vector, false)) {
				vectors[count][0] = vector[0];
				vectors[count][1] = vector[1];
				count++;
			}
		}

		DataFrame uncoveredPoints = DataFrame.of(vectors);

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

	private static JFrame frame;
	private static JLabel label;
	public static void DisplayDecisionTree(BufferedImage image) {
		if (frame == null) {
			frame = new JFrame();
			frame.setTitle("stained_image");
			frame.setSize(image.getWidth(), image.getHeight());
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			label = new JLabel();
			label.setIcon(new ImageIcon(image));
			frame.getContentPane().add(label, BorderLayout.CENTER);
			frame.setLocationRelativeTo(null);
			frame.pack();
			frame.setVisible(true);
		} else
			label.setIcon(new ImageIcon(image));
	}

	private XYDataset createPointDataset(DataFrame points) {
		XYSeriesCollection dataset = new XYSeriesCollection();

		// Boys (Age,weight) series
		XYSeries series1 = new XYSeries("NDPoints");
		for (int i = 0; i < points.size(); i++) {
			Tuple r = points.get(i);
			series1.add(r.getDouble(0), r.getDouble(1));
		}
		dataset.addSeries(series1);

		return dataset;
	}
}