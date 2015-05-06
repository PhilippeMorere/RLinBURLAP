package com.pmorere.frostbite;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by philippe on 01/05/15.
 */
public class FrostbiteDomain implements DomainGenerator {
    /**
     * Constant for the name of the x position attribute.
     */
    public static final String XATTNAME = "xAtt";
    /**
     * Constant for the name of the y position attribute.
     */
    public static final String YATTNAME = "yAtt";
    /**
     * Constant for the name of the size of a frozen platform
     */
    public static final String SIZEATTNAME = "sizeAtt";
    /**
     * Constant for the name of the agent OO-MDP class
     */
    public static final String AGENTCLASS = "agent";
    /**
     * Constant for the name of the igloo OO-MDP class
     */
    public static final String IGLOOCLASS = "igloo";
    /**
     * Constant for the name of the building step of the igloo
     */
    public static final String BUILDINGATTNAME = "buildingAtt";
    /**
     * Constant for the name of the activated status of a platform
     */
    public static final String ACTIVATEDATTNAME = "activatedAtt";
    /**
     * Constant for the name of the obstacle OO-MDP class
     */
    public static final String PLATFORMCLASS = "platform";
    /**
     * Constant for the name of the north action
     */
    public static final String ACTIONNORTH = "north";
    /**
     * Constant for the name of the south action
     */
    public static final String ACTIONSOUTH = "south";
    /**
     * Constant for the name of the east action
     */
    public static final String ACTIONEAST = "east";
    /**
     * Constant for the name of the west action
     */
    public static final String ACTIONWEST = "west";
    /**
     * Constant for the name of the west action
     */
    public static final String ACTIONIDLE = "idle";


    public static final String PFONPLATFORM = "pfOnPlatform";
    public static final String PFPLATFORMACTIVE = "pfPlatformActive";
    public static final String PFONICE = "pfOnIce";
    public static final String PFIGLOOBUILT = "pfIglooBuilt";
    public static final String PFINWATER = "pfInWater";
    private static final int SCALE = 5;
    protected static final int gameHeight = 130 * SCALE;
    protected static final int gameIceHeight = gameHeight / 4;
    protected static final int gameWidth = 160 * SCALE;
    private static final int jumpSize = 22 * SCALE;
    private static final int stepSize = 2 * SCALE;
    private static final int jumpSpeed = jumpSize / 4;
    private static final int platformSpeed = 1 * SCALE;
    /**
     * Matrix specifying the transition dynamics in terms of movement directions. The first index
     * indicates the action direction attempted (ordered north, south, east, west) the second index
     * indicates the actual resulting direction the agent will go (assuming there is no wall in the way).
     * The value is the probability of that outcome. The existence of walls does not affect the probability
     * of the direction the agent will actually go, but if a wall is in the way, it will affect the outcome.
     * For instance, if the agent selects north, but there is a 0.2 probability of actually going east and
     * there is a wall to the east, then with 0.2 probability, the agent will stay in place.
     */
    protected double[][] transitionDynamics;
    protected int buildingStepsToWin = 16;
    private int numberPlatformRow = 4;
    private int numberPlatformCol = 4;
    private int leftToJump = 0;
    private int agentSize = 8 * SCALE;
    private int platformSpeedOnAgent = 0;
    private int platformSize = 15 * SCALE;
    private int spaceBetweenPlatforms = 26 * SCALE;

    public FrostbiteDomain() {
        setDeterministicTransitionDynamics();
    }

    public static void main(String[] args) {
        FrostbiteDomain fd = new FrostbiteDomain();
        Domain d = fd.generateDomain();
        State s = fd.getCleanState(d);

        Visualizer vis = FrostbiteVisualizer.getVisualizer(fd);
        VisualExplorer exp = new VisualExplorer(d, vis, s);

        exp.addKeyAction("a", ACTIONWEST);
        exp.addKeyAction("d", ACTIONEAST);
        exp.addKeyAction("w", ACTIONNORTH);
        exp.addKeyAction("s", ACTIONSOUTH);
        exp.addKeyAction("x", ACTIONIDLE);

        exp.initGUI();
    }

    /**
     * Sets the agent/lander position/orientation and the velocity.
     *
     * @param s the state in which to set the agent
     * @param x the x position of the agent
     * @param y the y position of the agent
     */
    public static void setAgent(State s, double x, double y) {
        ObjectInstance agent = s.getObjectsOfTrueClass(AGENTCLASS).get(0);

        agent.setValue(XATTNAME, x);
        agent.setValue(YATTNAME, y);
    }

