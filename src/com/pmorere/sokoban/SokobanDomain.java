package com.pmorere.sokoban;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by philippe on 2/13/15.
 */


public class SokobanDomain extends GridWorldDomain {


    /**
     * Constant for the name of the rock attribute
     */
    public static final String ATTROCKATLOCATION = "attRockAtLocation";
    public static final String ATTROCKISBLOCKED = "attRockIsBlocked";
    public static final String ATTROCKEAST = "attRockEast";
    public static final String ATTROCKWEST = "attRockWest";
    public static final String ATTROCKNORTH = "attRockNorth";
    public static final String ATTROCKSOUTH = "attRockSouth";
    public static final String ATTWALLEAST = "attWallEast";
    public static final String ATTWALLWEST = "attWallWest";
    public static final String ATTWALLNORTH = "attWallNorth";
    public static final String ATTWALLSOUTH = "attWallSouth";


    /**
     * Constant for the name of the rock class
     */
    public static final String CLASSROCK = "rock";

    /**
     * Constant for the name of the wall to north propositional function
     */
    public static final String PFROCKNORTH = "rockToNorth";

    /**
     * Constant for the name of the wall to south propositional function
     */
    public static final String PFROCKSOUTH = "rockToSouth";

    /**
     * Constant for the name of the wall to east propositional function
     */
    public static final String PFROCKEAST = "rockToEast";

    /**
     * Constant for the name of the wall to west propositional function
     */
    public static final String PFROCKWEST = "rockToWest";

    /**
     * Constant for the name of the rock at location propositional function
     */
    public static final String PFROCKATLOCATION = "rockAtLocation";

    /**
     * Constant for the name of the rock is blocked propositional function
     */
    public static final String PFROCKISBLOCKED = "rockIsBlocked";


    public static final String PFEMPTYBEHINDEAST = "cellEmptyBehindEast";
    public static final String PFEMPTYBEHINDWEST = "cellEmptyBehindWest";
    public static final String PFEMPTYBEHINDNORTH = "cellEmptyBehindNorth";
    public static final String PFEMPTYBEHINDSOUTH = "cellEmptyBehindSouth";

    public static final String PFEMPTYBEHIND2EAST = "cellEmptyBehind2East";
    public static final String PFEMPTYBEHIND2WEST = "cellEmptyBehind2West";
    public static final String PFEMPTYBEHIND2NORTH = "cellEmptyBehind2North";
    public static final String PFEMPTYBEHIND2SOUTH = "cellEmptyBehind2South";

    public static final String PFLOCATIONBEHINDEAST = "locationBehindEast";
    public static final String PFLOCATIONBEHINDWEST = "locationBehindWest";
    public static final String PFLOCATIONBEHINDNORTH = "locationBehindNorth";
    public static final String PFLOCATIONBEHINDSOUTH = "locationBehindSouth";


    protected Map<String, PropositionalFunction> pfs;

    public SokobanDomain(int width, int height) {
        super(width, height);
    }

    public static void addRock(Domain d, State s, int x, int y) {
        int i = s.getObjectsOfTrueClass(CLASSROCK).size();
        ObjectInstance o = new ObjectInstance(d.getObjectClass(CLASSROCK), CLASSROCK + i);
        s.addObject(o);

        o.setValue(ATTX, x);
        o.setValue(ATTY, y);
    }

