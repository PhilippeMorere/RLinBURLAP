package com.pmorere.testing;

import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.learning.GoalBasedRF;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.LearningAgentFactory;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import com.pmorere.modellearning.doormax.Doormax;
import com.pmorere.modellearning.scaffolding.Scaffolding;
import com.pmorere.modellearning.scaffolding.Tree;
import com.pmorere.sokoban.SokobanStateParser;

/**
 * Created by philippe on 10/03/15.
 */
public class PerformanceDoormaxSokoban extends TestingScaffoldingSokoban {

    public static void main(String[] args) {
        PerformanceDoormaxSokoban perf = new PerformanceDoormaxSokoban();
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

                // Add the root domain
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


                scaff.runSubTasks();
                return scaff;
            }
        };


        StateGenerator sg = new ConstantStateGenerator(this.initialState);

        LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter((SADomain) this.domain,
                rf, sg, 50, 100, doormaxFactory, scaffoldingDoormaxFactory, qLearningFactory);

        exp.setUpPlottingConfiguration(600, 350, 2, 1200,
                TrialMode.MOSTRECENTANDAVERAGE,
                PerformanceMetric.CUMULATIVESTEPSPEREPISODE,
                PerformanceMetric.AVERAGEEPISODEREWARD);

        exp.startExperiment();

        exp.writeStepAndEpisodeDataToCSV("expData");

    }

    protected class CustomExperimenter extends LearningAlgorithmExperimenter{


        private final int maxStepsPerEpisode;

        /**
         * Initializes.
         * The trialLength will be interpreted as the number of episodes, but it can be reinterpreted as a total number of steps per trial using the
         * {@link #toggleTrialLengthInterpretation(boolean)}.
         *
         * @param domain         the domain in which agents will be tested
         * @param rf             the reward function used to measure performance
         * @param sg             the state generated used to generate states at the beginning of each episode
         * @param nTrials        the number of trials
         * @param trialLength    the length of the trials (by default in episodes, but can be intereted as maximum step length)
         * @param agentFactories factories to generate the agents to be tested.
         */
        public CustomExperimenter(SADomain domain, RewardFunction rf, StateGenerator sg, int nTrials, int trialLength,
                                  int maxStepsPerEpisode, LearningAgentFactory... agentFactories) {
            super(domain, rf, sg, nTrials, trialLength, agentFactories);
            this.maxStepsPerEpisode = maxStepsPerEpisode;
        }

        /**
         * Runs a trial for an agent generated by the given factory when interpreting trial length as a number of episodes.
         * @param agentFactory the agent factory used to generate the agent to test.
         */
        @Override
        protected void runEpisodeBoundTrial(LearningAgentFactory agentFactory){

            //temporarily disable plotter data collection to avoid possible contamination for any actions taken by the agent generation
            //(e.g., if there is pre-test training)
            this.plotter.toggleDataCollection(false);

            LearningAgent agent = agentFactory.generateAgent();

            this.plotter.toggleDataCollection(true); //turn it back on to begin

            this.plotter.startNewTrial();

            for(int i = 0; i < this.trialLength; i++){
                agent.runLearningEpisodeFrom(sg.generateState(), this.maxStepsPerEpisode);
                this.plotter.endEpisode();
            }

            this.plotter.endTrial();

        }
    }
}