    /**
     * Sets an obstacles boundaries/position
     *
     * @param s  the state in which the obstacle should be set
     * @param i  specifies the ith landing pad object to be set to these values
     * @param x  the left boundary
     * @param y  the right boundary
     * @param ss the bottom boundary
     */
    public static void setPlatform(State s, int i, double x, double y, double ss) {
        ObjectInstance platform = s.getObjectsOfTrueClass(PLATFORMCLASS).get(i);

        platform.setValue(XATTNAME, x);
        platform.setValue(YATTNAME, y);
        platform.setValue(SIZEATTNAME, ss);
        platform.setValue(ACTIVATEDATTNAME, false);
    }

    private void setPlatformRow(Domain d, State s, int row) {
        for (int i = 0; i < numberPlatformCol; i++) {
            ObjectInstance platform = new ObjectInstance(d.getObjectClass(PLATFORMCLASS), PLATFORMCLASS + (i + row * numberPlatformCol));
            s.addObject(platform);

            platform.setValue(XATTNAME, spaceBetweenPlatforms * i + ((row % 2 == 0) ? 0 : gameWidth / 3));
            platform.setValue(YATTNAME, gameIceHeight + jumpSize / 2 - platformSize / 2 + agentSize / 2 + jumpSize * row);
            platform.setValue(SIZEATTNAME, platformSize);
            platform.setValue(ACTIVATEDATTNAME, false);
        }
    }

    /**
     * Creates a state with one agent/lander, one landing pad, and no number of obstacles.
     *
     * @param domain the domain of the state to generate
     * @return a state object
     */
    public State getCleanState(Domain domain) {

        State s = new State();

        ObjectInstance agent = new ObjectInstance(domain.getObjectClass(AGENTCLASS), AGENTCLASS + "0");
        s.addObject(agent);
        ObjectInstance igloo = new ObjectInstance(domain.getObjectClass(IGLOOCLASS), IGLOOCLASS + "0");
        s.addObject(igloo);

        for (int i = 0; i < numberPlatformRow; i++)
            setPlatformRow(domain, s, i);

        setAgent(s, platformSize / 2 + agentSize / 2, gameIceHeight - jumpSize / 2);
        return s;
    }

    public int getAgentSize() {
        return agentSize;
    }

    /**
     * Will set the domain to use deterministic action transitions.
     */
    public void setDeterministicTransitionDynamics() {
        int na = 4;
        transitionDynamics = new double[na][na];
        for (int i = 0; i < na; i++) {
            for (int j = 0; j < na; j++) {
                if (i != j) {
                    transitionDynamics[i][j] = 0.;
                } else {
                    transitionDynamics[i][j] = 1.;
                }
            }
        }
    }

    @Override
    public Domain generateDomain() {

        Domain domain = new SADomain();

        //create attributes
        Attribute xatt = new Attribute(domain, XATTNAME, Attribute.AttributeType.INT);
        xatt.setLims(0, gameWidth);

        Attribute yatt = new Attribute(domain, YATTNAME, Attribute.AttributeType.INT);
        yatt.setLims(0, gameHeight);

        Attribute satt = new Attribute(domain, SIZEATTNAME, Attribute.AttributeType.INT);
        satt.setLims(0, gameWidth);

        Attribute batt = new Attribute(domain, BUILDINGATTNAME, Attribute.AttributeType.INT);
        satt.setLims(0, 256); // It's an Atari game, it should crash at some point!

        Attribute aatt = new Attribute(domain, ACTIVATEDATTNAME, Attribute.AttributeType.BOOLEAN);

        //create classes
        ObjectClass agentclass = new ObjectClass(domain, AGENTCLASS);
        agentclass.addAttribute(xatt);
        agentclass.addAttribute(yatt);

        ObjectClass platformclass = new ObjectClass(domain, PLATFORMCLASS);
        platformclass.addAttribute(xatt);
        platformclass.addAttribute(yatt);
        platformclass.addAttribute(satt);
        platformclass.addAttribute(aatt);

        ObjectClass iglooclass = new ObjectClass(domain, IGLOOCLASS);
        iglooclass.addAttribute(batt);

        //add actions
        new MovementAction(ACTIONSOUTH, domain, this.transitionDynamics[0]);
        new MovementAction(ACTIONNORTH, domain, this.transitionDynamics[1]);
        new MovementAction(ACTIONEAST, domain, this.transitionDynamics[2]);
        new MovementAction(ACTIONWEST, domain, this.transitionDynamics[3]);
        new ActionIdle(ACTIONIDLE, domain);


        //add pfs
        new PlatformActivePF(PFPLATFORMACTIVE, domain);
        new OnPlatformPF(PFONPLATFORM, domain);
        new InWaterPF(PFINWATER, domain);
        new OnIcePF(PFONICE, domain);
        new IglooBuiltPF(PFIGLOOBUILT, domain);

        return domain;
    }