    /**
     * Creates a visual explorer or terminal explorer. By default a visual explorer is presented; use the "t" argument
     * to create terminal explorer. Will create a 4 rooms grid world with the agent in lower left corner and a location in
     * the upper right. Use w-a-s-d to move.
     *
     * @param args
     */
    public static void main(String[] args) {

        SokobanDomain skbdg = new SokobanDomain(11, 11);
        skbdg.setMapToSokoban();
        skbdg.setMapToFourRooms();


        //gwdg.setProbSucceedTransitionDynamics(0.75);

        Domain d = skbdg.generateDomain();
       State s = skbdg.getOneAgentOneLocationState(d);
        skbdg.setAgent(s, 0, 0);
        skbdg.setLocation(s, 0, 10, 10);
        skbdg.addRock(d, s, 7, 7);

        int expMode = 1;
        if (args.length > 0) {
            if (args[0].equals("v")) {
                expMode = 1;
            } else if (args[0].equals("t")) {
                expMode = 0;
            }
        }

        if (expMode == 0) {

            TerminalExplorer exp = new TerminalExplorer(d);
            exp.addActionShortHand("n", ACTIONNORTH);
            exp.addActionShortHand("e", ACTIONEAST);
            exp.addActionShortHand("w", ACTIONWEST);
            exp.addActionShortHand("s", ACTIONSOUTH);

            exp.exploreFromState(s);

        } else if (expMode == 1) {

            Visualizer v = SokobanVisualizer.getVisualizer(skbdg.getMap());
            VisualExplorer exp = new VisualExplorer(d, v, s);

            //use w-s-a-d-x
            exp.addKeyAction("w", ACTIONNORTH);
            exp.addKeyAction("s", ACTIONSOUTH);
            exp.addKeyAction("a", ACTIONWEST);
            exp.addKeyAction("d", ACTIONEAST);

            exp.initGUI();
        }


    }

    @Override
    public void horizontal1DNorthWall(int xi, int xf, int y) {
        throw new UnsupportedOperationException("Not possible in Sokoban.");
    }

    @Override
    public void vertical1DEastWall(int yi, int yf, int x) {
        throw new UnsupportedOperationException("Not possible in Sokoban.");
    }

    @Override
    public void set1DNorthWall(int x, int y) {
        throw new UnsupportedOperationException("Not possible in Sokoban.");
    }

    @Override
    public void set1DEastWall(int x, int y) {
        throw new UnsupportedOperationException("Not possible in Sokoban.");
    }

    /**
     * Sets a rock in the designated location.
     *
     * @param x the x coordinate of the obstacle
     * @param y the y coordinate of the obstacle
     */
    public void setRockInCell(int x, int y) {
        this.map[x][y] = 2;
    }

    /**
     * Sets the map at the specified location to have the specified wall configuration.
     *
     * @param x        the x coordinate of the location
     * @param y        the y coordinate of the location
     * @param wallType the wall configuration for this location. 0 = no walls; 1 = complete cell wall/obstacle; 2 = rock in cell
     */
    @Override
    public void setCellWallState(int x, int y, int wallType) {
        this.map[x][y] = wallType;
    }

    public void setMapToSokoban() {

        /*
        #: out of map
        X: wall
        o: rock
        P: player
        .: hole
        ###########
        #         #
        # o     XX#
        #   X X   #
        #P  X    .#
        ###########

         */
        this.width = 9;
        this.height = 9;
        this.makeEmptyMap();

        horizontalWall(0, 8, 4);
        horizontalWall(7, 8, 2);

        verticalWall(0, 1, 3);
        verticalWall(1, 1, 5);

    }

    public State getSokobanMapState(Domain d) {

        State s = getOneAgentOneLocationState(d);
        setAgent(s, 0, 0);
        setLocation(s, 0, 8, 0, 0);
        addRock(d, s, 1, 2);
        //addRock(d, s, 8, 1);
        //addRock(d, s, 2, 3);
        return s;

    }

