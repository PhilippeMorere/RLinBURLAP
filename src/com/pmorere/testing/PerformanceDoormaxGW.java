package com.pmorere.testing;

import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.LearningAgentFactory;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import com.pmorere.modellearning.doormax.Doormax;
import com.pmorere.modellearning.grammar.ExpressionParser;
import com.pmorere.modellearning.grammar.GrammarLearner;
import com.pmorere.modellearning.grammar.GrammarParser;
import com.pmorere.modellearning.scaffolding.Scaffolding;
import com.pmorere.modellearning.scaffolding.Tree;

/**
 * Created by philippe on 10/03/15.
 */
public class PerformanceDoormaxGW extends TestingScaffoldingDoormaxGW {

    public static void main(String[] args) {
        PerformanceDoormaxGW perf = new PerformanceDoormaxGW();
        perf.createFullDomain();
        perf.experimenterAndPlotter();
    }

    private ExpressionParser setupGrammar(final GridWorldDomain gwdg) {
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
                else if (symbol.equals("EMPTY")) {
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

    public void experimenterAndPlotter() {

        //custom reward function for more interesting results
        final RewardFunction rf = new GoalBasedRF(this.goalCondition, 5., -0.1);

        /**
         * Create factories for Q-learning agent and SARSA agent to compare
         */

        LearningAgentFactory qLearningFactory = new LearningAgentFactory() {

            @Override
            public String getAgentName() {
                return "Q-learning";
            }

            @Override
            public LearningAgent generateAgent() {
                return new QLearning(domain, rf, tf, 0.99, hashingFactory, 0.3, 0.1);
            }
        };


        LearningAgentFactory sarsaLearningFactory = new LearningAgentFactory() {

            @Override
            public String getAgentName() {
                return "SARSA";
            }

            @Override
            public LearningAgent generateAgent() {
                return new SarsaLam(domain, rf, tf, 0.99, hashingFactory, 0.0, 0.1, 1.);
            }
        };

        LearningAgentFactory grammarLearnerFactory = new LearningAgentFactory() {

            @Override
            public String getAgentName() {
                return "GrammarLearner";
            }

            @Override
            public LearningAgent generateAgent() {
                // Set up the grammar
                GrammarParser gp = new GrammarParser();
                gp.addRule("Agent", "place");
                gp.addRule("EAST", "place", "place");
                gp.addRule("WEST", "place", "place");
                gp.addRule("NORTH", "place", "place");
                gp.addRule("SOUTH", "place", "place");
                gp.addRule("EMPTY", "place", GrammarParser.BOOLEAN);
                //gp.addRule("AND", new String[]{GrammarParser.BOOLEAN, GrammarParser.BOOLEAN}, GrammarParser.BOOLEAN);
                //gp.addRule("OR", new String[]{GrammarParser.BOOLEAN, GrammarParser.BOOLEAN}, GrammarParser.BOOLEAN);
                gp.addRule("NOT", GrammarParser.BOOLEAN, GrammarParser.BOOLEAN);


                return new GrammarLearner(domain, rf, tf, 0.99, hashingFactory, gp, setupGrammar(gwdg), 0);
            }
        };

        LearningAgentFactory doormaxFactory = new LearningAgentFactory() {

            @Override
            public String getAgentName() {
                return "Doormax";
            }

            @Override
            public LearningAgent generateAgent() {
                return new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
            }
        };

        LearningAgentFactory scaffoldingDoormaxFactory = new LearningAgentFactory() {

            @Override
            public String getAgentName() {
                return "Scaffolding Doormax";
            }

            @Override
            public LearningAgent generateAgent() {
                Scaffolding scaff = new Scaffolding("output/");
                Doormax agent;

                // Add the top node
                agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
                Tree.Node topStep = scaff.addScaffoldingElementTo(null, "Full_domain", domain, agent, initialState, new GridWorldStateParser(domain), gwdg.getMap(), 10, 200);

                // Add the top node
                Tree.Node roomStep = createRoomDomainAgent(scaff, topStep);

                // Add a subdomain to the top one
                createHorizontalDomainAgent(scaff, roomStep);

                // Add a subdomain to the top one
                createVerticalDomainAgent(scaff, roomStep);

                scaff.runSubTasks();
                return scaff;
            }
        };


        StateGenerator sg = new ConstantStateGenerator(this.initialState);

        LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter((SADomain) this.domain,
                rf, sg, 200, 40, doormaxFactory, grammarLearnerFactory);

        exp.setUpPlottingConfiguration(500, 250, 2, 1000,
                TrialMode.MOSTRECENTANDAVERAGE,
                PerformanceMetric.CUMULATIVESTEPSPEREPISODE,
                PerformanceMetric.AVERAGEEPISODEREWARD);

        exp.startExperiment();

        exp.writeStepAndEpisodeDataToCSV("expData");

    }
}
