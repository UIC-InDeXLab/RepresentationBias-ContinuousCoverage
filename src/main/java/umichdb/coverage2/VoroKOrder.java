package umichdb.coverage2;

/*
Higher Order Voronoi Diagrams - Demonstration Applet
written by Andreas Pollak 2007

This program is part of my diploma thesis in computer science at 
FernUniversität Hagen, supervised by Dr. Christian Icking, 
Abt. PI 6.

To find out more about the data structures and algoritm employed,
read my thesis ;)

change history:		
	2019-11-10 A. Pollak	
			converted form Applet to App
			allowed point drag with shift-click and point delete with ctrl-click
			substituted deprecated functions (replaces Integer and Double constructors with valueOf)
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;

//To be implemented by classes that need to be informed about
//changes by GfxInteractor
interface Updateable {
	// called by GfxInteractor upon changes
	void checkForUpdate();
}

// GfxInteractor is the layer between the class VoronoiKOrder
// and the (multi threaded) gui.
class GfxInteractor {
	public GfxInteractor() {
		updateAlways = new Vector<Updateable>();
		updateOnChange = new Vector<Updateable>();
		updateOnMouseMovement = new Vector<Updateable>();

		V = new VoronoiKOrder();

		activePoint = null;
		activeVertex = null;
		mouseIn = false;
		dragging = false;
	}

	// register class to be informed about changes
	// parameter onWhichChange:
	// 0=on structural changes only
	// 1=when the display should be redrawn (structural chanes, mouse over)
	// 2=always (including changes of the mouse position)
	public void addForUpdate(Updateable u, int onWhichChange) {
		switch (onWhichChange) {
			case 0 :
				updateOnChange.add(u);
				break;
			case 1 :
				updateAlways.add(u);
				break;
			case 2 :
				updateOnMouseMovement.add(u);
				break;
		}
	}

	// called by the gui: inform GfxInteractor about mouse movement
	public synchronized void mouseOver(int x, int y) {
		int typeOfChange = 2;

		mouseIn = true;
		if (dragging) {
			Point2D newPoint = new Point2D(activePoint.getX() + x - mouseX,
					activePoint.getY() + y - mouseY);
			V.movePoint(activePoint, newPoint);
			activePoint = newPoint;
			typeOfChange = 1;
		}
		mouseX = x;
		mouseY = y;
		if (!dragging) {
			Point2D newActivePoint = V.getS().findNeighbour(x, y, 5.0);
			if (newActivePoint != activePoint) {
				activePoint = newActivePoint;
				typeOfChange = 1;
			}
			VVertex newActiveVertex = (activePoint == null)
					? (VVertex) V.getVertices().findNeighbour(x, y, 5.0)
					: null;
			if (newActiveVertex != activeVertex) {
				activeVertex = newActiveVertex;
				typeOfChange = 1;
			}
		}

		requestUpdates(typeOfChange);
	}

	// called by the gui: inform GfxInteractor about mouse movement
	public synchronized void mouseOut() {
		int typeOfChange = 2;

		mouseIn = false;
		if (activePoint != null) {
			activePoint = null;
			dragging = false;
			typeOfChange = 1;
		} else if (activeVertex != null) {
			activeVertex = null;
			typeOfChange = 1;
		}

		requestUpdates(typeOfChange);
	}

	// called by the gui: inform GfxInteractor about mouse clicks
	public synchronized void click(int button) {
		boolean redraw = false;

		if (!mouseIn)
			return;
		switch (button) {
			case 1 :
				V.addPoint(activePoint = new Point2D(mouseX, mouseY));
				activeVertex = null;
				redraw = true;
				break;
			case 2 :
				if (activePoint != null) {
					dragging = true;
					redraw = true;
				}
				break;
			case 3 :
				if (activePoint != null) {
					V.removePoint(activePoint);
					mouseOver(mouseX, mouseY);
					redraw = true;
				}
				break;
		}

		if (redraw)
			requestUpdates(0);
	}

	// called by the gui: inform GfxInteractor about mouse clicks
	public synchronized void unClick(int button) {
		if (button == 2) {
			dragging = false;
			requestUpdates(1);
		}
	}

	// called by the gui: clear button
	public synchronized void clear() {
		V.clear();
		requestUpdates(0);
	}

	// called by the gui: switch between "lock k" and "lock n-k"
	public synchronized void setMaintainedOrder(boolean nMinusK) {
		V.setMaintainedOrder(nMinusK);
	}

	// called by the gui: set diagram order
	public synchronized void setOrder(int k) {
		V.changeOrder(k);
		requestUpdates(0);
	}

	// read the number of points and the order of the diagram
	public synchronized int getN() {
		return V.getN();
	}
	public synchronized int getK() {
		return V.getK();
	}

	// the following functions are used to get a list of the graphical objects
	// required to draw the diagram; after a call to startGfxObj(),
	// nextGfxObj() returns the objects to be drawn (lines, circles, etc.).
	// After returning the las object, nextGfxObj() returns null.
	// IMPORTANT: the calling method must make sure that the diagram is
	// not modified between consecutive calls of startGfxObj() and nextGfxObj()!
	public synchronized void startGfxObj() {
		gfxObjStage = 0;
	}
	public synchronized GfxObj nextGfxObj() {
		switch (gfxObjStage) {
			case 0 :
				gfxObjPI = V.getS().iterator();
				gfxObjStage = 1;
			case 1 : // start with the points...
				if (gfxObjPI.hasNext()) {
					Point2D p = gfxObjPI.next();
					Color c;
					if (p == activePoint)
						c = dragging ? new Color(255, 128, 128) : Color.RED;
					else if (activeVertex != null) {
						if (activeVertex.isCritical(p))
							c = Color.GREEN;
						else if (activeVertex.isRelevant(p))
							c = new Color(0, 128, 0);
						else
							c = Color.BLACK;
					} else
						c = Color.BLACK;
					return new GfxObj(4, c, (int) p.getX() - 3,
							(int) p.getY() - 3, 7, 7);
				}
				gfxObjEI = V.getEdges().iterator();
				gfxObjStage = 2;
			case 2 : // then the edges...
				if (gfxObjEI.hasNext()) {
					VEdge e = gfxObjEI.next();
					if (e.v1.getP().isAtInfinity()
							&& e.v2.getP().isAtInfinity())
						return new GfxObj(2,
								e.isCritical(activePoint)
										? Color.BLUE
										: Color.BLACK,
								(int) e.critical1.getX(),
								(int) e.critical1.getY(),
								(int) e.critical2.getX(),
								(int) e.critical2.getY());
					if (e.v1.getP().isAtInfinity())
						return new GfxObj(1,
								e.isCritical(activePoint)
										? Color.BLUE
										: Color.BLACK,
								(int) e.v2.getP().getX(),
								(int) e.v2.getP().getY(),
								(int) e.v1.getP().getX(),
								(int) e.v1.getP().getY());
					if (e.v2.getP().isAtInfinity())
						return new GfxObj(1,
								e.isCritical(activePoint)
										? Color.BLUE
										: Color.BLACK,
								(int) e.v1.getP().getX(),
								(int) e.v1.getP().getY(),
								(int) e.v2.getP().getX(),
								(int) e.v2.getP().getY());
					return new GfxObj(0,
							e.isCritical(activePoint)
									? Color.BLUE
									: Color.BLACK,
							(int) e.v1.getP().getX(), (int) e.v1.getP().getY(),
							(int) e.v2.getP().getX(), (int) e.v2.getP().getY());
				}
				gfxObjStage = 3;
			case 3 : // and finally the circle around the current vertex
				gfxObjStage = 4;
				if (activeVertex != null) {
					double r = Math.sqrt((activeVertex.getX()
							- activeVertex.getCritical().first().getX())
							* (activeVertex.getX()
									- activeVertex.getCritical().first().getX())
							+ (activeVertex.getY()
									- activeVertex.getCritical().first().getY())
									* (activeVertex.getY() - activeVertex
											.getCritical().first().getY()));
					return new GfxObj(3, Color.GRAY,
							(int) (activeVertex.getX() - r),
							(int) (activeVertex.getY() - r), (int) (2 * r),
							(int) (2 * r));
				}
			default :
				return null;
		}
	}

	// returns information about the diagram and the mouse status
	public synchronized String getInfoText(int type) {
		switch (type) {
			case 0 :
				if (!mouseIn)
					return "[move mouse over drawing area]";
				String pos = "mouse=(" + mouseX + "," + mouseY + ")";
				if (activePoint != null)
					pos += " over point@(" + activePoint.getX() + ","
							+ activePoint.getY() + ")";
				if (activeVertex != null)
					pos += " over vertex@("
							+ Math.round(activeVertex.getX() * 100) / 100.0
							+ ","
							+ Math.round(activeVertex.getY() * 100) / 100.0
							+ ")";
				return pos;
			case 1 :
				if (V.getS().size() == 0)
					return "n=0";
				return "n=" + V.getS().size() + "; " + V.getEdges().size()
						+ " edges, " + V.getVertices().size() + " vertices";
		}
		return "";
	}

	// invoke the checkForUpdate() of registered Updateable classes
	private void requestUpdates(int typeOfChange) {
		for (int i = 0; i < updateOnMouseMovement.size(); i++)
			updateOnMouseMovement.get(i).checkForUpdate();
		if (typeOfChange <= 1)
			for (int i = 0; i < updateAlways.size(); i++)
				updateAlways.get(i).checkForUpdate();
		if (typeOfChange == 0)
			for (int i = 0; i < updateOnChange.size(); i++)
				updateOnChange.get(i).checkForUpdate();
	}

	// regisered for updates:
	Vector<Updateable> updateAlways, updateOnChange, updateOnMouseMovement;

	// gfxObj iteration
	int gfxObjStage;
	Iterator<Point2D> gfxObjPI;
	Iterator<VEdge> gfxObjEI;

	// what the mouse is doing...
	Point2D activePoint;
	VVertex activeVertex;
	int mouseX, mouseY;
	boolean mouseIn, dragging;

	// most importantly: the Voronoi diagram
	VoronoiKOrder V;
}

// this is a VERY simple representation of a graphical object
// of a shape given by type and a color c:
// variable type:
// 0=a line from (i1,i2) to (i3,i4)
// 1=a ray from (i1,i2) in the direction (i3,i4)
// 2=the bisector of (i1,i2) and (i3,i4)
// 3=an oval bounded by the rectangle (i1,i2,i3,i4)
// 3=a filled oval bounded by the rectangle (i1,i2,i3,i4)
class GfxObj {
	public GfxObj(int type, Color c, int i1, int i2, int i3, int i4) {
		this.type = type;
		this.c = c;
		this.i1 = i1;
		this.i2 = i2;
		this.i3 = i3;
		this.i4 = i4;
	}

	public int type;
	public Color c;
	public int i1, i2, i3, i4;
}

// this class represents the drawing area of the applet; it draws the
// diagram and reports mouse events
class GfxPanel extends JPanel
		implements
			MouseMotionListener,
			MouseListener,
			Updateable {
	// all events will be reported to gi, the graphics are obtained from gi
	public GfxPanel(GfxInteractor gi) {
		super();
		setBackground(Color.WHITE);
		setBorder(new LineBorder(Color.BLACK));
		addMouseListener(this);
		addMouseMotionListener(this);
		this.gi = gi;
	}

	// overrides the standard method of JPanel do display the diagram
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// use this to adjust the rendering quality to your needs
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		// It is VERY IMPORTANT that consecutive accesses to gi.startGfxObj()
		// and gi.nextGfxObj() be synchronized. If the diagram changes during
		// GfxObj iteration, unpredictable behaviour will result.
		synchronized (gi) {
			gi.startGfxObj();
			GfxObj o = gi.nextGfxObj();
			while (o != null) {
				g.setColor(o.c);
				switch (o.type) {
					case 0 :
						g.drawLine(o.i1, o.i2, o.i3, o.i4);
						break;
					case 1 :
						// simple, but effective
						// CAUTION: this won't work if (i1,i2) is very far away
						// from the drwaing area
						g.drawLine(o.i1, o.i2, o.i1 + o.i3 * 5000,
								o.i2 + o.i4 * 5000);
						break;
					case 2 :
					// simple, but effective
					{
						int dx = (o.i2 - o.i4) * 5000,
								dy = (o.i3 - o.i1) * 5000;
						g.drawLine((o.i1 + o.i3) / 2 + dx,
								(o.i2 + o.i4) / 2 + dy, (o.i1 + o.i3) / 2 - dx,
								(o.i2 + o.i4) / 2 - dy);
					}
						break;
					case 3 :
						g.drawOval(o.i1, o.i2, o.i3, o.i4);
						break;
					case 4 :
						g.fillOval(o.i1, o.i2, o.i3, o.i4);
						break;
				}
				o = gi.nextGfxObj();
			}
		}
	}

	// MouseMotionListener methods: report events to gi
	public void mouseDragged(MouseEvent e) {
		gi.mouseOver(e.getX(), e.getY());
	}
	public void mouseMoved(MouseEvent e) {
		gi.mouseOver(e.getX(), e.getY());
	}
	// MouseListener methods: report events to gi
	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
		gi.mouseOut();
	}

	int mouseButtonModified = 0;
	public void mousePressed(MouseEvent e) {
		mouseButtonModified = 0;
		int b = e.getButton();
		if (b == MouseEvent.BUTTON1) {
			if (e.isShiftDown())
				mouseButtonModified = 2;
			else if (e.isControlDown())
				mouseButtonModified = 3;
			else
				mouseButtonModified = 1;
			gi.click(mouseButtonModified);
		} else if (b == MouseEvent.BUTTON2)
			gi.click(2);
		else if (b == MouseEvent.BUTTON3)
			gi.click(3);
	}
	public void mouseReleased(MouseEvent e) {
		int b = e.getButton();
		if (b == MouseEvent.BUTTON1)
			gi.unClick(mouseButtonModified);
		else if (b == MouseEvent.BUTTON2)
			gi.unClick(2);
		else if (b == MouseEvent.BUTTON3)
			gi.unClick(3);
	}

	// Updateable method: is invoked when the diagram must be redrawn
	public void checkForUpdate() {
		repaint();
	}

	// all events will be reported to gi, the graphics are obtained from gi
	GfxInteractor gi;
}

public class VoroKOrder extends JFrame
		implements
			Updateable,
			ActionListener,
			ChangeListener,
			DocumentListener {
	// not much to do here: window management performed in main()
	public VoroKOrder() {
	}

	public static void main(String args[]) {
		VoroKOrder voro = new VoroKOrder();
		voro.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		voro.setSize(800, 600);
		voro.setTitle(
				"Higher Order Voronoi Diagrams - Demonstration Program - (C) 2007-2019 Andreas Pollak - www.pollak.org");
		voro.createGUI();
		voro.setVisible(true);
	}

	// returns GridBagConstraints that describe the layout of an swing component
	private GridBagConstraints gbc(int wx, int wy, boolean isLast,
			boolean fillx, boolean filly) {
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = wx;
		c.weighty = wy;
		if (isLast)
			c.gridwidth = GridBagConstraints.REMAINDER;
		if (fillx && filly)
			c.fill = GridBagConstraints.BOTH;
		else if (fillx)
			c.fill = GridBagConstraints.HORIZONTAL;
		else if (filly)
			c.fill = GridBagConstraints.VERTICAL;
		c.insets = new Insets(4, 4, 4, 4);
		return c;
	}

	// set up the gui:
	private void createGUI() {
		gi = new GfxInteractor();

		GridBagLayout gridbag = new GridBagLayout();
		setLayout(gridbag);

		p1 = new GfxPanel(gi);
		gridbag.setConstraints(p1, gbc(1, 1, true, true, true));
		add(p1);

		info = new InfoLabel("[move mouse over drawing area]", gi, 0);
		gridbag.setConstraints(info, gbc(1, 0, true, true, false));
		add(info);

		stats = new InfoLabel("n=0", gi, 1);
		gridbag.setConstraints(stats, gbc(1, 0, true, true, false));
		add(stats);

		l1 = new JLabel("Order k=");
		gridbag.setConstraints(l1, gbc(0, 0, false, false, false));
		add(l1);

		t1 = new JTextField("1", 4);
		gridbag.setConstraints(t1, gbc(0, 0, false, false, false));
		add(t1);
		t1.getDocument().addDocumentListener(this);

		s1 = new JSlider(1, 1);
		s1.setMajorTickSpacing(1);
		gridbag.setConstraints(s1, gbc(1, 0, true, true, false));
		add(s1);
		s1.setMajorTickSpacing(0);
		s1.setMinorTickSpacing(1);
		s1.setPaintTicks(true);
		s1.setSnapToTicks(true);
		s1.setPaintLabels(true);
		s1.setLabelTable(getDictionary());
		s1.addChangeListener(this);

		l2 = new JLabel("Lock:");
		gridbag.setConstraints(l2, gbc(0, 0, false, false, false));
		add(l2);

		bg = new ButtonGroup();

		r1 = new JRadioButton("k", true);
		gridbag.setConstraints(r1, gbc(0, 0, false, false, false));
		add(r1);
		r1.setActionCommand("k");
		bg.add(r1);
		r1.addActionListener(this);

		r2 = new JRadioButton("n-k", false);
		gridbag.setConstraints(r2, gbc(0, 0, false, false, false));
		add(r2);
		r2.setActionCommand("n-k");
		bg.add(r2);
		r2.addActionListener(this);

		empty = new JLabel("");
		gridbag.setConstraints(empty, gbc(0, 0, true, true, false));
		add(empty);

		b1 = new JButton("clear");
		gridbag.setConstraints(b1, gbc(0, 0, true, false, false));
		add(b1);
		b1.setActionCommand("clear");
		b1.addActionListener(this);

		gi.addForUpdate(this, 0);
		gi.addForUpdate(p1, 1);
		gi.addForUpdate(info, 2);
		gi.addForUpdate(stats, 1);

		n = 0;
		k = 1;
	}

	// this function creates the labels for the slider (in a
	// moderately clever way); the idea: put mark the numbers
	// 10^i where i=floor(logn)
	private Dictionary<Integer, JComponent> getDictionary() {
		Dictionary<Integer, JComponent> d = new Hashtable<Integer, JComponent>();
		d.put(Integer.valueOf(1), new JLabel("1"));
		if (n > 2) {
			int dst = (int) Math.round(Math.exp(Math.log(10.0)
					* Math.floor(Math.log(n - 1) / Math.log(10.0))));
			int pos = dst;
			while (pos < n - 1) {
				if ((pos > 1) && ((n - 1 - pos) * 20 >= n - 1))
					d.put(Integer.valueOf(pos), new JLabel(pos + ""));
				pos += dst;
			}
			d.put(Integer.valueOf(n - 1), new JLabel((n - 1) + ""));
		}
		return d;
	}

	// Updateable method: check if n or k have changed, update the gui
	// accordingly
	public void checkForUpdate() {
		if (n != gi.getN()) {
			n = gi.getN();
			s1.setMaximum(Math.max(1, n - 1));
			s1.setLabelTable(getDictionary());
		}
		if (k != gi.getK()) {
			k = gi.getK();
			s1.setValue(k);
		}
		if (!handsOffTextBox)
			t1.setText(k + "");
	}

	// ActionListener method: checks if the "clear" button or the "k"/"n-k"
	// radio
	// buttons are selected
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("clear"))
			gi.clear();
		else if (e.getActionCommand().equals("k"))
			gi.setMaintainedOrder(false);
		else if (e.getActionCommand().equals("n-k"))
			gi.setMaintainedOrder(true);
	}

	// ChangeListener method: keeps sight of the slider
	public void stateChanged(ChangeEvent e) {
		if ((e.getSource() == s1) && !s1.getValueIsAdjusting()
				&& (s1.getValue() != k))
			gi.setOrder(s1.getValue());
	}

	// DocumentListener methods: recognizes a textual input of the order k
	public void changedUpdate(DocumentEvent e) {
		docChange();
	}
	public void insertUpdate(DocumentEvent e) {
		docChange();
	}
	public void removeUpdate(DocumentEvent e) {
		docChange();
	}
	private void docChange() {
		try {
			int newK = Integer.parseInt(t1.getText());
			// handsOffTextBox==true makes sure that the content of the textbox
			// is not changed during
			// an update caused by an input in this very textbox
			if ((newK >= 1) && (newK < n) && (newK != k)) {
				handsOffTextBox = true;
				gi.setOrder(newK);
				handsOffTextBox = false;
			}
		} catch (Exception ex) {
		}
	}

	// the various swing components:
	JButton b1;
	JRadioButton r1, r2;
	JLabel l1, l2, empty;
	InfoLabel info, stats;
	JSlider s1;
	GfxPanel p1;
	JTextField t1;
	ButtonGroup bg;

	// gi is the interface to the diagram
	GfxInteractor gi;

	// the current state of affairs, as far as the gui is concerned
	int n, k;
	boolean handsOffTextBox = false;
}

// InfoLabel is a simple text label that responds to update requests from
// a GfxInteractor gi.
class InfoLabel extends JLabel implements Updateable {
	public InfoLabel(String text, GfxInteractor gi, int infoType) {
		super(text);
		this.infoType = infoType;
		this.gi = gi;
	}

	// Updateable method: obtain new text and display it
	public void checkForUpdate() {
		setText(gi.getInfoText(infoType));
	}

	int infoType;
	GfxInteractor gi;
}