    @Override
    public Domain generateDomain() {

        Domain domain = new SADomain();

        //Creates a new Attribute object
        Attribute xatt = new Attribute(domain, ATTX, Attribute.AttributeType.INT);
        xatt.setLims(0, this.width - 1);

        Attribute yatt = new Attribute(domain, ATTY, Attribute.AttributeType.INT);
        yatt.setLims(0., this.height - 1);

        Attribute ltatt = new Attribute(domain, ATTLOCTYPE, Attribute.AttributeType.DISC);
        ltatt.setDiscValuesForRange(0, numLocationTypes - 1, 1);


        ObjectClass agentClass = new ObjectClass(domain, CLASSAGENT);
        agentClass.addAttribute(xatt);
        agentClass.addAttribute(yatt);


        ObjectClass locationClass = new ObjectClass(domain, CLASSLOCATION);
        locationClass.addAttribute(xatt);
        locationClass.addAttribute(yatt);
        locationClass.addAttribute(ltatt);

        new MovementAction(ACTIONNORTH, domain, this.transitionDynamics[0]);
        new MovementAction(ACTIONSOUTH, domain, this.transitionDynamics[1]);
        new MovementAction(ACTIONEAST, domain, this.transitionDynamics[2]);
        new MovementAction(ACTIONWEST, domain, this.transitionDynamics[3]);

        // Prop functions
        new RockAtLocationPF(PFROCKATLOCATION, domain, new String[]{CLASSROCK, CLASSLOCATION});

        new RockIsBlockedPF(PFROCKISBLOCKED, domain, new String[]{CLASSROCK});

        new SokobanWallToPF(PFWALLNORTH, domain, new String[]{CLASSAGENT}, 0);
        new SokobanWallToPF(PFWALLSOUTH, domain, new String[]{CLASSAGENT}, 1);
        new SokobanWallToPF(PFWALLEAST, domain, new String[]{CLASSAGENT}, 2);
        new SokobanWallToPF(PFWALLWEST, domain, new String[]{CLASSAGENT}, 3);

        new RockToPF(PFROCKNORTH, domain, new String[]{CLASSAGENT}, 0);
        new RockToPF(PFROCKSOUTH, domain, new String[]{CLASSAGENT}, 1);
        new RockToPF(PFROCKEAST, domain, new String[]{CLASSAGENT}, 2);
        new RockToPF(PFROCKWEST, domain, new String[]{CLASSAGENT}, 3);

        new EmptyCellBehindPF(PFEMPTYBEHINDNORTH, domain, new String[]{CLASSAGENT}, 0, 2);
        new EmptyCellBehindPF(PFEMPTYBEHINDSOUTH, domain, new String[]{CLASSAGENT}, 1, 2);
        new EmptyCellBehindPF(PFEMPTYBEHINDEAST, domain, new String[]{CLASSAGENT}, 2, 2);
        new EmptyCellBehindPF(PFEMPTYBEHINDWEST, domain, new String[]{CLASSAGENT}, 3, 2);

        new EmptyCellBehindPF(PFEMPTYBEHIND2NORTH, domain, new String[]{CLASSAGENT}, 0, 3);
        new EmptyCellBehindPF(PFEMPTYBEHIND2SOUTH, domain, new String[]{CLASSAGENT}, 1, 3);
        new EmptyCellBehindPF(PFEMPTYBEHIND2EAST, domain, new String[]{CLASSAGENT}, 2, 3);
        new EmptyCellBehindPF(PFEMPTYBEHIND2WEST, domain, new String[]{CLASSAGENT}, 3, 3);

        new LocationBehindPF(PFLOCATIONBEHINDNORTH, domain, new String[]{CLASSAGENT, CLASSLOCATION}, 0);
        new LocationBehindPF(PFLOCATIONBEHINDSOUTH, domain, new String[]{CLASSAGENT, CLASSLOCATION}, 1);
        new LocationBehindPF(PFLOCATIONBEHINDEAST, domain, new String[]{CLASSAGENT, CLASSLOCATION}, 2);
        new LocationBehindPF(PFLOCATIONBEHINDWEST, domain, new String[]{CLASSAGENT, CLASSLOCATION}, 3);

        ObjectClass rockClass = new ObjectClass(domain, CLASSROCK);
        rockClass.addAttribute(xatt);
        rockClass.addAttribute(yatt);

        return domain;
    }

