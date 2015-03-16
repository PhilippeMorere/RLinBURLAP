package com.pmorere.testing;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.doormax.Doormax;
import com.pmorere.sokoban.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 20/02/15.
 */
public class TestingDoormax {
    GridWorldDomain gwdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;
    List<PropositionalFunction> pfs;

    public TestingDoormax() {
    }

    public static void main(String[] args) {

        TestingDoormax example = new TestingDoormax();
        //example.testOnGridWorld();
        example.testOnSokoban();
        String outputPath = "output/"; //directory to record results

        //we will call planning and learning algorithms here
        example.DoormaxExample(outputPath);

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

        //set up the pfs
        pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());
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


        //set up the pfs
        pfs = new ArrayList<PropositionalFunction>();
        pfs.addAll(domain.getPropFunctions());
        pfs.remove(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
    }

    public void DoormaxExample(String outputPath) {
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
            System.out.println(i + ": " + (ea.getReward(ea.numTimeSteps()-1) > 0 ? "won " : "lost") + " in " +
                    ea.numTimeSteps() + " steps.");
            if((ea.getReward(ea.numTimeSteps()-1) > 0 && ea.numTimeSteps() < 23))
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

    public void visualizeGridWorld(String outputPath) {
        Visualizer v = GridWorldVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }

    public void visualizeSokoban(String outputPath) {
        Visualizer v = SokobanVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }
}
