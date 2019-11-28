package umichdb.coverage2;
// From http://www.nirarebakun.com/voro/ehivoro.html
import java.awt.Graphics;
import java.applet.Applet;
import java.awt.Color;
public class Hivoro extends java.applet.Applet {
	Color backGroundColor, fontColor;
	int numOfVertices, boundingBoxHeight, boudingBoxWidth;
	int dotRadius = 2;
	int orderShown = 1;

	double xMin, yMin, xMax, yMax, xActualMax = 0, yActualMax = 0, yy2;
	double ds, us;
	double y21, sa0, sa1;
	int br, u, k2;
//	int xz, xz2, yz, yz2;
	Color colors[] = new Color[13];
	double vironoiX[] = new double[100];
	double vironoiY[] = new double[100];
	double kz[] = new double[100];
	

	double siteX[] = new double[100];
	double siteY[] = new double[100];

	double distance[] = new double[100];
	String sss[] = new String[100];
	
	
	public double str2Double(String dous) {
		double dou1;
		dou1 = (Double.valueOf(dous)).doubleValue();
		return dou1;
	}
	public double rand() {
		double rand1;
		rand1 = Math.random();
		return rand1;
	}
	public void init() {
		backGroundColor = Color.black;
		fontColor = Color.yellow;
		colors[0] = Color.white;
		colors[1] = Color.green;
		colors[2] = new Color(199, 111, 238);

		boudingBoxWidth = 1000;
		boundingBoxHeight = 1000;
		numOfVertices = 10;
		orderShown = 1;
	}


	public double power(double a, double b) {
		double val;
		val = Math.pow(a, b);
		return val;
	}

