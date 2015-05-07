package com.pmorere.testing;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.frostbite.*;
import com.pmorere.modellearning.doormax.Doormax;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 20/02/15.
 */
public class TestingFrostbite {
    FrostbiteDomain fdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;
    List<PropositionalFunction> pfs;

    public TestingFrostbite() {
    }

    public static void main(String[] args) {
        TestingFrostbite example = new TestingFrostbite();
        example.loadDomain();
        String outputPath = "output/"; //directory to record results

        //we will call planning and learning algorithms here
        //example.run(outputPath);
        example.ValueIterationExample(outputPath);

        //run the visualizer
        example.visualizeFrostbite(outputPath);

    }

    public void loadDomain() {
        //create the domain
        fdg = new FrostbiteDomain();
        domain = fdg.generateDomain();
        initialState = fdg.getCleanState(domain);

        //create the state parser
        sp = new FrostbiteStateParser(domain);

        //define the task
        rf = new FrostbiteRF(domain);
        tf = new FrostbiteTF(domain);
        goalCondition = new TFGoalCondition(tf);


        //set up the state hashing system
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
                domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

        //set up the pfs
        pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());
    }

    public void ValueIterationExample(String outputPath){

        if(!outputPath.endsWith("/")) {
            outputPath = outputPath + "/";
        }

        OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.001, 100);
        planner.planFromState(initialState);

        //create a Q-greedy policy from the planner
        Policy p = new GreedyQPolicy((QComputablePlanner)planner);

        //record the plan results to a file
        p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);

    }

    public void run(String outputPath) {
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath + "/";
        }
        //discount= 0.99; initialQ=0.0; learning rate=0.5; lambda=1.0
        LearningAgent agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);
        //((Doormax) agent).loadModelRules(domain, "doormax/");

        //run learning for 1000 episodes
        int maxTimeSteps = 200;
        for (int i = 0; i < 50; i++) {
            EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, maxTimeSteps);
            //if(ea.numTimeSteps() < maxTimeSteps)
            ea.writeToFile(String.format("%se%03d", outputPath, i), sp);
            System.out.println(i + ": " + (ea.getReward(ea.numTimeSteps() - 1) > 0 ? "won " : "lost") + " in " +
                    ea.numTimeSteps() + " steps.");
            if ((ea.getReward(ea.numTimeSteps() - 1) > 0 && ea.numTimeSteps() < 23))
                break;
        }

        // Print the rules
        ((Doormax) agent).printModel();
        //((Doormax) agent).saveModelRules("doormax/");

        // Evaluate optimal policy with this model
        Model model = ((Doormax) agent).getModel();
        ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, model, false);
        OOMDPPlanner planner = new ValueIteration(mdg.generateDomain(), rf, model.getModelTF(), 0.99, hashingFactory, 0.001, 10);
        planner.planFromState(initialState);

        //create a Q-greedy policy from the planner
        //Policy p = new GreedyQPolicy((QComputablePlanner) planner);

        //record the plan results to a file
        //p.evaluateBehavior(initialState, rf, tf, maxTimeSteps).writeToFile(outputPath + "planResult", sp);
        System.out.println("Done");
    }

    public void visualizeFrostbite(String outputPath) {
        Visualizer v = FrostbiteVisualizer.getVisualizer(fdg);
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }
}
