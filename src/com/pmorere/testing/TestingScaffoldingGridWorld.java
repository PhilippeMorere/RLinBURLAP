package com.pmorere.testing;

import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import com.pmorere.modellearning.doormax.Doormax;
import com.pmorere.modellearning.scaffolding.Scaffolding;
import com.pmorere.modellearning.scaffolding.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 09/03/15.
 */
public class TestingScaffoldingGridWorld {
    GridWorldDomain gwdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;
    List<PropositionalFunction> pfs;

    public TestingScaffoldingGridWorld() {
    }

    public static void main(String[] args) {

        TestingScaffoldingGridWorld example = new TestingScaffoldingGridWorld();
        //example.testOnSokoban();
        String outputPath = "output/"; //directory to record results

        //we will call planning and learning algorithms here
        example.scaffoldingExample(outputPath);

    }


    public void createFullDomain() {
        //create the domain
        gwdg = new GridWorldDomain(11, 11);
        gwdg.setMapToFourRooms();
        domain = gwdg.generateDomain();

        //create the state parser
        sp = new GridWorldStateParser(domain);

        //define the task
        rf = new UniformCostRF();

        tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
        goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        initialState = gwdg.getOneAgentOneLocationState(domain);
        gwdg.setAgent(initialState, 0, 0);
        gwdg.setLocation(initialState, 0, 10, 10);

        //set up the state hashing system
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
                domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

        //set up the pfs
        pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());
    }

    public Tree.Node createRoomDomainAgent(Scaffolding scaff, Tree.Node parentStep) {
        //create the domain
        GridWorldDomain gwdg = new GridWorldDomain(6, 6);
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
        StateParser sp = new GridWorldStateParser(domain);

        //define the task
        RewardFunction rf = new UniformCostRF();

        TerminalFunction tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
        StateConditionTest goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        State initialState = gwdg.getOneAgentNoLocationState(domain);
        gwdg.setAgent(initialState, 3, 3);

        //set up the state hashing system
        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
                domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

        //set up the pfs
        List<PropositionalFunction> pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());

        // Create the agent and add it to the scaffolding tree
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        return scaff.addScaffoldingElementTo(parentStep, "Room_domain", domain, agent, initialState, new GridWorldStateParser(domain), gwdg.getMap(), 20, 20);
    }

    public Tree.Node createVerticalDomainAgent(Scaffolding scaff, Tree.Node parentStep) {
        //create the domain
        GridWorldDomain gwdg = new GridWorldDomain(11, 5) {
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


                new AtLocationPF(PFATLOCATION, domain, new String[]{CLASSAGENT, CLASSLOCATION});

                new WallToPF(PFWALLNORTH, domain, new String[]{CLASSAGENT}, 0);
                new WallToPF(PFWALLSOUTH, domain, new String[]{CLASSAGENT}, 1);
                new WallToPF(PFWALLEAST, domain, new String[]{CLASSAGENT}, 2);
                new WallToPF(PFWALLWEST, domain, new String[]{CLASSAGENT}, 3);

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
        StateParser sp = new GridWorldStateParser(domain);

        //define the task
        RewardFunction rf = new UniformCostRF();
        TerminalFunction tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
        StateConditionTest goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        State initialState = gwdg.getOneAgentNoLocationState(domain);
        gwdg.setAgent(initialState, 3, 5);

        //set up the state hashing system
        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
                domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

        //set up the pfs
        List<PropositionalFunction> pfs = new ArrayList<PropositionalFunction>();
        pfs.add(domain.getPropFunction(GridWorldDomain.PFWALLEAST));
        pfs.add(domain.getPropFunction(GridWorldDomain.PFWALLWEST));

        // Create the agent and add it to the scaffolding tree
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        return scaff.addScaffoldingElementTo(parentStep, "Vertical_domain", domain, agent, initialState, new GridWorldStateParser(domain), gwdg.getMap(), 1, 10);
    }

    public Tree.Node createHorizontalDomainAgent(Scaffolding scaff, Tree.Node parentStep) {
        //create the domain
        GridWorldDomain gwdg = new GridWorldDomain(5, 11) {
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

                new AtLocationPF(PFATLOCATION, domain, new String[]{CLASSAGENT, CLASSLOCATION});

                new WallToPF(PFWALLNORTH, domain, new String[]{CLASSAGENT}, 0);
                new WallToPF(PFWALLSOUTH, domain, new String[]{CLASSAGENT}, 1);
                new WallToPF(PFWALLEAST, domain, new String[]{CLASSAGENT}, 2);
                new WallToPF(PFWALLWEST, domain, new String[]{CLASSAGENT}, 3);

                return domain;
            }
        };

        int[][] map = {{1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1},
                {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}};


        gwdg.setMap(map);
        Domain domain = gwdg.generateDomain();

        //create the state parser
        StateParser sp = new GridWorldStateParser(domain);

        //define the task
        RewardFunction rf = new UniformCostRF();
        TerminalFunction tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
        StateConditionTest goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the tasks
        State initialState = gwdg.getOneAgentNoLocationState(domain);
        gwdg.setAgent(initialState, 5, 3);

        //set up the state hashing system
        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
                domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

        //set up the pfs
        List<PropositionalFunction> pfs = new ArrayList<PropositionalFunction>();
        pfs.add(domain.getPropFunction(GridWorldDomain.PFWALLNORTH));
        pfs.add(domain.getPropFunction(GridWorldDomain.PFWALLSOUTH));

        // Create the agent and add it to the scaffolding tree
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        return scaff.addScaffoldingElementTo(parentStep, "Horizontal_domain", domain, agent, initialState, new GridWorldStateParser(domain), gwdg.getMap(), 1, 10);
    }


    private void scaffoldingExample(String outputPath) {
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath + "/";
        }
        Scaffolding scaff = new Scaffolding(outputPath);
        Doormax agent;

        // Add the top node
        createFullDomain();
        agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        Tree.Node topStep = scaff.addScaffoldingElementTo(null, "Full_domain", domain, agent, initialState, new GridWorldStateParser(domain), gwdg.getMap(), 10, 200);

        // Add the top node
        Tree.Node roomStep = createRoomDomainAgent(scaff, topStep);
        // Add a subdomain to the top one
        createHorizontalDomainAgent(scaff, roomStep);
        // Add a subdomain to the top one
        createVerticalDomainAgent(scaff, roomStep);

        // Learn the subtasks
        scaff.runSubTasks();

        // Run it
        scaff.runLearningEpisodeFrom(null);

    }
}
