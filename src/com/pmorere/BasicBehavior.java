package com.pmorere;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.SarsaLam;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.visualizer.Visualizer;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import com.pmorere.modellearning.doormax.Doormax;
import com.pmorere.sokoban.SokobanDomain;
import com.pmorere.sokoban.SokobanRF;
import com.pmorere.sokoban.SokobanStateParser;
import com.pmorere.sokoban.SokobanVisualizer;

public class BasicBehavior {
    SokobanDomain gwdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;

    public BasicBehavior(){

        //create the domain
        gwdg = new SokobanDomain(11, 11);
        gwdg.setMapToSokoban();
        domain = gwdg.generateDomain();

        //create the state parser
        sp = new SokobanStateParser(domain);

        //define the task
        rf = new SokobanRF(1, -1);
        ((SokobanRF)rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKISBLOCKED), -2);
        ((SokobanRF)rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKATLOCATION), 200);

        tf = new SinglePFTF(domain.getPropFunction(SokobanDomain.PFROCKATLOCATION));
        goalCondition = new TFGoalCondition(tf);

        //set up the initial state of the task
        initialState = gwdg.getSokobanMapState(domain);
        //initialState = SokobanDomain.getOneAgentOneLocationState(domain);
        //SokobanDomain.setAgent(initialState, 0, 0);
        //SokobanDomain.setLocation(initialState, 0, 10, 10);
        //SokobanDomain.addRock(domain, initialState, 1, 1);

        //set up the state hashing system
        hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSAGENT,
                domain.getObjectClass(SokobanDomain.CLASSAGENT).attributeList);
        hashingFactory.setAttributesForClass(SokobanDomain.CLASSROCK,
                domain.getObjectClass(SokobanDomain.CLASSROCK).attributeList);


    }

    public void visualize(String outputPath){
        Visualizer v = SokobanVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }

    public static void main(String[] args) {

        BasicBehavior example = new BasicBehavior();
        String outputPath = "output/"; //directory to record results

        //we will call planning and learning algorithms here
        example.SarsaLearningExample(outputPath);

        //run the visualizer
        example.visualize(outputPath);

    }


    public void SarsaLearningExample(String outputPath){

        if(!outputPath.endsWith("/")){
            outputPath = outputPath + "/";
        }
        //discount= 0.99; initialQ=0.0; learning rate=0.5; lambda=1.0
        LearningAgent agent = new SarsaLam(domain, rf, tf, 0.99, hashingFactory, 0., 0.5, 1.0);

        //run learning for 1000 episodes
        int maxTimeSteps = 500;
        for(int i = 0; i < 1000; i++){
            EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, maxTimeSteps);
            if(ea.numTimeSteps() < maxTimeSteps)
                ea.writeToFile(String.format("%se%03d", outputPath, i), sp);
            System.out.println(i + ": " + ea.numTimeSteps());
        }

    }
}
