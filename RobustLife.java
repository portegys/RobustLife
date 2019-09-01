/**
 * Robust Game of Life
 *
 * A Game of Life variation in which noise that causes cells to misread
 * neighboring cell states is corrected using consistency rules and
 * previous cell states.
 *
 * The error-correction scheme is based on a plausible mechanism for a
 * networked node configuration involving propagating state information.
 * Cells in the grid transmit their states to each other via signals sent
 * at the "speed of light" (one cell per step), limited by a maximum range.
 * Noise probabilistically causes signals to be misread by destination cells
 * in proportion to the distance traveled. A cell uses this "light cone"
 * information to construct a set of state histories for its locale. The
 * most recent history, that of the previous step, is the conventional 3x3
 * Life neighborhood. The cone can then be used for error-correction.
 * A neighbor's state is compared against its neighbors' states during the
 * previous step. If they agree according to Life rules, then the neighbor
 * is considered to be valid. If they disagree, then an error must have
 * occurred either in the neighbor or its neighbors. The checking then
 * continues back in time. Once a cell's neighborhood is corrected, limited
 * by the number of available histories, the next state can be produced.
 */

package robustlife;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;
import java.net.*;

// RobustLife.
public class RobustLife extends Canvas
{
    // Automaton.
    Dimension size;
    boolean[][][] cells;
    int range;
    int synchCount;
	private Integer lock;

	// Noise probability.
	public static final double DEFAULT_NOISE_QUANTUM = 0.0;
	private double noise = DEFAULT_NOISE_QUANTUM;
	private Random random;

	// Internal distance: a cell's "distance" from itself.
	// This is used to make a cell's reading of its own state noisy.
	public static final double DEFAULT_INTERNAL_DISTANCE = 0.0;
	private double internalDistance = DEFAULT_INTERNAL_DISTANCE;

    // Buffered display.
    private Dimension displaySize;
    private Graphics graphics;
    private Image image;
    private Graphics imageGraphics;

    // Message and font.
    private String message;
    private Font font;
    private FontMetrics fontMetrics;
    private int fontAscent;
    private int fontWidth;
    private int fontHeight;

    // Constructor.
    public RobustLife(Dimension size, Dimension displaySize, int range)
    {
		// Create automaton.
		this.size = size;
		this.range = range;
		synchCount = range - 1;
		cells = new boolean[range][size.width][size.height];

		// Initialize mutex.
		lock = new Integer(0);

		// Configure display.
		this.displaySize = displaySize;
		setBounds(0, 0, displaySize.width, displaySize.height);
		addMouseListener(new CanvasMouseListener());
		addMouseMotionListener(new CanvasMouseMotionListener());

		// Random numbers.
		random = new Random(new Date().getTime());
    }

    // Set random number seed.
    public void setRandomSeed(long randomSeed)
    {
		random = new Random(randomSeed);
	}

	// Set noise quantum.
	void setNoise(double noise)
	{
		this.noise = noise;
	}

	// Set internal distance.
	void setInternalDistance(double internalDistance)
	{
		this.internalDistance = internalDistance;
	}

