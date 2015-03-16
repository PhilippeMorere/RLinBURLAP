package com.pmorere.testing;

import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.LearningAgentFactory;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import com.pmorere.modellearning.doormax.Doormax;
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

        LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter((SADomain)this.domain,
                rf, sg, 100, 40, doormaxFactory, scaffoldingDoormaxFactory);

        exp.setUpPlottingConfiguration(500, 250, 2, 1000,
                TrialMode.MOSTRECENTANDAVERAGE,
                PerformanceMetric.CUMULATIVESTEPSPEREPISODE,
                PerformanceMetric.AVERAGEEPISODEREWARD);

        exp.startExperiment();

        exp.writeStepAndEpisodeDataToCSV("expData");

    }
}