    /**
     * Attempts to move the agent into the given position, taking into account walls and blocks
     *
     * @param s  the current state
     * @param xd the attempted new X position of the agent
     * @param yd the attempted new Y position of the agent
     */
    protected void move(State s, int xd, int yd) {

        ObjectInstance agent = s.getObjectsOfTrueClass(CLASSAGENT).get(0);
        List<ObjectInstance> rocks = s.getObjectsOfTrueClass(CLASSROCK);

        int ax = agent.getDiscValForAttribute(ATTX);
        int ay = agent.getDiscValForAttribute(ATTY);

        int nx = ax + xd;
        int ny = ay + yd;
        int nnx = nx + xd;
        int nny = ny + yd;

        // Find if there are rocks in the direction
        boolean isRockInTheWay = false, isRockBehindTheWay = false;
        ObjectInstance rockInTheWay = null;
        for (ObjectInstance rock : rocks) {
            if (rock.getDiscValForAttribute(ATTX) == nx && rock.getDiscValForAttribute(ATTY) == ny) {
                isRockInTheWay = true;
                rockInTheWay = rock;
            } else if (rock.getDiscValForAttribute(ATTX) == nnx && rock.getDiscValForAttribute(ATTY) == nny)
                isRockBehindTheWay = true;
        }

        //hit wall, so do not change position
        if (nx < 0 || nx >= this.width || ny < 0 || ny >= this.height || this.map[nx][ny] == 1) {
            nx = ax;
            ny = ay;
        } else if (isRockInTheWay) {
            if ((nnx < 0 || nnx >= this.width || nny < 0 || nny >= this.height)) {
                nx = ax;
                ny = ay;
            } else {
                if (this.map[nnx][nny] == 0 && !isRockBehindTheWay) { //pushing a rock
                    rockInTheWay.setValue(ATTX, nnx);
                    rockInTheWay.setValue(ATTY, nny);
                } else { // the rock is blocked, do not change position
                    nx = ax;
                    ny = ay;
                }
            }
        }

        agent.setValue(ATTX, nx);
        agent.setValue(ATTY, ny);

        //updatePFAttributes(s);
    }

    protected void updatePFAttributes(State s) {
        // Rock is at location
        List<GroundedProp> gps = this.pfs.get(PFROCKATLOCATION).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance rock = s.getObject(gp.params[0]);
            rock.setValue(ATTROCKATLOCATION, gp.isTrue(s));
        }