    /**
     * Returns the change in x and y position for a given direction number.
     *
     * @param i the direction number (0,1,2,3 indicates north,south,east,west, respectively)
     * @return the change in direction for x and y; the first index of the returned double is change in x, the second index is change in y.
     */
    protected int[] movementDirectionFromIndex(int i) {

        int[] result = null;

        switch (i) {
            case 0:
                result = new int[]{0, 1};
                break;

            case 1:
                result = new int[]{0, -1};
                break;

            case 2:
                result = new int[]{1, 0};
                break;

            case 3:
                result = new int[]{-1, 0};

            default:
                break;
        }

        return result;
    }

    /**
     * Attempts to move the agent into the given position, taking into account walls and blocks
     *
     * @param s  the current state
     * @param xd the attempted new X position of the agent
     * @param yd the attempted new Y position of the agent
     */
    protected void move(State s, int xd, int yd) {

        ObjectInstance agent = s.getObjectsOfTrueClass(AGENTCLASS).get(0);
        int ax = agent.getDiscValForAttribute(XATTNAME);
        int ay = agent.getDiscValForAttribute(YATTNAME);

        int nx = ax + xd * stepSize;
        int ny = ay;

        // Is a jump triggered while player is on the ground?
        if (leftToJump <= 0 && yd != 0) {
            // Player can only jump when on a platform (except last line), or when hitting down on the top part
            if ((platformSpeedOnAgent != 0 && ay + yd * jumpSize < gameHeight - agentSize) || (platformSpeedOnAgent == 0 && yd > 0)) {
                leftToJump = yd * jumpSize;
                platformSpeedOnAgent = 0;
            }
        }

        // If the player is in the air, move it.
        if (leftToJump < 0) {
            int jumpIncrement = Math.max(-jumpSpeed, leftToJump);
            leftToJump -= jumpIncrement;
            ny += jumpIncrement;
        } else if (leftToJump > 0) {
            int jumpIncrement = Math.min(jumpSpeed, leftToJump);
            leftToJump -= jumpIncrement;
            ny += jumpIncrement;
        }

        // If agent is on platform make it move with the platform
        if (leftToJump == 0)
            nx += platformSpeedOnAgent;

        // If agent goes out of the screen, stop it.
        if (nx < 0 || nx >= gameWidth - agentSize || ny < 0 || ny >= gameHeight - agentSize) {
            nx = ax;
            ny = ay;
        }

        agent.setValue(XATTNAME, nx);
        agent.setValue(YATTNAME, ny);

        update(s);
    }

    private void update(State s) {
        // Move the platforms
        List<ObjectInstance> platforms = s.getObjectsOfTrueClass(PLATFORMCLASS);
        for (int i = 0; i < platforms.size(); i++) {
            int directionL = ((i / numberPlatformCol) % 2 == 0) ? 1 : -1;
            int x = platforms.get(i).getDiscValForAttribute(XATTNAME) + directionL * platformSpeed;
            if (x < 0)
                x += gameWidth;
            platforms.get(i).setValue(XATTNAME, x % gameWidth);
        }

        // Player landed
        if (leftToJump == 0) {
            // Just landed: Potentially activate some platforms
            if (platformSpeedOnAgent == 0)
                activatePlatforms(s);

            platformSpeedOnAgent = getLandedPlatformSpeed(s);

            // Check if the agent landed on the top or in the water
            ObjectInstance agent = s.getObjectsOfTrueClass(AGENTCLASS).get(0);
            int ay = agent.getDiscValForAttribute(YATTNAME) + agentSize / 2;
            if (platformSpeedOnAgent == 0 && ay >= gameIceHeight) {
                System.out.println("Lost");
                System.exit(0);
            }

        }

        // If all platforms are active, deactivate them
        for (int i = 0; i < platforms.size(); i++)
            if (!platforms.get(i).getBooleanValue(ACTIVATEDATTNAME))
                return;
        for (int i = 0; i < platforms.size(); i++)
            platforms.get(i).setValue(ACTIVATEDATTNAME, false);
    }