	// Load automaton.
	public void load(String fileName, URL baseURL) throws IOException
	{
		synchronized(lock)
		{
			URL u;
			BufferedReader in;
			String s;
			StringTokenizer t;
			int w,h,x,y;

			try
			{
				try { u = new URL(fileName); }
				catch(MalformedURLException e) {
					u = new URL(baseURL, fileName);
				}
				in = new BufferedReader(new InputStreamReader(u.openStream()));
			} catch(Exception e) {
				throw new IOException("Cannot open input file " + fileName + ":" + e.getMessage());
			}

			// Load the visibility range.
			if ((s = in.readLine()) == null)
			{
				throw(new IOException("Unexpected EOF on file " + fileName));
			}
			try
			{
				range = Integer.parseInt(s, 10);
				if (range <= 0)
				{
					throw(new IOException("Invalid visibility range value " + s + " in file " + fileName));
				}
			} catch(NumberFormatException e) {
				throw(new IOException("Invalid visibility range value " + s + " in file " + fileName));
			}
			synchCount = range - 1;

			// Load the noise quantum.
			if ((s = in.readLine()) == null)
			{
				throw(new IOException("Unexpected EOF on file " + fileName));
			}
			try
			{
				noise = Double.parseDouble(s);
				if (noise < 0.0 || noise > 1.0)
				{
					throw(new IOException("Invalid noise quantum value " + s + " in file " + fileName));
				}
			} catch(NumberFormatException e) {
				throw(new IOException("Invalid noise quantum value " + s + " in file " + fileName));
			}

			// Load the automaton dimensions.
			if ((s = in.readLine()) == null)
			{
				throw(new IOException("Unexpected EOF on file " + fileName));
			}
			t = new StringTokenizer(s, " ");
			if (!t.hasMoreTokens())
			{
				throw(new IOException("Invalid dimensions in file " + fileName));
			}
			try
			{
				s = t.nextToken().trim();
				w = Integer.parseInt(s, 10);
				if (w <= 0)
				{
					throw(new IOException("Invalid width value " + s + " in file " + fileName));
				}
			} catch(NumberFormatException e) {
				throw(new IOException("Invalid width value " + s + " in file " + fileName));
			}
			if (!t.hasMoreTokens())
			{
				throw(new IOException("Invalid dimensions in file " + fileName));
			}
			try
			{
				s = t.nextToken().trim();
				h = Integer.parseInt(s, 10);
				if (h <= 0)
				{
					throw(new IOException("Invalid height value " + s + " in file " + fileName));
				}
			} catch(NumberFormatException e) {
				throw(new IOException("Invalid height value " + s + " in file " + fileName));
			}
			size.width = w;
			size.height = h;
			cells = new boolean[range][size.width][size.height];

			// Load the cell values.
			while ((s = in.readLine()) != null)
			{
				t = new StringTokenizer(s, " ");
				if (!t.hasMoreTokens()) continue;
				try
				{
					s = t.nextToken().trim();
					x = Integer.parseInt(s, 10);
					if (x < 0 || x >= w)
					{
						throw(new IOException("Invalid x value " + s + " in file " + fileName));
					}
				} catch(NumberFormatException e) {
					throw(new IOException("Invalid x value " + s + " in file " + fileName));
				}
				if (!t.hasMoreTokens())
				{
					throw(new IOException("Invalid cell values in file " + fileName));
				}
				try
				{
					s = t.nextToken().trim();
					y = Integer.parseInt(s, 10);
					if (y < 0 || y >= h)
					{
						throw(new IOException("Invalid y value " + s + " in file " + fileName));
					}
				} catch(NumberFormatException e) {
					throw(new IOException("Invalid height y " + s + " in file " + fileName));
				}
				cells[0][x][y] = true;
			}
			in.close();
		}
	}

	// Save automaton.
	public void save(String fileName) throws IOException
	{
		synchronized(lock)
		{
			int x,y;
			PrintWriter out;

			try
			{
				out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
				out.println(range + "");
				out.println(noise + "");
				out.println(size.width + " " + size.height);
				for (x = 0; x < size.width; x++)
				{
					for (y = 0; y < size.height; y++)
					{
						if (cells[0][x][y])
						{
							out.println(x + " " + y);
						}
					}
				}
				out.close();
			} catch(Exception e) {
				throw new IOException("Cannot open output file " + fileName + ":" + e.getMessage());
			}
		}
	}

