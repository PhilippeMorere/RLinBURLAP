package com.pmorere.testing;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.grammarLearner.grammar.ChunkGrammarParser;
import com.pmorere.modellearning.grammarLearner.grammar.ExpressionParser;
import com.pmorere.modellearning.grammarLearner.GrammarLearner;
import com.pmorere.modellearning.grammarLearner.grammar.GrammarParser;
import com.pmorere.modellearning.ModelLearner;
import com.pmorere.modellearning.grammarLearner.grammar.GrammarRule;
import com.pmorere.modellearning.scaffolding.Scaffolding;
import com.pmorere.modellearning.scaffolding.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 16/03/15.
 */
public class TestingScaffoldingGrammarGW {
    GridWorldDomain gwdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;
    List<PropositionalFunction> pfs;
    GrammarParser gp;
    ExpressionParser ep;

    public TestingScaffoldingGrammarGW() {
        // Set up the grammar
        gp = new ChunkGrammarParser();
        gp.addRule("Agent", "place");
        gp.addRule("EAST", "place", "place");
        gp.addRule("WEST", "place", "place");
        gp.addRule("NORTH", "place", "place");
        gp.addRule("SOUTH", "place", "place");
        gp.addRule("EMPTY", "place", GrammarParser.BOOLEAN);
        gp.addLogic(GrammarRule.LOGIC_RULE_AND);
        gp.addLogic(GrammarRule.LOGIC_RULE_NOT);
        gp.addLogic(GrammarRule.LOGIC_RULE_OR);

        ((ChunkGrammarParser) gp).addChunck("EMPTY(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("EMPTY(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("EMPTY(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("EMPTY(SOUTH(Agent))");
    }

    private ExpressionParser setupGrammar(final GridWorldDomain gwdg) {
        return new ExpressionParser("Agent") {

            int[][] map = gwdg.getMap();

            @Override
            public Object evaluateOperator(String symbol, Object[] args) {
                if (symbol.equals("EMPTY")) {
                    Pos pos = getXY((String) args[0]);
                    if (pos.x >= gwdg.getWidth() || pos.x < 0 || pos.y >= gwdg.getHeight() || pos.y < 0)
                        return false;
                    return map[pos.x][pos.y] == 0;
                } else if (symbol.equals("EAST")) {
                    Pos pos = getXY((String) args[0]);
                    return (pos.x + 1) + "," + pos.y;
                } else if (symbol.equals("WEST")) {
                    Pos pos = getXY((String) args[0]);
                    return (pos.x - 1) + "," + pos.y;
                } else if (symbol.equals("SOUTH")) {
                    Pos pos = getXY((String) args[0]);
                    return pos.x + "," + (pos.y - 1);
                } else if (symbol.equals("NORTH")) {
                    Pos pos = getXY((String) args[0]);
                    return pos.x + "," + (pos.y + 1);
                }
                throw new RuntimeException("Unsupported symbol " + symbol);
            }

            private Pos getXY(String arg) {
                int x, y;
                if (arg.equals("Agent")) {
                    ObjectInstance agent = sh.s.getObjectsOfTrueClass(GridWorldDomain.CLASSAGENT).get(0);
                    x = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
                    y = agent.getDiscValForAttribute(GridWorldDomain.ATTY);
                } else {
                    String[] coord = arg.split(",");
                    x = Integer.valueOf(coord[0]);
                    y = Integer.valueOf(coord[1]);
                }
                return new Pos(x, y);
            }

            class Pos {
                public int x, y;

                public Pos(int x, int y) {
                    this.x = x;
                    this.y = y;
                }
            }
        };
    }

    public static void main(String[] args) {

        TestingScaffoldingGrammarGW example = new TestingScaffoldingGrammarGW();
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

        ep = setupGrammar(gwdg);
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
        ModelLearner agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
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
        ModelLearner agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
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
        ModelLearner agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
        return scaff.addScaffoldingElementTo(parentStep, "Horizontal_domain", domain, agent, initialState, new GridWorldStateParser(domain), gwdg.getMap(), 1, 10);
    }


    private void scaffoldingExample(String outputPath) {
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath + "/";
        }
        Scaffolding scaff = new Scaffolding(outputPath);
        ModelLearner agent;

        // Add the top node
        createFullDomain();
        agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, ep, 0);
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
        int episodeNb = 0;
        System.out.println("Starting step 4 rooms");
        while (episodeNb++ < 10) {
            System.out.println("episode " + episodeNb);
            EpisodeAnalysis ea = scaff.runLearningEpisodeFrom(null, 200);

            // Save to file
            ea.writeToFile(String.format("%se%03d", outputPath, episodeNb), sp);
        }
        agent.printModel();
        visualizeGridWorld(outputPath);

        throw new RuntimeException("Wrong implrementation of scaffolding with grammar. Think about it...");
    }


    public void visualizeGridWorld(String outputPath) {
        Visualizer v = GridWorldVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }
}