    private void activatePlatforms(State s) {
        ObjectInstance agent = s.getObjectsOfTrueClass(AGENTCLASS).get(0);
        int ax = agent.getDiscValForAttribute(XATTNAME) + agentSize / 2;
        int ay = agent.getDiscValForAttribute(YATTNAME) + agentSize / 2;
        List<ObjectInstance> platforms = s.getObjectsOfTrueClass(PLATFORMCLASS);
        for (int i = 0; i < platforms.size(); i++) {
            ObjectInstance platform = platforms.get(i);
            if (!platform.getBooleanValue(ACTIVATEDATTNAME))
                if (pointInPlatform(ax, ay, platform.getDiscValForAttribute(XATTNAME), platform.getDiscValForAttribute(YATTNAME), platform.getDiscValForAttribute(SIZEATTNAME))) {
                    for (int j = numberPlatformCol * (i / numberPlatformCol); j < numberPlatformCol * (1 + i / numberPlatformCol); j++)
                        platforms.get(j).setValue(ACTIVATEDATTNAME, true);
                    ObjectInstance igloo = s.getFirstObjectOfClass(IGLOOCLASS);
                    igloo.setValue(BUILDINGATTNAME, igloo.getDiscValForAttribute(BUILDINGATTNAME) + 1);
                    break;
                }
        }
    }

    private int getLandedPlatformSpeed(State s) {
        ObjectInstance agent = s.getObjectsOfTrueClass(AGENTCLASS).get(0);
        int ax = agent.getDiscValForAttribute(XATTNAME) + agentSize / 2;
        int ay = agent.getDiscValForAttribute(YATTNAME) + agentSize / 2;
        List<ObjectInstance> platforms = s.getObjectsOfTrueClass(PLATFORMCLASS);
        for (int i = 0; i < platforms.size(); i++) {
            ObjectInstance platform = platforms.get(i);
            if (pointInPlatform(ax, ay, platform.getDiscValForAttribute(XATTNAME), platform.getDiscValForAttribute(YATTNAME), platform.getDiscValForAttribute(SIZEATTNAME)))
                return ((i / numberPlatformCol) % 2 == 0) ? platformSpeed : -platformSpeed;
        }
        return 0;
    }

    private boolean pointInPlatform(int px, int py, int x, int y, int s) {
        if (pointInPlatformHelper(px, py, x, y, s))
            return true;
        if (x + s > FrostbiteDomain.gameWidth && pointInPlatformHelper(px, py, x - gameWidth, y, s))
            return true;
        else if (x < 0 && pointInPlatformHelper(px, py, x + gameWidth, y, s))
            return true;
        return false;
    }

    private boolean pointInPlatformHelper(int px, int py, int x, int y, int s) {
        return px > x && px < x + s && py > y && py < y + s;
    }

    public class MovementAction extends Action {

        /**
         * Probabilities of the actual direction the agent will go
         */
        protected double[] directionProbs;

        /**
         * Random object for sampling distribution
         */
        protected Random rand;


        /**
         * Initializes for the given name, domain and actually direction probabilities the agent will go
         *
         * @param name       name of the action
         * @param domain     the domain of the action
         * @param directions the probability for each direction (index 0,1,2,3 corresponds to north,south,east,west, respectively).
         */
        public MovementAction(String name, Domain domain, double[] directions) {
            super(name, domain, "");
            this.directionProbs = directions;
            this.rand = RandomFactory.getMapped(0);
        }

        @Override
        protected State performActionHelper(State st, String[] params) {

            double roll = rand.nextDouble();
            double curSum = 0.;
            int dir = 0;
            for (int i = 0; i < directionProbs.length; i++) {
                curSum += directionProbs[i];
                if (roll < curSum) {
                    dir = i;
                    break;
                }
            }

            int[] dcomps = FrostbiteDomain.this.movementDirectionFromIndex(dir);
            FrostbiteDomain.this.move(st, dcomps[0], dcomps[1]);

            return st;
        }

        @Override
        public List<TransitionProbability> getTransitions(State st, String[] params) {

            List<TransitionProbability> transitions = new ArrayList<TransitionProbability>();
            for (int i = 0; i < directionProbs.length; i++) {
                double p = directionProbs[i];
                if (p == 0.) {
                    continue; //cannot transition in this direction
                }
                State ns = st.copy();
                int[] dcomps = FrostbiteDomain.this.movementDirectionFromIndex(i);
                FrostbiteDomain.this.move(ns, dcomps[0], dcomps[1]);

                //make sure this direction doesn't actually stay in the same place and replicate another no-op
                boolean isNew = true;
                for (TransitionProbability tp : transitions) {
                    if (tp.s.equals(ns)) {
                        isNew = false;
                        tp.p += p;
                        break;
                    }
                }

                if (isNew) {
                    TransitionProbability tp = new TransitionProbability(ns, p);
                    transitions.add(tp);
                }


            }


            return transitions;
        }


    }