	void heapv(double te1[], double siteX[], double siteY[], int numberOfVertices) {
		int kk, kks, ii, jj, mm;
		double b1, b2, b3, c1, c2, c3;
		kks = (int) (numberOfVertices / 2);
		for (kk = kks; kk >= 1; kk--) {
			ii = kk;
			b1 = te1[ii - 1];
			b2 = siteX[ii - 1];
			b3 = siteY[ii - 1];
			while (2 * ii <= numberOfVertices) {
				jj = 2 * ii;
				if (jj + 1 <= numberOfVertices) {
					if (te1[jj - 1] < te1[jj]) {
						jj++;
					}
				}
				if (te1[jj - 1] <= b1) {
					break;
				}
				te1[ii - 1] = te1[jj - 1];
				siteX[ii - 1] = siteX[jj - 1];
				siteY[ii - 1] = siteY[jj - 1];
				ii = jj;
			} // wend
			te1[ii - 1] = b1;
			siteX[ii - 1] = b2;
			siteY[ii - 1] = b3;
		} // next kk
		for (mm = numberOfVertices - 1; mm >= 1; mm--) {
			c1 = te1[mm];
			c2 = siteX[mm];
			c3 = siteY[mm];
			te1[mm] = te1[0];
			siteX[mm] = siteX[0];
			siteY[mm] = siteY[0];
			ii = 1;
			while (2 * ii <= mm) {
				kk = 2 * ii;
				if (kk + 1 <= mm) {
					if (te1[kk - 1] <= te1[kk]) {
						kk++;
					}
				}
				if (te1[kk - 1] <= c1) {
					break;
				}
				te1[ii - 1] = te1[kk - 1];
				siteX[ii - 1] = siteX[kk - 1];
				siteY[ii - 1] = siteY[kk - 1];
				ii = kk;
			} // wend
			te1[ii - 1] = c1;
			siteX[ii - 1] = c2;
			siteY[ii - 1] = c3;
		} // next mm
	}
	public void paint(java.awt.Graphics g) {
		g.setColor(backGroundColor);
		g.fillRect(1, 1, boudingBoxWidth, boundingBoxHeight);
		g.setColor(fontColor);
		g.drawString("N=" + numOfVertices, 15, 15);
		
		// Draw sites
		for (int k = 0; k < numOfVertices; k++) {
			siteX[k] = rand() * (boudingBoxWidth - 30) + 15;
			siteY[k] = rand() * (boundingBoxHeight - 30) + 15;
			int x = (int) (siteX[k] + 0.5);
			int y = (int) (siteY[k] + 0.5);
			distance[k] = power(siteX[k] * siteX[k] + siteY[k] * siteY[k], 0.5);
			g.fillOval(x - 2, y - 2, dotRadius * 2, dotRadius * 2);
		}
		
		// Draw Voronoi cells
		heapv(distance, siteX, siteY, numOfVertices);
		for (int i = 1; i <= numOfVertices - 1; i++) {
			for (int j = i + 1; j <= numOfVertices; j++) {
				double ijSegmentSlope = (siteY[i - 1] - siteY[j - 1]) / (siteX[i - 1] - siteX[j - 1]);
				double ijBisectorSlope = -1 / ijSegmentSlope;
				double ijBisectiorPointY = (siteY[i - 1] + siteY[j - 1]) / 2;
				double ijBisectorPointX = (siteX[i - 1] + siteX[j - 1]) / 2;
				double ijBisectorIntercept = ijBisectiorPointY - ijBisectorPointX * ijBisectorSlope;
				
				if (ijBisectorIntercept > 0 && ijBisectorIntercept < boundingBoxHeight) {
					xMin = 0;
					yMin = ijBisectorIntercept;
				} else {
					if (ijBisectorSlope > 0) {
						xMin = -ijBisectorIntercept / ijBisectorSlope;
						yMin = 0;
					} else {
						xMin = (boundingBoxHeight - ijBisectorIntercept) / ijBisectorSlope;
						yMin = boundingBoxHeight;
					}
				}
				yMax = ijBisectorSlope * boudingBoxWidth + ijBisectorIntercept;
				if (yMax > 0 && yMax < boundingBoxHeight) {
					xActualMax = boudingBoxWidth;
					yActualMax = yMax;
				} else {
					if (ijBisectorSlope > 0) {
						xActualMax = (boundingBoxHeight - ijBisectorIntercept) / ijBisectorSlope;
						yActualMax = boundingBoxHeight;
					} else {
						xActualMax = -ijBisectorIntercept / ijBisectorSlope;
						yActualMax = 0;
					}
				}
				int numOfVoronoiVertices = 1;
				vironoiX[numOfVoronoiVertices - 1] = xMin;
				vironoiY[numOfVoronoiVertices - 1] = yMin;

				// Third point
				for (int k = 1; k <= numOfVertices; k++) {
					if (k != i && k != j) {
						double ikSegmentSlope = (siteY[i - 1] - siteY[k - 1]) / (siteX[i - 1] - siteX[k - 1]);
						double ikBisectorSlope = -1 / ikSegmentSlope;
						double ikBisectorPointY = (siteY[i - 1] + siteY[k - 1]) / 2;
						double ikBisectorValueX = (siteX[i - 1] + siteX[k - 1]) / 2;
						double ikBisectorIntercept = ikBisectorPointY - ikBisectorValueX * ikBisectorSlope;

						double y20 = ikBisectorSlope * xMin + ikBisectorIntercept;
						y21 = ikBisectorSlope * xActualMax + ikBisectorIntercept;
						sa0 = yMin - y20;
						sa1 = yActualMax - y21;
						if (sa0 * sa1 < 0) {
							numOfVoronoiVertices++;
							vironoiX[numOfVoronoiVertices - 1] = (ikBisectorIntercept - ijBisectorIntercept) / (ijBisectorSlope - ikBisectorSlope);
							vironoiY[numOfVoronoiVertices - 1] = ijBisectorSlope * vironoiX[numOfVoronoiVertices - 1] + ijBisectorIntercept;
						} // if sa0*sa1<0
					} // if(k!=i && k!=j)
				} // next k
				numOfVoronoiVertices++;

				vironoiX[numOfVoronoiVertices - 1] = xActualMax;
				vironoiY[numOfVoronoiVertices - 1] = yActualMax;
				for (u = 1; u <= numOfVoronoiVertices; u++) {
					kz[u - 1] = 0;
				}
				heapv(vironoiX, vironoiY, kz, numOfVoronoiVertices);
				for (int voronoiVertexId = 1; voronoiVertexId <= numOfVoronoiVertices - 1; voronoiVertexId++) {
					k2 = voronoiVertexId + 1;
					xMax = (vironoiX[voronoiVertexId - 1] + vironoiX[k2 - 1]) / 2;
					yy2 = ijBisectorSlope * xMax + ijBisectorIntercept;
					ds = power(xMax - siteX[i - 1], 2) + power(yy2 - siteY[i - 1], 2);
					int br2 = 0;
					for (u = 1; u <= numOfVertices; u++) {
						if (u != i && u != j) {
							us = power(xMax - siteX[u - 1], 2)
									+ power(yy2 - siteY[u - 1], 2);
							if (us < ds) {
								br2 = br2 + 1;
							}
						}
					} // next u
					System.out.println(br2 + " " + numOfVoronoiVertices);
					

					if (br2 == orderShown - 1) {

						int vironoiEdgeX1 = (int) (vironoiX[voronoiVertexId - 1] + 0.5);
						int vironoiEdgeX2 = (int) (vironoiX[k2 - 1] + 0.5);
						int vironoiEdgeY1 = (int) (vironoiY[voronoiVertexId - 1] + 0.5);
						int vironoiEdgeY2 = (int) (vironoiY[k2 - 1] + 0.5);
						g.setColor(colors[br2]);
						g.drawLine(vironoiEdgeX1, vironoiEdgeY1, vironoiEdgeX2, vironoiEdgeY2);
					} // if br2<3
				} // next k
			} // next j
		} // next i
	}

}