	// Step automaton.
	public void step()
	{
		int r,x,y;
		Cone[][] cones;
		boolean[][] workCells = new boolean[size.width][size.height];

		synchronized(lock)
		{
			// Error-correct cells using "light cones".
			cones = new Cone[size.width][size.height];
			for (x = 0; x < size.width; x++)
			{
				for (y = 0; y < size.height; y++)
				{
					// Create light cone around cell.
					cones[x][y] = new Cone(x, y);

					if (synchCount == 0 && noise > 0.0)
					{
						// Introduce noise.
						cones[x][y].addNoise();

						// Error-correct.
						cones[x][y].correctNoise();
					}
				}
			}

			// Apply game of life rules.
			workCells = new boolean[size.width][size.height];
			for (x = 0; x < size.width; x++)
			{
				for (y = 0; y < size.height; y++)
				{
					workCells[x][y] = cones[x][y].step();
				}
			}

			// Shift cells cronologically.
			for (r = range - 2; r >= 0; r--)
			{
				for (x = 0; x < size.width; x++)
				{
					for (y = 0; y < size.height; y++)
					{
						cells[r + 1][x][y] = cells[r][x][y];
					}
				}
			}
			for (x = 0; x < size.width; x++)
			{
				for (y = 0; y < size.height; y++)
				{
					cells[0][x][y] = workCells[x][y];
				}
			}

			if (synchCount > 0) synchCount--;
		}
	}

	public void step(int cycles)
	{
		for (int i = 0; i < cycles; i++)
		{
			step();
		}
	}

	// "Light cone" history of noisy cell states.
	// The center cell state is known with certainty.
	// Cells further from center are proportionately subject to noisy readings.
	class Cone
	{
		private int layer;
		private int dimension;
		private boolean[][] states;
		private double[][] weights;	// Confidence weights.
		private boolean[][] marks;
		private Cone nextLayer = null;

		// Construct cone.
		Cone(int cx, int cy)
		{
			init(cx, cy, 0);
		}

		private Cone(int cx, int cy, int layer)
		{
			init(cx, cy, layer);
		}

		// Initialize cone.
		private void init(int cx, int cy, int layer)
		{
			int x,y,x2,y2,d2,i;

			this.layer = layer;
			dimension = (2 * (layer + 1)) + 1;
			states = new boolean[dimension][dimension];
			weights = new double[dimension][dimension];
			marks = new boolean[dimension][dimension];
			d2 = dimension / 2;
			for (x = 0; x < dimension; x++)
			{
				x2 = cx - d2 + x;
				while (x2 < 0) x2 += size.width;
				while (x2 >= size.width) x2 -= size.width;
				for (y = 0; y < dimension; y++)
				{
					y2 = cy - d2 + y;
					while (y2 < 0) y2 += size.height;
					while (y2 >= size.height) y2 -= size.height;
					states[x][y] = cells[layer][x2][y2];

					// State confidence weight is inversely proportional
					// to distance from center plus internal distance.
					i = 0;
					if (x < d2) i += d2 - x; else i += x - d2;
					if (y < d2) i += d2 - y; else i += y - d2;
					weights[x][y] = 1.0 - (noise * ((double)i + internalDistance));
					if (weights[x][y] < 0.0) weights[x][y] = 0.0;
				}
			}

			// Create the next layer.
			if (layer < range - 1 && noise > 0.0)
			{
				nextLayer = new Cone(cx, cy, layer + 1);
			}
		}

		// Add noise.
		void addNoise()
		{
			int x,y;

			for (x = 0; x < dimension; x++)
			{
				for (y = 0; y < dimension; y++)
				{
					if (random.nextDouble() >= weights[x][y])
					{
						states[x][y] = !states[x][y];
					}
				}
			}

			if (nextLayer != null)
			{
				nextLayer.addNoise();
			}
		}

		// Correct cells in cone.
		void correctNoise()
		{
			int x,y;

			// Clear marks.
			clearMarks();

			// Correct.
			for (x = 0; x < 3; x++)
			{
				for (y = 0; y < 3; y++)
				{
					if (x != 1 || y != 1)
					{
						correctCell(x, y);
					}
				}
			}
		}

		// Clear marks.
		private void clearMarks()
		{
			int x,y;

			for (x = 0; x < dimension; x++)
			{
				for (y = 0; y < dimension; y++)
				{
					marks[x][y] = false;
				}
			}

			if (nextLayer != null) nextLayer.clearMarks();
		}

