package com.pmorere.testing;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.doormax.Doormax;
import com.pmorere.modellearning.scaffolding.Scaffolding;
import com.pmorere.modellearning.scaffolding.Tree;
import com.pmorere.sokoban.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 10/03/15.
 */
public class TestingScaffoldingSokoban {
    SokobanDomain gwdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;
    List<PropositionalFunction> pfs;

    public TestingScaffoldingSokoban() {
    }

    public static void main(String[] args) {

        TestingScaffoldingSokoban example = new TestingScaffoldingSokoban();
        String outputPath = "output/"; //directory to record results

        //we will call planning and learning algorithms here
        example.scaffoldingExample(outputPath);
    }


    public void createFullDomain() {
        //create the domain
        gwdg = new SokobanDomain(11, 11);
        gwdg.setMapToFourRooms();
        //((SokobanDomain)gwdg).setMapToSokoban();
        domain = gwdg.generateDomain();

        //create the state parser
        sp = new SokobanStateParser(domain);

        //define the task
        //rf = new UniformCostRF();
        rf = new SokobanRF(1, -1);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKISBLOCKED), -10);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKATLOCATION), 1000);

        tf = new SokobanTF(domain);
        goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the task
        initialState = gwdg.getOneAgentOneLocationState(domain);
        gwdg.setAgent(initialState, 0, 0);
        gwdg.setLocation(initialState, 0, 10, 10);
        ((SokobanDomain) gwdg).addRock(domain, initialState, 7, 7);
        //initialState = ((SokobanDomain)gwdg).getSokobanMapState(domain);

        //set up the state hashing system
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSROCK,
                domain.getObjectClass(SokobanDomain.CLASSROCK).attributeList);


        //set up the pfs
        pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());
        pfs.remove(domain.getPropFunction(SokobanDomain.PFATLOCATION));
    }

    public Tree.Node createRoomRockDomainAgent(Scaffolding scaff, Tree.Node parentStep) {
        //create the domain
        SokobanDomain gwdg = new SokobanDomain(6, 6);
        int[][] map = {
                {1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1}};
        gwdg.setMap(map);
        Domain domain = gwdg.generateDomain();

        //create the state parser
        StateParser sp = new SokobanStateParser(domain);

        //define the task
        RewardFunction rf = new SokobanRF(1, -1);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKISBLOCKED), -10);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKATLOCATION), 1000);

        TerminalFunction tf = new SokobanTF(domain);
        StateConditionTest goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        State initialState = gwdg.getOneAgentOneLocationState(domain);
        gwdg.setAgent(initialState, 2, 2);
        gwdg.setLocation(initialState, 0, 1, 1);
        ((SokobanDomain) gwdg).addRock(domain, initialState, 3, 3);

        //set up the state hashing system
        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);

        //set up the pfs
        List<PropositionalFunction> pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());

        // Create the agent and add it to the scaffolding tree
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        return scaff.addScaffoldingElementTo(parentStep, "Rock_Room_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 30, 20);
    }

    public Tree.Node createRoomDomainAgent(Scaffolding scaff, Tree.Node parentStep) {
        //create the domain
        SokobanDomain gwdg = new SokobanDomain(6, 6);
        int[][] map = {
                {1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1}};
        gwdg.setMap(map);
        Domain domain = gwdg.generateDomain();

        //create the state parser
        StateParser sp = new SokobanStateParser(domain);

        //define the task
        RewardFunction rf = new SokobanRF(1, -1);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKISBLOCKED), -10);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKATLOCATION), 1000);

        TerminalFunction tf = new SokobanTF(domain);
        StateConditionTest goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        State initialState = gwdg.getOneAgentNoLocationState(domain);
        gwdg.setAgent(initialState, 3, 3);

        //set up the state hashing system
        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);

        //set up the pfs
        List<PropositionalFunction> pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());

        // Create the agent and add it to the scaffolding tree
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        return scaff.addScaffoldingElementTo(parentStep, "Room_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 5, 20);
    }

    public Tree.Node createVerticalDomainAgent(Scaffolding scaff, Tree.Node parentStep) {
        //create the domain
        SokobanDomain gwdg = new SokobanDomain(11, 5) {
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
        };
        int[][] map = {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};
        gwdg.setMap(map);
        Domain domain = gwdg.generateDomain();

        //create the state parser
        StateParser sp = new SokobanStateParser(domain);

        //define the task
        RewardFunction rf = new SokobanRF(1, -1);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKISBLOCKED), -10);
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKATLOCATION), 1000);
        TerminalFunction tf = new SokobanTF(domain);
        StateConditionTest goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        State initialState = gwdg.getOneAgentNoLocationState(domain);
        gwdg.setAgent(initialState, 3, 5);

        //set up the state hashing system
        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);

        //set up the pfs
        List<PropositionalFunction> pfs = new ArrayList<PropositionalFunction>();
        pfs.add(domain.getPropFunction(SokobanDomain.PFWALLEAST));
        pfs.add(domain.getPropFunction(SokobanDomain.PFWALLWEST));

        // Create the agent and add it to the scaffolding tree
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        return scaff.addScaffoldingElementTo(parentStep, "Vertical_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 1, 10);
    }

    public Tree.Node createHorizontalDomainAgent(Scaffolding scaff, Tree.Node parentStep) {
        //create the domain
        SokobanDomain gwdg = new SokobanDomain(5, 11) {
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
        };

        int[][] map = {{1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1},
                {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}};


        gwdg.setMap(map);
        Domain domain = gwdg.generateDomain();

        //create the state parser
        StateParser sp = new SokobanStateParser(domain);

        //define the task
        RewardFunction rf = new UniformCostRF();
        TerminalFunction tf = new SokobanTF(domain);
        StateConditionTest goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        State initialState = gwdg.getOneAgentNoLocationState(domain);
        gwdg.setAgent(initialState, 5, 3);

        //set up the state hashing system
        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);

        //set up the pfs
        List<PropositionalFunction> pfs = new ArrayList<PropositionalFunction>();
        pfs.add(domain.getPropFunction(SokobanDomain.PFWALLNORTH));
        pfs.add(domain.getPropFunction(SokobanDomain.PFWALLSOUTH));

        // Create the agent and add it to the scaffolding tree
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        return scaff.addScaffoldingElementTo(parentStep, "Horizontal_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 1, 10);
    }

    private void scaffoldingExample(String outputPath) {
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath + "/";
        }
        Scaffolding scaff = new Scaffolding(outputPath);
        Doormax agent;

        // Add the root domain
        createFullDomain();
        agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        Tree.Node topStep = scaff.addScaffoldingElementTo(null, "Full_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 10, 200);

        // Add rock room sub-domain to root domain
        Tree.Node roomRockStep = createRoomRockDomainAgent(scaff, topStep);

        // Add room sub-domain to rock room sub-domain
        Tree.Node roomStep = createRoomDomainAgent(scaff, roomRockStep);

        // Add horizontal sub-domain to room sub-domain
        createHorizontalDomainAgent(scaff, roomStep);

        // Add vertical sub-domain to room sub-domain
        createVerticalDomainAgent(scaff, roomStep);


        /** LEARN **/
        // Learn on the sub-domains
        scaff.runSubTasks();

        // Run it
        scaff.runLearningEpisodeFrom(null);

    }


}
