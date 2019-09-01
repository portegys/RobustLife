/*
 * Test Robust Game of Life.
 * Control runs are compared against noise-disrupted experimental runs.
 *
 * Usage:
 * java robustlife.RobustLifeTest [-automatonSize <width> <height>]
 *	[-screenSize <width>] [-numTrials <number of trials>]
 *	[-trialLength <trial length (steps)>] [-cellDensity <cell density>]
 *	[-range <visibility range>] [-noise <noise quantum>]
 *  [-internalDistance <cell distance to self>] [-nodisplay]
 */

package robustlife;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

// RobustLife test.
public class RobustLifeTest
{
	// Parameters.
	static final Dimension DEFAULT_AUTOMATON_SIZE = new Dimension(50, 50);
	static final Dimension DEFAULT_SCREEN_SIZE = new Dimension(600, 700);
	static final int DEFAULT_NUM_TRIALS = 50;
	static final int DEFAULT_TRIAL_LENGTH = 50;
	static final double DEFAULT_CELL_DENSITY = 0.1;
	static final int DEFAULT_VISIBILITY_RANGE = 5;
	static final double DEFAULT_NOISE_QUANTUM = 0.00005;
	static final double DEFAULT_INTERNAL_DISTANCE = 0.0;

	static Dimension automatonSize = DEFAULT_AUTOMATON_SIZE;
	static Dimension screenSize = DEFAULT_SCREEN_SIZE;
	static int numTrials = DEFAULT_NUM_TRIALS;
	static int trialLength = DEFAULT_TRIAL_LENGTH;
	static double cellDensity = DEFAULT_CELL_DENSITY;
	static int range = DEFAULT_VISIBILITY_RANGE;
	static double noise = DEFAULT_NOISE_QUANTUM;
	static double internalDistance = DEFAULT_INTERNAL_DISTANCE;
	static boolean display = true;