		// Correct a cell.
		private void correctCell(int cx, int cy)
		{
			int x,y,x2,y2,count;
			double weight;
			boolean state;

			if (marks[cx][cy]) return;
			marks[cx][cy] = true;
			if (layer == range - 1 || noise == 0.0) return;

			// Correct cells in next layer.
			for (x = 0; x < 3; x++)
			{
				x2 = cx + x;
				for (y = 0; y < 3; y++)
				{
					y2 = cy + y;
					nextLayer.correctCell(x2, y2);
				}
			}

			// Compare current state with previous neighborhood.
			count = 0;
			weight = nextLayer.weights[cx + 1][cy + 1];
			for (x = 0; x < 3; x++)
			{
				x2 = cx + x;
				for (y = 0; y < 3; y++)
				{
					y2 = cy + y;
					if (x == 1 && y == 1) continue;
					if (nextLayer.states[x2][y2]) count++;
					weight *= nextLayer.weights[x2][y2];
				}
			}
			if (nextLayer.states[cx + 1][cy + 1])
			{
				if (count > 3 || count < 2)
				{
					state = false;
				} else {
					state = true;
				}
			} else {
				if (count == 3)
				{
					state = true;
				} else {
					state = false;
				}
			}

			// State mismatch?
			if (state != states[cx][cy])
			{
				// Correct state if outweighed by history.
				if (weight > weights[cx][cy])
				{
					states[cx][cy] = state;
					weights[cx][cy] = weight;
				}
			} else {

				// States agree: accumulate confidence weight.
				weights[cx][cy] = 1.0 - ((1.0 - weight) * (1.0 - weights[cx][cy]));
			}
		}

		// Step to next state.
		boolean step()
		{
			int x,y;
			int count = 0;
			for (x = 0; x < 3; x++)
			{
				for (y = 0; y < 3; y++)
				{
					if (x == 1 && y == 1) continue;
					if (states[x][y]) count++;
				}
			}
			if (states[1][1])
			{
				if (count > 3 || count < 2)
				{
					return false;
				} else {
					return true;
				}
			} else {
				if (count == 3)
				{
					return true;
				} else {
					return false;
				}
			}
		}

		// Dump cone.
		void dump()
		{
			System.out.println("Cone dump:");
			dumpSub();
		}

		private void dumpSub()
		{
			int x,y;
			for (x = 0; x < dimension; x++)
			{
				for (y = 0; y < dimension; y++)
				{
					if (states[x][y]) System.out.print("1"); else System.out.print("0");
				}
				System.out.println();
			}
			System.out.println();
			if (nextLayer != null) nextLayer.dumpSub();
		}
	};

	// Clear cells.
	public void clear()
	{
		int r,x,y;

		for (r = 0; r < range; r++)
		{
			for (x = 0; x < size.width; x++)
			{
				for (y = 0; y < size.height; y++)
				{
					cells[r][x][y] = false;
				}
			}
		}
	}

	// Last cell visited by mouse.
	private int lastX = -1, lastY = -1;

    // Canvas mouse listener.
    class CanvasMouseListener extends MouseAdapter
    {
        // Mouse pressed.
        public void mousePressed(MouseEvent evt)
        {
            int x, y;
			double cellWidth = (double)displaySize.width / (double)size.width;
			double cellHeight = (double)displaySize.height / (double)size.height;
            x = (int)((double)evt.getX() / cellWidth);
            y = size.height - (int)((double)evt.getY() / cellHeight) - 1;
            if (x >= 0 && x < size.width && y >= 0 && y < size.height)
            {
				lastX = x;
				lastY = y;
				synchronized(lock)
				{
					if (cells[0][x][y])
					{
						cells[0][x][y] = false;
					} else {
						cells[0][x][y] = true;
					}
					synchCount = range - 1;
				}
			}
		}
	}

