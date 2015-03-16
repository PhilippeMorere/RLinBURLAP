package com.pmorere.expert;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.doormax.Doormax;
import com.pmorere.sokoban.*;
import com.pmorere.teachingagent.VisualExplorerTeacher;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 03/03/15.
 */
public class TeachingDoormax {
    SokobanDomain gwdg;
    Domain domain;
    StateParser sp;
    RewardFunction rf;
    TerminalFunction tf;
    StateConditionTest goalCondition;
    State initialState;
    DiscreteStateHashFactory hashingFactory;
    List<PropositionalFunction> pfs;

    public static void main(String[] args) {

        TeachingDoormax example = new TeachingDoormax();
        example.testOnSokoban();
        String outputPath = "output/"; //directory to record results

        //we will call planning and learning algorithms here
        example.DoormaxExample(outputPath);

        //run the visualizer
        example.visualizeSokoban(outputPath);
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
        ((SokobanRF) rf).addPFReward(domain.getPropFunction(SokobanDomain.PFROCKISBLOCKED), -200);
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
    }

    public void DoormaxExample(String outputPath) {
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath + "/";
        }

        List<ObjectInstance> rocks = initialState.getObjectsOfTrueClass(SokobanDomain.CLASSROCK);
        ObjectInstance rock = rocks.get(0);

        //discount= 0.99; initialQ=0.0; learning rate=0.5; lambda=1.0
        Doormax agent = new Doormax(domain, rf, tf, 0.99, hashingFactory, 1, 30, initialState, pfs, 0);


        // Let's teach!
        Visualizer v = SokobanVisualizer.getVisualizer(gwdg.getMap());
        VisualExplorerTeacher teacher = new VisualExplorerTeacher(domain, v, initialState, agent, rf, tf);
        teacher.addKeyAction("w", SokobanDomain.ACTIONNORTH);
        teacher.addKeyAction("s", SokobanDomain.ACTIONSOUTH);
        teacher.addKeyAction("a", SokobanDomain.ACTIONWEST);
        teacher.addKeyAction("d", SokobanDomain.ACTIONEAST);
        teacher.addEndKeyAction("p");
        teacher.initGUI();

        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (teacher.isActive());
        int j = 0;


        // Print the rules
        agent.printModel();

        //run learning for 1000 episodes
        int maxTimeSteps = 200;
        for (int i = 0; i < 100; i++) {
            // Random position for the rock
            //rock.setValue(SokobanDomain.ATTX, (int) (Math.random() * 3 + 7));
            //rock.setValue(SokobanDomain.ATTX, (int) (Math.random() * 3 + 7));

            EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, maxTimeSteps);
            //if(ea.numTimeSteps() < maxTimeSteps)
            ea.writeToFile(String.format("%se%03d", outputPath, i), sp);
            System.out.println(i + ": " + (ea.getReward(ea.numTimeSteps() - 1) > 0 ? "won " : "lost") + " in " +
                    ea.numTimeSteps() + " steps.");
            if((ea.getReward(ea.numTimeSteps()-1) > 0 && ea.numTimeSteps() < 30))
                break;
        }

        // Print the rules
        agent.printModel();

        // Evaluate optimal policy with this model
        Model model = ((Doormax) agent).getModel();
        ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, model, false);
        OOMDPPlanner planner = new ValueIteration(mdg.generateDomain(), model.getModelRF(), model.getModelTF(), 0.99, hashingFactory, 0.001, 100);
        planner.planFromState(initialState);

        //create a Q-greedy policy from the planner
        Policy p = new GreedyQPolicy((QComputablePlanner) planner);

        //record the plan results to a file
        p.evaluateBehavior(initialState, rf, tf, maxTimeSteps).writeToFile(outputPath + "planResult", sp);
        System.out.println("Done");
    }

    public void visualizeSokoban(String outputPath) {
        Visualizer v = SokobanVisualizer.getVisualizer(gwdg.getMap());
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
    }
}