    // Main.
    public static void main(String[] args)
    {
		JFrame controlScreen, experimentScreen;
		RobustLife control, experiment;
		Random random;
		long randomSeed;
		int i,j,x,y;
		boolean error;
		int errors;

		// Get arguments.
		getArgs(args);

		// Create automata.
		control = new RobustLife(automatonSize, screenSize, 1);
		if (display)
		{
			controlScreen = new JFrame("Control");
			controlScreen.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) { System.exit(0); }
			});
			controlScreen.setSize(screenSize);
			controlScreen.getContentPane().setLayout(new BorderLayout());
			controlScreen.getContentPane().add(control, BorderLayout.NORTH);
        	controlScreen.setVisible(true);
		}
		experiment = new RobustLife(automatonSize, screenSize, range);
		experiment.setNoise(noise);
		experiment.setInternalDistance(internalDistance);
		if (display)
		{
			experimentScreen = new JFrame("Experiment");
			experimentScreen.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) { System.exit(0); }
			});
			experimentScreen.setSize(screenSize);
			experimentScreen.getContentPane().setLayout(new BorderLayout());

			experimentScreen.getContentPane().add(experiment, BorderLayout.NORTH);
			experimentScreen.setVisible(true);
		}

        // Print parameters.
		System.out.println("Parameters:");
		System.out.println("Automaton size = " + automatonSize.width + "x" + automatonSize.height);
		System.out.println("Screen size = " + screenSize.width + "x" + screenSize.height);
		System.out.println("Number of trials = " + numTrials);
		System.out.println("Trial length = " + trialLength);
		System.out.println("Cell density = " + cellDensity);
		System.out.println("Visibility range = " + range);
		System.out.println("Noise quantum = " + noise);
		System.out.println("Internal distance = " + internalDistance);

  		// Run trials.
  		System.out.println("Begin trials:");
  		random = new Random(new Date().getTime());
  		errors = 0;
		for (i = 0; i < numTrials; i++)
		{
			System.out.print("Trial=" + i + "...");
			randomSeed = random.nextLong();

			// Run control automaton.
			initAutomaton(control, randomSeed);
			for (j = 0; j < trialLength; j++)
			{
				control.step();
				if (display) control.display();
			}

			// Run experimental automaton.
			initAutomaton(experiment, randomSeed);
			for (j = 0; j < trialLength; j++)
			{
				experiment.step();
				if (display) experiment.display();
			}

			// Check for errors.
			error = false;
			for (x = 0; x < automatonSize.width; x++)
			{
				for (y = 0; y < automatonSize.height; y++)
				{
					if (control.cells[0][x][y] != experiment.cells[0][x][y])
					{
						error = true;
					}
				}
			}
			if (error)
			{
				errors++;
				System.out.println("Error");
			} else {
				System.out.println("OK");
			}
		}

		System.out.println("Success rate=" +
			((double)(numTrials - errors) / (double)numTrials) +
			" (" + (numTrials - errors) + "/" + numTrials + ")");
		System.exit(0);
    }

    // Get arguments.
    private static void getArgs(String[] args)
    {
        for (int i = 0; i < args.length;)
        {
			if (args[i].equals("-automatonSize"))
			{
				i++;
				automatonSize.width = automatonSize.height = -1;
				if (i < args.length)
				{
					automatonSize.width = Integer.parseInt(args[i]);
					i++;
				}
				if (i < args.length)
				{
					automatonSize.height = Integer.parseInt(args[i]);
					i++;
				}
				if (automatonSize.width <= 0 || automatonSize.height <= 0)
				{
					System.err.println("Invalid automaton size");
					System.exit(1);
				}
			} else if (args[i].equals("-screenSize"))
			{
				i++;
				screenSize.width = screenSize.height = -1;
				if (i < args.length)
				{
					screenSize.width = Integer.parseInt(args[i]);
					i++;
				}
				if (i < args.length)
				{
					screenSize.height = Integer.parseInt(args[i]);
					i++;
				}
				if (screenSize.width <= 0 || screenSize.height <= 0)
				{
					System.err.println("Invalid screen size");
					System.exit(1);
				}
			} else if (args[i].equals("-numTrials"))
			{
				i++;
				numTrials = -1;
				if (i < args.length)
				{
					numTrials = Integer.parseInt(args[i]);
					i++;
				}
				if (numTrials < 0)
				{
					System.err.println("Invalid number of trials");
					System.exit(1);
				}
			} else if (args[i].equals("-trialLength"))
			{
				i++;
				trialLength = -1;
				if (i < args.length)
				{
					trialLength = Integer.parseInt(args[i]);
					i++;
				}
				if (trialLength < 0)
				{
					System.err.println("Invalid trial length");
					System.exit(1);
				}
			} else if (args[i].equals("-cellDensity"))
			{
				i++;
				cellDensity = -1.0;
				if (i < args.length)
				{
					cellDensity = Double.parseDouble(args[i]);
					i++;
				}
				if (cellDensity < 0.0 || cellDensity > 1.0)
				{
					System.err.println("Invalid cell density");
					System.exit(1);
				}
			} else if (args[i].equals("-range"))
			{
				i++;
				range = -1;
				if (i < args.length)
				{
					try
					{
						range = Integer.parseInt(args[i]);
					} catch (NumberFormatException e) {}
					i++;
				}
				if (range <= 0)
				{
					System.err.println("Invalid visibility range");
					System.exit(1);
				}
			} else if (args[i].equals("-noise"))
			{
				i++;
				noise = -1.0;
				if (i < args.length)
				{
					try
					{
						noise = Double.parseDouble(args[i]);
					} catch (NumberFormatException e) {}
					i++;
				}
				if (noise < 0.0 || noise > 1.0)
				{
					System.err.println("Invalid noise quantum");
					System.exit(1);
				}
			} else if (args[i].equals("-internalDistance"))
			{
				i++;
				internalDistance = -1.0;
				if (i < args.length)
				{
					try
					{
						internalDistance = Double.parseDouble(args[i]);
					} catch (NumberFormatException e) {}
					i++;
				}
				if (internalDistance < 0.0)
				{
					System.err.println("Invalid internal distance");
					System.exit(1);
				}
			} else if (args[i].equals("-nodisplay"))
			{
				i++;
				display = false;
			} else {
				System.err.println("Usage:");
				System.err.println("java robustlife.RobustLifeTest [-automatonSize <width> <height>]");
				System.err.println("\t[-screenSize <width>] [-numTrials <number of trials>]");
				System.err.println("\t[-trialLength <trial length (steps)>] [-cellDensity <cell density>]");
				System.err.println("\t[-range <visibility range>] [-noise <noise quantum>]");
				System.err.println("\t[-internalDistance <cell distance to self>] [-nodisplay]");
				System.exit(1);
			}
		}
	}

    // Initialize automaton.
    private static void initAutomaton(RobustLife automaton, long randomSeed)
    {
		int x,y;
		Random random = new Random(randomSeed);

		automaton.setRandomSeed(randomSeed);
		automaton.clear();
		for (x = 0; x < automatonSize.width; x++)
		{
			for (y = 0; y < automatonSize.height; y++)
			{
				if (random.nextDouble() < cellDensity)
				{
					automaton.cells[0][x][y] = true;
				}
			}
		}
		automaton.synchCount = range - 1;
	}
}
