package com.pmorere.modellearning.doormax;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.DomainMappedPolicy;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.ModelPlanner;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.learning.modellearning.rmax.UnmodeledFavoredPolicy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.rtdp.BoundedRTDP;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import com.pmorere.modellearning.scaffolding.ModelLearner;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by philippe on 19/02/15.
 */
public class Doormax extends OOMDPPlanner implements LearningAgent, QComputablePlanner, ModelLearner {

    /**
     * The model of the world that is being learned.
     */
    protected Model model;

    /**
     * The modeled domain object containing the modeled actions that a planner will use.
     */
    protected Domain modeledDomain;
    /**
     * The model-adaptive planning algorithm to use
     */
    protected ModelPlanner modelPlanner;
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

    public Doormax(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory,
                   double maxVIDelta, int maxVIPasses, State initState, List<PropositionalFunction> pfs, double rmax) {
        this.plannerInit(domain, rf, tf, gamma, hashingFactory);
        this.model = new ScaffoldingDormaxModel(domain, initState, pfs, rmax);

        ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, this.model, true);
        this.modeledDomain = mdg.generateDomain();

        //this.modelPlanner = new VIModelPlanner(modeledDomain, model.getModelRF(), model.getModelTF(), gamma, hashingFactory, maxVIDelta, maxVIPasses);
        //this.planner = new BoundedRTDP(modeledDomain, model.getModelRF(), model.getModelTF(), gamma, hashingFactory,
        //        new ValueFunctionInitialization.ConstantValueFunctionInitialization(-10.0), new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.0), 0.1, 30);

        this.planner = new DoormaxPlanner(this.modeledDomain, rf, this.model.getModelTF(), gamma, hashingFactory, rmax - 10, rmax, 0.1, 10);

        //this.policy = new BoltzmannQPolicy(this, 0.1);
        this.policy = new GreedyQPolicy(this);
    }

    public void printModel() {
        ((DoormaxModel) model).displayFailureConditions();
        ((DoormaxModel) model).displayTermNames();
        ((DoormaxModel) model).displayPredictions();
    }


    public void saveModelRules(String path) {
        ((DoormaxModel) model).saveModelRules(path);
    }


    public void loadModelRules(Domain domain, String filePath) {
        ((DoormaxModel) model).loadModelRules(domain, filePath);
        System.out.println("Loaded rules:");
        printModel();
    }

    /**
     * Returns the model learning algorithm being used.
     *
     * @return the model learning algorithm being used.
     */
    public Model getModel() {
        return model;
    }


    /**
     * Returns the model domain for planning. This model domain may differ from the real domain in the actions it uses for planning.
     *
     * @return the model domain for planning
     */
    public Domain getModeledDomain() {
        return modeledDomain;
    }


    @Override
    public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
        return this.runLearningEpisodeFrom(initialState, -1);
    }

    @Override
    public EpisodeAnalysis runLearningEpisodeFrom(State initialState, int maxSteps) {
        //this.modelPlanner.initializePlannerIn(initialState);
        //this.planner.planFromState(initialState);

        EpisodeAnalysis ea = new EpisodeAnalysis(initialState);

        //DomainMappedPolicy policy = new DomainMappedPolicy(domain, this.modelPlanner.modelPlannedPolicy());
        //Policy policy = this.createDomainMappedPolicy();

        GroundedAction lastAction = null;
        GroundedAction lastAction2 = null;
        GroundedAction lastAction3 = null;
        GroundedAction lastAction4 = null;
        GroundedAction lastAction5 = null;
        GroundedAction lastAction6 = null;
        State curState = initialState;
        int steps = 0;
        while (!this.tf.isTerminal(curState) && (steps < maxSteps || maxSteps == -1)) {
            GroundedAction ga = (GroundedAction) policy.getAction(curState);
            State nextState = ga.executeIn(curState);
            double r = this.rf.reward(curState, ga, nextState);
            ea.recordTransitionTo(ga, nextState, r);

            this.model.updateModel(curState, ga, nextState, r, this.tf.isTerminal(nextState));
            this.planner.performBellmanUpdateOn(curState);

            curState = nextState;
            steps++;
        }

        if (episodeHistory.size() >= numEpisodesToStore)
            episodeHistory.poll();

        episodeHistory.offer(ea);
        return ea;
    }

    protected Policy createDomainMappedPolicy() {
        return new DomainMappedPolicy(domain, new UnmodeledFavoredPolicy(
                this.modelPlanner.modelPlannedPolicy(),
                this.model,
                this.modeledDomain.getActions()));
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
        this.modelPlanner.resetPlanner();
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

    public Policy getPolicy() {
        return this.policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    protected class DoormaxPlanner extends BoundedRTDP {

        /**
         * Initializes
         *
         * @param domain         the modeled domain
         * @param rf             the modeled reward function
         * @param tf             the modeled terminal function
         * @param gamma          the discount factor
         * @param hashingFactory the hashing factory
         */
        public DoormaxPlanner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory,
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
        public DoormaxPlanner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory,
                              ValueFunctionInitialization lowerVInit, ValueFunctionInitialization upperVInit, double maxDiff, int maxRollouts) {
            super(domain, rf, tf, gamma, hashingFactory, lowerVInit, upperVInit, maxDiff, maxRollouts);
            //VFPInit(domain, rf, tf, gamma, hashingFactory);

            //don't cache transition dynamics because our leanred model keeps changing!
            this.useCachedTransitions = false;

            //this.valueInitializer = vInit;
        }

        @Override
        public void planFromState(State initialState) {
            throw new UnsupportedOperationException("This method should not be called for the inner ARTDP planner");
        }

    }
}