        // Rock is blocked
        gps = this.pfs.get(PFROCKISBLOCKED).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance rock = s.getObject(gp.params[0]);
            rock.setValue(ATTROCKISBLOCKED, gp.isTrue(s));
        }

        // Rock to direction
        gps = this.pfs.get(PFROCKEAST).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTROCKEAST, gp.isTrue(s));
        }

        gps = this.pfs.get(PFROCKWEST).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTROCKWEST, gp.isTrue(s));
        }

        gps = this.pfs.get(PFROCKNORTH).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTROCKNORTH, gp.isTrue(s));
        }

        gps = this.pfs.get(PFROCKSOUTH).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTROCKSOUTH, gp.isTrue(s));
        }

        // Wall to direction
        gps = this.pfs.get(PFWALLEAST).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTWALLEAST, gp.isTrue(s));
        }

        gps = this.pfs.get(PFWALLWEST).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTWALLWEST, gp.isTrue(s));
        }

        gps = this.pfs.get(PFWALLNORTH).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTWALLNORTH, gp.isTrue(s));
        }

        gps = this.pfs.get(PFWALLSOUTH).getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            ObjectInstance agent = s.getObject(gp.params[0]);
            agent.setValue(ATTWALLSOUTH, gp.isTrue(s));
        }
    }

    protected abstract class RegisteredPropositionalFunction extends PropositionalFunction {
        public RegisteredPropositionalFunction(String name, Domain domain, String[] parameterClasses) {
            super(name, domain, parameterClasses);
            if (SokobanDomain.this.pfs == null)
                SokobanDomain.this.pfs = new HashMap<String, PropositionalFunction>();
            SokobanDomain.this.pfs.put(name, this);
        }
    }

    /**
     * Propositional function for indicating if a rock is in a given position relative to the agent position
     */
    public class RockToPF extends RegisteredPropositionalFunction {

        /**
         * The relative x distance from the agent of the cell to check
         */
        protected int xdelta;

        /**
         * The relative y distance from the agent of the cell to check
         */
        protected int ydelta;


        /**
         * Initializes the function.
         *
         * @param name             the name of the function
         * @param domain           the domain of the function
         * @param parameterClasses the object class parameter types
         * @param direction        the unit distance direction from the agent to check for a wall (0,1,2,3 corresponds to north,south,east,west).
         */
        public RockToPF(String name, Domain domain, String[] parameterClasses, int direction) {
            super(name, domain, parameterClasses);
            int[] dcomps = SokobanDomain.this.movementDirectionFromIndex(direction);
            xdelta = dcomps[0];
            ydelta = dcomps[1];
        }

        @Override
        public boolean isTrue(State st, String[] params) {

            ObjectInstance agent = st.getObject(params[0]);

            int ax = agent.getDiscValForAttribute(ATTX);
            int ay = agent.getDiscValForAttribute(ATTY);

            int cx = ax + xdelta;
            int cy = ay + ydelta;

            List<ObjectInstance> rocks = st.getObjectsOfTrueClass(CLASSROCK);
            for (ObjectInstance rock : rocks)
                if (rock.getDiscValForAttribute(ATTX) == cx && rock.getDiscValForAttribute(ATTY) == cy)
                    return true;

            return false;
        }
    }

    public class RockAtLocationPF extends RegisteredPropositionalFunction {


        /**
         * Initializes with given name domain and parameter object class types
         *
         * @param name             name of function
         * @param domain           the domain of the function
         * @param parameterClasses the object class types for the parameters
         */
        public RockAtLocationPF(String name, Domain domain, String[] parameterClasses) {
            super(name, domain, parameterClasses);
        }

        @Override
        public boolean isTrue(State st, String[] params) {

            ObjectInstance rock = st.getObject(params[0]);
            ObjectInstance location = st.getObject(params[1]);

            int rx = rock.getDiscValForAttribute(ATTX);
            int ry = rock.getDiscValForAttribute(ATTY);

            int lx = location.getDiscValForAttribute(ATTX);
            int ly = location.getDiscValForAttribute(ATTY);

            if (rx == lx && ry == ly) {
                return true;
            }

            return false;
        }

    }

    public class RockIsBlockedPF extends RegisteredPropositionalFunction {


        /**
         * Initializes with given name domain and parameter object class types
         *
         * @param name             name of function
         * @param domain           the domain of the function
         * @param parameterClasses the object class types for the parameters
         */
        public RockIsBlockedPF(String name, Domain domain, String[] parameterClasses) {
            super(name, domain, parameterClasses);
        }

        @Override
        public boolean isTrue(State st, String[] params) {

            ObjectInstance rock = st.getObject(params[0]);

            int rx = rock.getDiscValForAttribute(ATTX);
            int ry = rock.getDiscValForAttribute(ATTY);

            boolean wallN = ry + 1 >= SokobanDomain.this.height || ry < 0 || rx >= SokobanDomain.this.width ||
                    rx < 0 || SokobanDomain.this.map[rx][ry + 1] == 1;
            boolean wallS = ry - 1 < 0 || ry >= SokobanDomain.this.height || rx >= SokobanDomain.this.width ||
                    rx < 0 || SokobanDomain.this.map[rx][ry - 1] == 1;
            boolean wallW = rx - 1 < 0 || rx >= SokobanDomain.this.width || ry < 0 ||
                    ry >= SokobanDomain.this.height || SokobanDomain.this.map[rx - 1][ry] == 1;
            boolean wallE = rx + 1 >= SokobanDomain.this.width || rx < 0 || ry < 0 ||
                    ry >= SokobanDomain.this.height || SokobanDomain.this.map[rx + 1][ry] == 1;
            if (wallE && (wallN || wallS)) {
                return true;
            }
            if (wallW && (wallN || wallS)) {
                return true;
            }

            return false;
        }

    }

    /**
     * Propositional function for indicating if a wall is in a given position relative to the agent position
     */
    public class SokobanWallToPF extends RegisteredPropositionalFunction {

        /**
         * The relative x distance from the agent of the cell to check
         */
        protected int xdelta;

        /**
         * The relative y distance from the agent of the cell to check
         */
        protected int ydelta;


        /**
         * Initializes the function.
         *
         * @param name             the name of the function
         * @param domain           the domain of the function
         * @param parameterClasses the object class parameter types
         * @param direction        the unit distance direction from the agent to check for a wall (0,1,2,3 corresponds to north,south,east,west).
         */
        public SokobanWallToPF(String name, Domain domain, String[] parameterClasses, int direction) {
            super(name, domain, parameterClasses);
            int[] dcomps = SokobanDomain.this.movementDirectionFromIndex(direction);
            xdelta = dcomps[0];
            ydelta = dcomps[1];
        }

        @Override
        public boolean isTrue(State st, String[] params) {

            ObjectInstance agent = st.getObject(params[0]);

            int ax = agent.getDiscValForAttribute(ATTX);
            int ay = agent.getDiscValForAttribute(ATTY);

            int cx = ax + xdelta;
            int cy = ay + ydelta;

            if (cx < 0 || cx >= SokobanDomain.this.width || cy < 0 || cy >= SokobanDomain.this.height || SokobanDomain.this.map[cx][cy] == 1)
                return true;

            return false;
        }
    }

    public class EmptyCellBehindPF extends RegisteredPropositionalFunction {

        /**
         * The relative x distance from the agent of the cell to check
         */
        protected int xdelta;

        /**
         * The relative y distance from the agent of the cell to check
         */
        protected int ydelta;

        public EmptyCellBehindPF(String name, Domain domain, String[] parameterClasses, int direction, int dist) {
            super(name, domain, parameterClasses);
            int[] dcomps = SokobanDomain.this.movementDirectionFromIndex(direction);
            xdelta = dcomps[0] * dist;
            ydelta = dcomps[1] * dist;
        }

        @Override
        public boolean isTrue(State st, String[] params) {

            ObjectInstance agent = st.getObject(params[0]);

            int ax = agent.getDiscValForAttribute(ATTX);
            int ay = agent.getDiscValForAttribute(ATTY);

            int cx = ax + xdelta;
            int cy = ay + ydelta;

            if (cx < 0 || cx >= SokobanDomain.this.width || cy < 0 || cy >= SokobanDomain.this.height || SokobanDomain.this.map[cx][cy] != 0)
                return false;

            List<ObjectInstance> rocks = st.getObjectsOfTrueClass(CLASSROCK);
            for (ObjectInstance rock : rocks)
                if (rock.getDiscValForAttribute(ATTX) == cx && rock.getDiscValForAttribute(ATTY) == cy)
                    return false;

            return true;
        }
    }

    public class LocationBehindPF extends RegisteredPropositionalFunction {

        /**
         * The relative x distance from the agent of the cell to check
         */
        protected int xdelta;

        /**
         * The relative y distance from the agent of the cell to check
         */
        protected int ydelta;

        public LocationBehindPF(String name, Domain domain, String[] parameterClasses, int direction) {
            super(name, domain, parameterClasses);
            int[] dcomps = SokobanDomain.this.movementDirectionFromIndex(direction);
            xdelta = dcomps[0];
            ydelta = dcomps[1];
        }

        @Override
        public boolean isTrue(State st, String[] params) {

            ObjectInstance agent = st.getObject(params[0]);
            ObjectInstance location = st.getObject(params[1]);

            int ax = agent.getDiscValForAttribute(ATTX);
            int ay = agent.getDiscValForAttribute(ATTY);

            int cx = ax + xdelta * 2;
            int cy = ay + ydelta * 2;

            int lx = location.getDiscValForAttribute(ATTX);
            int ly = location.getDiscValForAttribute(ATTY);

            if(lx == cx && ly == cy)
                return true;
            return false;
        }
    }
}