    public class ActionIdle extends Action {

        /**
         * Initializes the idle action.
         *
         * @param name   the name of the action
         * @param domain the domain of the action.
         */
        public ActionIdle(String name, Domain domain) {
            super(name, domain, "");
        }


        @Override
        protected State performActionHelper(State st, String[] params) {
            FrostbiteDomain.this.move(st, 0, 0);
            return st;
        }

        @Override
        public List<TransitionProbability> getTransitions(State s, String[] params) {
            return this.deterministicTransition(s, params);
        }

    }

    public class OnPlatformPF extends PropositionalFunction {
        /**
         * Initializes to be evaluated on an agent object and platform object.
         *
         * @param name   the name of the propositional function
         * @param domain the domain of the propositional function
         */
        public OnPlatformPF(String name, Domain domain) {
            super(name, domain, new String[]{AGENTCLASS, PLATFORMCLASS});
        }


        @Override
        public boolean isTrue(State st, String[] params) {
            if (leftToJump != 0)
                return false;

            ObjectInstance agent = st.getObject(params[0]);
            ObjectInstance platform = st.getObject(params[1]);

            int x = platform.getDiscValForAttribute(XATTNAME);
            int y = platform.getDiscValForAttribute(YATTNAME);
            int s = platform.getDiscValForAttribute(SIZEATTNAME);

            int ax = agent.getDiscValForAttribute(XATTNAME) + agentSize / 2;
            int ay = agent.getDiscValForAttribute(YATTNAME) + agentSize / 2;

            return pointInPlatform(ax, ay, x, y, s);
        }


    }

    public class PlatformActivePF extends PropositionalFunction {
        /**
         * Initializes to be evaluated on an agent object and platform object.
         *
         * @param name   the name of the propositional function
         * @param domain the domain of the propositional function
         */
        public PlatformActivePF(String name, Domain domain) {
            super(name, domain, new String[]{PLATFORMCLASS});
        }

        @Override
        public boolean isTrue(State st, String[] params) {
            ObjectInstance platform = st.getObject(params[0]);
            return platform.getBooleanValue(ACTIVATEDATTNAME);
        }
    }

    public class InWaterPF extends PropositionalFunction {
        /**
         * Initializes to be evaluated on an agent object.
         *
         * @param name   the name of the propositional function
         * @param domain the domain of the propositional function
         */
        public InWaterPF(String name, Domain domain) {
            super(name, domain, new String[]{AGENTCLASS});
        }


        @Override
        public boolean isTrue(State st, String[] params) {
            if (leftToJump != 0)
                return false;

            ObjectInstance agent = st.getObject(params[0]);

            // Agent is on a platform
            if (getLandedPlatformSpeed(st) != 0)
                return false;

            int ay = agent.getDiscValForAttribute(YATTNAME) + agentSize / 2;
            return ay >= gameIceHeight;
        }
    }

    public class OnIcePF extends PropositionalFunction {
        /**
         * Initializes to be evaluated on an agent object.
         *
         * @param name   the name of the propositional function
         * @param domain the domain of the propositional function
         */
        public OnIcePF(String name, Domain domain) {
            super(name, domain, new String[]{AGENTCLASS});
        }


        @Override
        public boolean isTrue(State st, String[] params) {
            ObjectInstance agent = st.getObject(params[0]);

            int ay = agent.getDiscValForAttribute(YATTNAME) + agentSize / 2;
            return ay < gameIceHeight;
        }
    }

    public class IglooBuiltPF extends PropositionalFunction {
        /**
         * Initializes to be evaluated on an agent object.
         *
         * @param name   the name of the propositional function
         * @param domain the domain of the propositional function
         */
        public IglooBuiltPF(String name, Domain domain) {
            super(name, domain, new String[]{IGLOOCLASS});
        }

        @Override
        public boolean isTrue(State st, String[] params) {
            ObjectInstance igloo = st.getObject(params[0]);

            int building = igloo.getDiscValForAttribute(BUILDINGATTNAME);
            return building >= buildingStepsToWin;
        }
    }
}