    // Canvas mouse motion listener.
    class CanvasMouseMotionListener extends MouseMotionAdapter
    {
        // Mouse dragged.
        public void mouseDragged(MouseEvent evt)
        {
            int x, y;
			double cellWidth = (double)displaySize.width / (double)size.width;
			double cellHeight = (double)displaySize.height / (double)size.height;
            x = (int)((double)evt.getX() / cellWidth);
            y = size.height - (int)((double)evt.getY() / cellHeight) - 1;
            if (x >= 0 && x < size.width && y >= 0 && y < size.height)
            {
				if (x != lastX || y != lastY)
				{
					lastX = x;
					lastY = y;
					synchronized(lock)
					{
						if (cells[0][x][y])
						{
							cells[0][x][y] = false;
						} else {
							cells[0][x][y] = true;
						}
						synchCount = range - 1;
					}
				}
			}
		}
	}

    // Display automaton.
    void display()
	{
		int x,y,x2,y2;
		double cellWidth,cellHeight;

		if (graphics == null)
		{
			graphics = getGraphics();
			image = createImage(displaySize.width, displaySize.height);
			imageGraphics = image.getGraphics();
			font = new Font("Helvetica", Font.BOLD, 12);
			fontMetrics = graphics.getFontMetrics();
			fontAscent = fontMetrics.getMaxAscent();
			fontWidth = fontMetrics.getMaxAdvance();
			fontHeight = fontMetrics.getHeight();
			graphics.setFont(font);
		}
		if (graphics == null) return;

		// Clear display.
		imageGraphics.setColor(Color.white);
		imageGraphics.fillRect(0,0,displaySize.width,displaySize.height);

		// Draw grid.
		synchronized(lock)
		{
			cellWidth = (double)displaySize.width / (double)size.width;
			cellHeight = (double)displaySize.height / (double)size.height;
			imageGraphics.setColor(Color.black);
			y2 = displaySize.height;
			for (x = 1, x2 = (int)cellWidth; x < size.width;
				x++, x2 = (int)(cellWidth * (double)x))
			{
				imageGraphics.drawLine(x2, 0, x2, y2);
			}
			x2 = displaySize.width;
			for (y = 1, y2 = (int)cellHeight; y < size.height;
				y++, y2 = (int)(cellHeight * (double)y))
			{
				imageGraphics.drawLine(0, y2, x2, y2);
			}

			// Draw cells.
			for (x = x2 = 0; x < size.width;
			   x++, x2 = (int)(cellWidth * (double)x))
			{
				for (y = 0, y2 = displaySize.height - (int)cellHeight;
					y < size.height;
					y++, y2 = (int)(cellHeight * (double)(size.height - (y + 1))))
				{
					if (cells[0][x][y])
					{
						imageGraphics.fillRect(x2, y2, (int)cellWidth + 1, (int)cellHeight + 1);
					}
				}
			}

			// Draw message.
			drawMessage();
		}

		// Refresh display.
		graphics.drawImage(image, 0, 0, this);
	}

	// Set message.
	public void setMessage(String s)
	{
		message = s;
	}

	// Draw message.
	private void drawMessage()
	{
		if (message != null && !message.equals(""))
		{
 			imageGraphics.setFont(font);
			imageGraphics.setColor(Color.black);
			imageGraphics.drawString(message,
				(displaySize.width - fontMetrics.stringWidth(message)) / 2,
				displaySize.height / 2);
		}
	}

	// Dump a "light cone" history for a given cell.
	public void dumpCell(int cx, int cy)
	{
		System.out.println("Dump for cell " + cx + "," + cy + ":");
		dumpCellSub(0, cx, cy);
	}

	private void dumpCellSub(int level, int cx, int cy)
	{
		int x,y,x2,y2;
		if (level >= range) return;
		for (x = cx - level; x <= cx + level; x++) {
			for (y = cy - level; y <= cy + level; y++) {
				x2 = x;
				if (x2 < 0) x2 += size.width;
				if (x2 >= size.width) x2 -= size.width;
				y2 = y;
				if (y2 < 0) y2 += size.height;
				if (y2 >= size.height) y2 -= size.height;
				if (cells[level][x2][y2]) System.out.print("1"); else System.out.print("0");
			} System.out.println();
		}
		System.out.println();
		dumpCellSub(level + 1, cx, cy);
	}
}

