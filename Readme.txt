A Robust Game of Life

Use build.bat/build.sh to build.

To run application:

java robustlife.RobustLifeApplication [-automatonSize <width> <height>]
   [-screenSize <width> <height>] [-range <visibility range>] [-noise <noise quantum>]

To run test driver:

 java robustlife.RobustLifeTest [-automatonSize <width> <height>]
    [-screenSize <width>] [-numTrials <number of trials>]
    [-trialLength <trial length (steps)>] [-cellDensity <cell density>]
    [-range <visibility range>] [-noise <noise quantum>]
    [-internalDistance <cell distance to self>] [-nodisplay]

Reference:
Tom Portegys and Janet Wiles, "A Robust Game of Life".
The International Conference on Complex Systems (ICCS2004).
http://tom.portegys.com/research/RobustLife.pdf
