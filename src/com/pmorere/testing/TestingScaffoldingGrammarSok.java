package com.pmorere.testing;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.grammarLearner.grammar.ChunkGrammarParser;
import com.pmorere.modellearning.grammarLearner.grammar.ExpressionParser;
import com.pmorere.modellearning.grammarLearner.GrammarLearner;
import com.pmorere.modellearning.grammarLearner.grammar.GrammarParser;
import com.pmorere.modellearning.grammarLearner.grammar.GrammarRule;
import com.pmorere.modellearning.ModelLearner;
import com.pmorere.modellearning.scaffolding.Scaffolding;
import com.pmorere.modellearning.scaffolding.Tree;
import com.pmorere.sokoban.*;

import java.util.List;

/**
 * Created by philippe on 16/03/15.
 */
public class TestingScaffoldingGrammarSok {
    SokobanDomain gwdg;
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

    public TestingScaffoldingGrammarSok() {
        // Set up the grammar
        gp = new ChunkGrammarParser();
        gp.addRule("Agent", "place");
        gp.addRule("EAST", "place", "place");
        gp.addRule("WEST", "place", "place");
        gp.addRule("NORTH", "place", "place");
        gp.addRule("SOUTH", "place", "place");
        gp.addRule("EMPTY", "place", GrammarParser.BOOLEAN);
        gp.addRule("WALL", "place", GrammarParser.BOOLEAN);
        gp.addRule("GOAL", "place", GrammarParser.BOOLEAN);
        gp.addRule("ROCK", "place", GrammarParser.BOOLEAN);
        gp.addLogic(GrammarRule.LOGIC_RULE_AND);
        gp.addLogic(GrammarRule.LOGIC_RULE_NOT);
        gp.addLogic(GrammarRule.LOGIC_RULE_OR);

        ((ChunkGrammarParser) gp).addChunck("EMPTY(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("EMPTY(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("EMPTY(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("EMPTY(SOUTH(Agent))");

        ((ChunkGrammarParser) gp).addChunck("WALL(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(SOUTH(Agent))");

        ((ChunkGrammarParser) gp).addChunck("ROCK(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("ROCK(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("ROCK(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("ROCK(SOUTH(Agent))");

        ((ChunkGrammarParser) gp).addChunck("GOAL(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("GOAL(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("GOAL(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("GOAL(SOUTH(Agent))");
    }

    private ExpressionParser setupGrammar(final SokobanDomain gwdg) {
        return new ExpressionParser("Agent") {

            int[][] map = gwdg.getMap();

            @Override
            public Object evaluateOperator(String symbol, Object[] args) {
                if (symbol.equals("AND"))
                    return (Boolean) args[0] && (Boolean) args[1];
                else if (symbol.equals("OR"))
                    return (Boolean) args[0] || (Boolean) args[1];
                else if (symbol.equals("NOT"))
                    return !(Boolean) args[0];
                else if (symbol.equals("WALL")) {
                    Pos pos = getXY((String) args[0]);
                    if (pos.x >= gwdg.getWidth() || pos.x < 0 || pos.y >= gwdg.getHeight() || pos.y < 0)
                        return true;
                    return map[pos.x][pos.y] != 0;
                } else if (symbol.equals("EMPTY")) {
                    Pos pos = getXY((String) args[0]);
                    if (pos.x >= gwdg.getWidth() || pos.x < 0 || pos.y >= gwdg.getHeight() || pos.y < 0)
                        return false;
                    return map[pos.x][pos.y] == 0;
                }else if (symbol.equals("GOAL")) {
                    Pos pos = getXY((String) args[0]);
                    ObjectInstance goal = sh.s.getFirstObjectOfClass(SokobanDomain.CLASSLOCATION);
                    if (goal == null)
                        return false;
                    return goal.getDiscValForAttribute(SokobanDomain.ATTX) == pos.x &&
                            goal.getDiscValForAttribute(SokobanDomain.ATTY) == pos.y;
                } else if (symbol.equals("ROCK")) {
                    Pos pos = getXY((String) args[0]);
                    ObjectInstance rock = sh.s.getFirstObjectOfClass(SokobanDomain.CLASSROCK);
                    if (rock == null)
                        return false;
                    return rock.getDiscValForAttribute(SokobanDomain.ATTX) == pos.x &&
                            rock.getDiscValForAttribute(SokobanDomain.ATTY) == pos.y;
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
                    ObjectInstance agent = sh.s.getObjectsOfTrueClass(SokobanDomain.CLASSAGENT).get(0);
                    x = agent.getDiscValForAttribute(SokobanDomain.ATTX);
                    y = agent.getDiscValForAttribute(SokobanDomain.ATTY);
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

        TestingScaffoldingGrammarSok example = new TestingScaffoldingGrammarSok();
        //example.testOnSokoban();
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

        ep = setupGrammar(gwdg);
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
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSROCK,
                domain.getObjectClass(SokobanDomain.CLASSROCK).attributeList);

        // Create the agent and add it to the scaffolding tree
        ModelLearner agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
        return scaff.addScaffoldingElementTo(parentStep, "Rock_room_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 30, 20);

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
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSROCK,
                domain.getObjectClass(SokobanDomain.CLASSROCK).attributeList);

        // Create the agent and add it to the scaffolding tree
        ModelLearner agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
        return scaff.addScaffoldingElementTo(parentStep, "Room_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 20, 20);
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
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSROCK,
                domain.getObjectClass(SokobanDomain.CLASSROCK).attributeList);

        // Create the agent and add it to the scaffolding tree
        ModelLearner agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
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
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSROCK,
                domain.getObjectClass(SokobanDomain.CLASSROCK).attributeList);

        // Create the agent and add it to the scaffolding tree
        ModelLearner agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
        return scaff.addScaffoldingElementTo(parentStep, "Horizontal_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 1, 10);
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
        Tree.Node topStep = scaff.addScaffoldingElementTo(null, "Full_domain", domain, agent, initialState, new SokobanStateParser(domain), gwdg.getMap(), 10, 200);

        // Add rock room sub-domain to root domain
        Tree.Node roomRockStep = createRoomRockDomainAgent(scaff, topStep);

        // Add room sub-domain to rock room sub-domain
        Tree.Node roomStep = createRoomDomainAgent(scaff, roomRockStep);

        // Add horizontal sub-domain to room sub-domain
        createHorizontalDomainAgent(scaff, roomStep);

        // Add vertical sub-domain to room sub-domain
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
    }


    public void visualizeGridWorld(String outputPath) {
        Visualizer v = SokobanVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }
}
