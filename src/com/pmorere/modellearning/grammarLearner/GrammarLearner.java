package com.pmorere.modellearning.grammarLearner;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.rtdp.BoundedRTDP;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import com.pmorere.modellearning.grammarLearner.grammar.ExpressionParser;
import com.pmorere.modellearning.grammarLearner.grammar.GrammarParser;
import com.pmorere.modellearning.ModelLearner;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by philippe on 12/03/15.
 */
public class GrammarLearner extends OOMDPPlanner implements LearningAgent, QComputablePlanner, ModelLearner {

    /**
     * The model of the world that is being learned.
     */
    protected Model model;

    /**
     * The modeled domain object containing the modeled actions that a planner will use.
     */
    protected Domain modeledDomain;

    protected ValueFunctionPlanner planner;

    protected Policy policy;


    /**
     * the saved previous learning episodes
     */
    protected LinkedList<EpisodeAnalysis> episodeHistory = new LinkedList<EpisodeAnalysis>();

    /**
     * The number of the most recent learning episodes to store.
     */
    protected int numEpisodesToStore = 1;

    public GrammarLearner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma,
                          StateHashFactory hashingFactory, GrammarParser grammarParser,
                          ExpressionParser expressionParser, double rmax) {
        this.plannerInit(domain, rf, tf, gamma, hashingFactory);
        this.model = new ScaffoldingGrammarBasedModel(domain, hashingFactory, grammarParser, expressionParser);

        ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, this.model, true);
        this.modeledDomain = mdg.generateDomain();

        this.planner = new GLPlanner(this.modeledDomain, rf, this.model.getModelTF(), gamma, hashingFactory, rmax - 10, rmax, 0.1, 10);

        this.policy = new GreedyQPolicy(this);
    }

    @Override
    public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
        return runLearningEpisodeFrom(initialState, -1);
    }

    @Override
    public EpisodeAnalysis runLearningEpisodeFrom(State initialState, int maxSteps) {
        EpisodeAnalysis ea = new EpisodeAnalysis(initialState);
        int steps = 0;
        while (!this.tf.isTerminal(initialState) && (steps < maxSteps || maxSteps == -1)) {
            GroundedAction ga = (GroundedAction) policy.getAction(initialState);
            State nextState = ga.executeIn(initialState);
            double r = this.rf.reward(initialState, ga, nextState);
            ea.recordTransitionTo(ga, nextState, r);

            this.model.updateModel(initialState, ga, nextState, r, this.tf.isTerminal(nextState));
            this.planner.performBellmanUpdateOn(initialState);

            initialState = nextState;
            steps++;
        }

        if (episodeHistory.size() >= numEpisodesToStore)
            episodeHistory.poll();

        episodeHistory.offer(ea);
        return ea;
    }


    @Override
    public EpisodeAnalysis getLastLearningEpisode() {
        return episodeHistory.getLast();
    }

    @Override
    public void setNumEpisodesToStore(int numEps) {
        if (numEps > 0) {
            numEpisodesToStore = numEps;
        } else {
            numEpisodesToStore = 1;
        }
    }

    @Override
    public List<EpisodeAnalysis> getAllStoredLearningEpisodes() {
        return episodeHistory;
    }

    @Override
    public void planFromState(State initialState) {
        throw new RuntimeException("Model learning algorithms should not be used as planning algorithms.");
    }

    @Override
    public void resetPlannerResults() {
        this.model.resetModel();
        this.planner.resetPlannerResults();
        this.episodeHistory.clear();
    }

    public List<QValue> getQs(State s) {
        List<QValue> qs = this.planner.getQs(s);
        for (QValue q : qs) {

            //if Q for unknown action, use value initialization of curent state
            if (!this.model.transitionIsModeled(s, (GroundedAction) q.a)) {
                q.q = this.planner.getValueFunctionInitialization().qValue(s, q.a);
            }

            //update action to real world action
            Action realWorldAction = this.domain.getAction(q.a.actionName());
            GroundedAction nga = new GroundedAction(realWorldAction, q.a.params);
            q.a = nga;

        }
        return qs;
    }

    @Override
    public QValue getQ(State s, AbstractGroundedAction a) {

        QValue q = this.planner.getQ(s, a);

        //if Q for unknown action, use value initialization of curent state
        if (!this.model.transitionIsModeled(s, (GroundedAction) q.a)) {
            q.q = this.planner.getValueFunctionInitialization().qValue(s, q.a);
        }

        //update action to real world action
        Action realWorldAction = this.domain.getAction(q.a.actionName());
        GroundedAction nga = new GroundedAction(realWorldAction, q.a.params);
        q.a = nga;
        return q;
    }

    public Model getModel() {
        return model;
    }

    @Override
    public void printModel() {
        ((GrammarBasedModel) this.model).printRules();
    }

    private class GLPlanner extends BoundedRTDP {

        /**
         * Initializes
         *
         * @param domain         the modeled domain
         * @param rf             the modeled reward function
         * @param tf             the modeled terminal function
         * @param gamma          the discount factor
         * @param hashingFactory the hashing factory
         */
        public GLPlanner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory,
                         double lowerVInit, double upperVInit, double maxDiff, int maxRollouts) {
            this(domain, rf, tf, gamma, hashingFactory, new ValueFunctionInitialization.ConstantValueFunctionInitialization(lowerVInit),
                    new ValueFunctionInitialization.ConstantValueFunctionInitialization(upperVInit), maxDiff, maxRollouts);

        }

        /**
         * Initializes
         *
         * @param domain         the modeled domain
         * @param rf             the modeled reward function
         * @param tf             the modeled terminal function
         * @param gamma          the discount factor
         * @param hashingFactory the hashing factory
         */
        public GLPlanner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory,
                         ValueFunctionInitialization lowerVInit, ValueFunctionInitialization upperVInit, double maxDiff, int maxRollouts) {
            super(domain, rf, tf, gamma, hashingFactory, lowerVInit, upperVInit, maxDiff, maxRollouts);

            //don't cache transition dynamics because our leanred model keeps changing!
            this.useCachedTransitions = false;
        }

        @Override
        public void planFromState(State initialState) {
            throw new UnsupportedOperationException("This method should not be called for the inner ARTDP planner");
        }
    }
}
