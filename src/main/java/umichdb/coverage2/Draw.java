package umichdb.coverage2;

import java.awt.Color;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class Draw  extends JFrame {
	  private static final long serialVersionUID = 6294689542092367723L;

	  public Draw(String title, NDPoint[] points, NDCube cube) {
	    super(title);

	    // Create dataset
	    XYDataset pointsSet = createPointDataset(points);

	    // Create chart
	    JFreeChart chart = ChartFactory.createScatterPlot("coverage", "x", "y", pointsSet, PlotOrientation.HORIZONTAL, false, false, false);
	    
	    //Changes background color
	    XYPlot plot = (XYPlot)chart.getPlot();
	    plot.setBackgroundPaint(new Color(255,228,196));
	    plot.addAnnotation(new XYBoxAnnotation( cube.getMinValAtDim(0), cube.getMinValAtDim(1), cube.getMaxValAtDim(0), cube.getMaxValAtDim(1)));
	      
	    // Create Panel
	    ChartPanel panel = new ChartPanel(chart);
	    setContentPane(panel);
	  }

	  private XYDataset createPointDataset(NDPoint[] points) {
	    XYSeriesCollection dataset = new XYSeriesCollection();

	    //Boys (Age,weight) series
	    XYSeries series1 = new XYSeries("NDPoints");
	    for (NDPoint p : points) {
	    		series1.add(p.valAtNDimension(0), p.valAtNDimension(1));
	    }
	    dataset.addSeries(series1);

	    return dataset;
	  }
}
