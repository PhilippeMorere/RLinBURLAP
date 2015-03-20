package com.pmorere.testing;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.grammarLearner.GrammarBasedModel;
import com.pmorere.modellearning.grammarLearner.GrammarLearner;
import com.pmorere.modellearning.grammarLearner.grammar.ChunkGrammarParser;
import com.pmorere.modellearning.grammarLearner.grammar.ExpressionParser;
import com.pmorere.modellearning.grammarLearner.grammar.GrammarParser;
import com.pmorere.modellearning.grammarLearner.grammar.GrammarRule;
import com.pmorere.sokoban.*;

/**
 * Created by philippe on 12/03/15.
 */
public class TestingGrammarLearner {
    GridWorldDomain gwdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;
    GrammarParser gp;
    ExpressionParser ep;

    public TestingGrammarLearner() {
    }

    public static void main(String[] args) {

        TestingGrammarLearner example = new TestingGrammarLearner();
        //example.testOnGridWorld();
        example.testOnSokoban();
        String outputPath = "output/"; //directory to record results

        //we will call planning and learning algorithms here
        example.GLExample(outputPath);

        //run the visualizer
        //example.visualizeGridWorld(outputPath);
        example.visualizeSokoban(outputPath);

    }

    public void testOnGridWorld() {
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

        // Set up the grammar
        gp = new ChunkGrammarParser();
        gp.addRule("Agent", "place");
        gp.addRule("EAST", "place", "place");
        gp.addRule("WEST", "place", "place");
        gp.addRule("NORTH", "place", "place");
        gp.addRule("SOUTH", "place", "place");
        gp.addRule("WALL", "place", GrammarParser.BOOLEAN);
        gp.addLogic(GrammarRule.LOGIC_RULE_AND);
        gp.addLogic(GrammarRule.LOGIC_RULE_NOT);
        gp.addLogic(GrammarRule.LOGIC_RULE_OR);

        ((ChunkGrammarParser) gp).addChunck("WALL(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(SOUTH(Agent))");

        ep = new ExpressionParser("Agent") {

            int[][] map = gwdg.getMap();

            @Override
            public Object evaluateOperator(String symbol, Object[] args) {
                if (symbol.equals("WALL")) {
                    Pos pos = getXY((String) args[0]);
                    if (pos.x >= gwdg.getWidth() || pos.x < 0 || pos.y >= gwdg.getHeight() || pos.y < 0)
                        return true;
                    return map[pos.x][pos.y] != 0;
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
                return null;
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

    public void testOnSokoban() {
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


        // Set up the grammar
        gp = new ChunkGrammarParser();
        gp.addRule("Agent", "place");
        gp.addRule("EAST", "place", "place");
        gp.addRule("WEST", "place", "place");
        gp.addRule("NORTH", "place", "place");
        gp.addRule("SOUTH", "place", "place");
        gp.addRule("WALL", "place", GrammarParser.BOOLEAN);
        gp.addRule("GOAL", "place", GrammarParser.BOOLEAN);
        gp.addRule("ROCK", "place", GrammarParser.BOOLEAN);
        gp.addLogic(GrammarRule.LOGIC_RULE_AND);
        gp.addLogic(GrammarRule.LOGIC_RULE_NOT);
        gp.addLogic(GrammarRule.LOGIC_RULE_OR);

        ((ChunkGrammarParser) gp).addChunck("WALL(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("WALL(SOUTH(Agent))");

        ((ChunkGrammarParser) gp).addChunck("ROCK(EAST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("ROCK(WEST(Agent))");
        ((ChunkGrammarParser) gp).addChunck("ROCK(NORTH(Agent))");
        ((ChunkGrammarParser) gp).addChunck("ROCK(SOUTH(Agent))");

        ((ChunkGrammarParser) gp).addChunck("WALL(EAST(EAST(Agent)))");
        ((ChunkGrammarParser) gp).addChunck("WALL(WEST(WEST(Agent)))");
        ((ChunkGrammarParser) gp).addChunck("WALL(NORTH(NORTH(Agent)))");
        ((ChunkGrammarParser) gp).addChunck("WALL(SOUTH(SOUTH(Agent)))");

        ((ChunkGrammarParser) gp).addChunck("GOAL(EAST(EAST(Agent)))");
        ((ChunkGrammarParser) gp).addChunck("GOAL(WEST(WEST(Agent)))");
        ((ChunkGrammarParser) gp).addChunck("GOAL(NORTH(NORTH(Agent)))");
        ((ChunkGrammarParser) gp).addChunck("GOAL(SOUTH(SOUTH(Agent)))");

        ep = new ExpressionParser("Agent") {

            int[][] map = gwdg.getMap();

            @Override
            public Object evaluateOperator(String symbol, Object[] args) {
                if (symbol.equals("WALL")) {
                    Pos pos = getXY((String) args[0]);
                    if (pos.x >= gwdg.getWidth() || pos.x < 0 || pos.y >= gwdg.getHeight() || pos.y < 0)
                        return true;
                    return map[pos.x][pos.y] != 0;
                } else if (symbol.equals("GOAL")) {
                    Pos pos = getXY((String) args[0]);
                    ObjectInstance goal = sh.s.getFirstObjectOfClass(SokobanDomain.CLASSLOCATION);
                    return goal.getDiscValForAttribute(SokobanDomain.ATTX) == pos.x &&
                            goal.getDiscValForAttribute(SokobanDomain.ATTY) == pos.y;
                } else if (symbol.equals("ROCK")) {
                    Pos pos = getXY((String) args[0]);
                    ObjectInstance rock = sh.s.getFirstObjectOfClass(SokobanDomain.CLASSROCK);
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
                return null;
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

    public void GLExample(String outputPath) {
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath + "/";
        }
        //discount= 0.99; initialQ=0.0; learning rate=0.5; lambda=1.0
        LearningAgent agent = new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, ep, 1);

        //run learning for 1000 episodes
        int maxTimeSteps = 200;
        for (int i = 0; i < 50; i++) {
            EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, maxTimeSteps);
            //if(ea.numTimeSteps() < maxTimeSteps)
            ea.writeToFile(String.format("%se%03d", outputPath, i), sp);
            double sum = 0;
            for (int j = 1; j < ea.numTimeSteps(); j++)
                sum += ea.getReward(j);
            System.out.println(i + ": Total reward " + sum + " in " +
                    ea.numTimeSteps() + " steps.");
        }


        // Evaluate optimal policy with this model
        Model model = ((GrammarLearner) agent).getModel();
        ((GrammarBasedModel) model).printRules();
        //ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, model, false);
        //OOMDPPlanner planner = new ValueIteration(mdg.generateDomain(), rf, model.getModelTF(), 0.99, hashingFactory, 0.001, 10);
        //planner.planFromState(initialState);

        //create a Q-greedy policy from the planner
        //Policy p = new GreedyQPolicy((QComputablePlanner) planner);

        //record the plan results to a file
        //p.evaluateBehavior(initialState, rf, tf, maxTimeSteps).writeToFile(outputPath + "planResult", sp);
        System.out.println("Done");
    }

    public void visualizeGridWorld(String outputPath) {
        Visualizer v = GridWorldVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }

    public void visualizeSokoban(String outputPath) {
        Visualizer v = SokobanVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }
}

