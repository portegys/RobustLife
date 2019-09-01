/*
 * Robust Game of Life application.
 *
 * Usage:
 *
 * java robustlife.RobustLifeApplication [-automatonSize <width> <height>]
 *	[-screenSize <width> <height>] [-range <visibility range>] [-noise <noise quantum>]
 */

package robustlife;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

// RobustLife application.
public class RobustLifeApplication implements Runnable
{
	// Update frequency (ms).
	static final int MIN_DELAY = 5;
	static final int MAX_DELAY = 500;
	static final int MAX_SLEEP = 100;
	int delay = MAX_DELAY;
    Thread updateThread;

	// Default parameters.
	static final Dimension DEFAULT_AUTOMATON_SIZE = new Dimension(50, 50);
	static final Dimension DEFAULT_SCREEN_SIZE = new Dimension(600, 700);
	static final int DEFAULT_VISIBILITY_RANGE = 5;
	static final double DEFAULT_NOISE_QUANTUM = 0.0;

	static Dimension automatonSize = DEFAULT_AUTOMATON_SIZE;
	static Dimension screenSize = DEFAULT_SCREEN_SIZE;
	static int range = DEFAULT_VISIBILITY_RANGE;
	static double noise = DEFAULT_NOISE_QUANTUM;

	// RobustLife automaton.
	RobustLife robustLife;

	// Control panel.
	class Controls extends JPanel implements ActionListener, ChangeListener
	{
		// Components.
		JSlider speedSlider;
		JButton stepButton;
		JButton clearButton;
 		JComboBox operationChoice;
 		JTextField noiseText;
 		JButton noiseButton;
		JTextField inputText;
  		JTextField outputText;

  		// Code base.
  		URL baseURL;

		// Constructor.
		Controls()
		{
			setLayout(new GridLayout(3, 1));
			setBorder(BorderFactory.createRaisedBevelBorder());
			JPanel panel = new JPanel();
			panel.add(new JLabel("Speed:   Fast", Label.RIGHT));
			speedSlider = new JSlider(JSlider.HORIZONTAL, MIN_DELAY,
				MAX_DELAY, MAX_DELAY);
			speedSlider.addChangeListener(this);
			panel.add(speedSlider);
			panel.add(new JLabel("Stop", Label.LEFT));
			stepButton = new JButton("Step");
			stepButton.addActionListener(this);
			panel.add(stepButton);
			clearButton = new JButton("Clear");
			clearButton.addActionListener(this);
			panel.add(clearButton);
			add(panel);
			panel = new JPanel();
			panel.add(new JLabel("Noise: "));
			noiseText = new JTextField("", 10);
			noiseText.addActionListener(this);
			panel.add(noiseText);
			noiseButton = new JButton("Set");
			noiseButton.addActionListener(this);
			panel.add(noiseButton);
			panel.add(new JLabel("File: "));
			inputText = new JTextField("", 20);
			inputText.addActionListener(this);
			panel.add(inputText);
 			operationChoice = new JComboBox();
			operationChoice.addItem("Load");
			operationChoice.addItem("Save");
			operationChoice.addActionListener(this);
			panel.add(operationChoice);
			add(panel);
			panel = new JPanel();
			panel.add(new JLabel("Status: "));
  			outputText = new JTextField("", 40);
     		outputText.setEditable(false);
     		panel.add(outputText);
     		add(panel);

       		// Get base URL.
			try {
				String s = System.getProperty("user.dir");
				baseURL = new File(s).toURL();
			}
			catch (SecurityException e) {
				System.err.println("Cannot get property for current directory");
			}
			catch (MalformedURLException e) {
				System.err.println("Cannot get URL of current directory");
			}
		}

		// Speed slider listener.
		public void stateChanged(ChangeEvent evt)
		{
			outputText.setText("");
			delay = speedSlider.getValue();
		}

		// Input text listener.
		public void actionPerformed(ActionEvent evt)
		{
			outputText.setText("");

			// Set Noise?
			if (evt.getSource() == (Object)noiseButton ||
				evt.getSource() == (Object)noiseText)
			{
				double newNoise = -1.0;
				try
				{
					newNoise = Double.parseDouble(noiseText.getText().trim());
				} catch (NumberFormatException e) {}
				if (newNoise < 0.0 || newNoise > 1.0)
				{
					outputText.setText("Invalid noise quantum");
				} else {
					noise = newNoise;
					robustLife.setNoise(noise);
					outputText.setText("Noise quantum = " + noise);
				}
				return;
			}

			// Step?
			if (evt.getSource() == (Object)stepButton)
			{
				robustLife.step();
				robustLife.display();
				return;
			}

			// Clear?
			if (evt.getSource() == (Object)clearButton)
			{
				robustLife.clear();
				return;
			}

			// Load/save.
			String fileName = inputText.getText().trim();
			if (fileName.equals("")) return;
			try
			{
				if (operationChoice.getSelectedIndex() == 0)
				{
					robustLife.load(fileName, baseURL);
					outputText.setText(fileName + " loaded");
				} else {
					robustLife.save(fileName);
					outputText.setText(fileName + " saved");
				}
			} catch(IOException e) {
				outputText.setText(e.getMessage());
			}
		}
	}

	// Controls.
	Controls controls;

	// Constructor.
	public RobustLifeApplication()
	{
		// Set up screen.
		JFrame screen = new JFrame("Robust Game of Life");
        screen.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
        screen.setSize(screenSize);
		screen.getContentPane().setLayout(new BorderLayout());

		// Create display.
		Dimension displaySize = new Dimension((int)((double)screenSize.width * .99),
			(int)((double)screenSize.height * .80));
		robustLife = new RobustLife(automatonSize, displaySize, range);
		robustLife.setNoise(noise);
		screen.getContentPane().add(robustLife, BorderLayout.NORTH);

		// Create controls.
		controls = new Controls();
		screen.getContentPane().add(controls);

		// Make screen visible.
        screen.setVisible(true);

		// Start update thread.
		updateThread = new Thread(this);
		updateThread.start();
	}

    // Run.
    public void run()
    {
		int timer = 0;

        // Lower thread's priority.
        if (Thread.currentThread() == updateThread)
        {
        	Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		}

        // Update loop.
        while (Thread.currentThread() == updateThread && !updateThread.isInterrupted())
        {
			if (delay < MAX_DELAY && timer >= delay)
			{
				robustLife.step();
				timer = 0;
			}
 			robustLife.display();

            try
            {
				if (delay < MAX_SLEEP)
				{
					Thread.sleep(delay);
					if (timer < MAX_DELAY) timer += delay;
				} else {
					Thread.sleep(MAX_SLEEP);
					if (timer < MAX_DELAY) timer += MAX_SLEEP;
				}
            } catch(InterruptedException e) { break; }
        }
    }

    // Main.
    public static void main(String[] args)
    {
        // Get arguments.
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
			} else {
				System.err.println("Usage:");
				System.err.println("java robustlife.RobustLifeApplication [-automatonSize <width> <height>]");
				System.err.println("\t[-screenSize <width> <height>] [-range <visibility range>] [-noise <noise quantum>]");
				System.exit(1);
			}
		}

        // Create the application.
        new RobustLifeApplication();
    }
}
