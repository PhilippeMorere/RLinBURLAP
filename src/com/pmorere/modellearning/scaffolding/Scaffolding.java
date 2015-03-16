package com.pmorere.modellearning.scaffolding;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.scaffolding.Tree.BottomUpTraversal;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by philippe on 09/03/15.
 */
public class Scaffolding implements LearningAgent {

    private final Tree<ScaffoldStep> scaffoldTree;
    private final String outputPath;


    /**
     * the saved previous learning episodes
     */
    protected LinkedList<EpisodeAnalysis> episodeHistory = new LinkedList<EpisodeAnalysis>();

    /**
     * The number of the most recent learning episodes to store.
     */
    protected int numEpisodesToStore = 1;

    public Scaffolding(String outputPath) {
        this.scaffoldTree = new Tree<ScaffoldStep>(null);
        this.outputPath = outputPath;
    }

    /* Appends child to the Domain tree, under parent */
    public Tree.Node addScaffoldingElementTo(Tree.Node parent, String elementName, Domain childDomain, ModelLearner childAgent,
                                             State initialState, StateParser sp, int[][] map, int maxEpisode, int maxSteps) {
        Tree.Node<ScaffoldStep> childNode = new Tree.Node<ScaffoldStep>();
        if (parent == null)
            parent = scaffoldTree.root;
        else // Build model tree as well
            ((ScaffoldableModel) ((ScaffoldStep) parent.data).model).addSubModel(childAgent.getModel());
        parent.addChild(childNode, new ScaffoldStep(childDomain, elementName, childAgent, initialState, sp, map, maxEpisode, maxSteps));
        return childNode;
    }

    @Override
    public EpisodeAnalysis runLearningEpisodeFrom(State initialState) {
        return runLearningEpisodeFrom(initialState, -1);
    }

    @Override
    public EpisodeAnalysis runLearningEpisodeFrom(State initialState, int maxSteps) {
        if (this.scaffoldTree.root.children.isEmpty())
            throw new RuntimeException("Scaffolding tree is empty");

        ScaffoldStep step = this.scaffoldTree.root.children.get(0).data;

        StateParser sp = step.sp;
        String newPath = outputPath.substring(0, outputPath.length() - 1) + step.elementName + "/";

        //System.out.println("episode " + episodeNb);
        EpisodeAnalysis ea = step.agent.runLearningEpisodeFrom(step.initialState, maxSteps);

        // Save to file
        //ea.writeToFile(String.format("%se%03d", newPath, episodeNb), sp);
        episodeHistory.offer(ea);
        //step.agent.printModelRules();
        //visualizeGridWorld(step.map, step.domain, step.sp, newPath);
        return ea;
    }

    public EpisodeAnalysis runSubTasks() {
        BottomUpTraversal traversal = new BottomUpTraversal(this.scaffoldTree.root);

        Tree.Node topStep = this.scaffoldTree.root.children.get(0);
        while (traversal.hasNext()) {
            Tree.Node<ScaffoldStep> node = (Tree.Node) traversal.next();
            if (node == topStep)
                break;
            ScaffoldStep step = node.data;
            System.out.println("Starting step " + step.elementName);
            StateParser sp = step.sp;
            String newPath = outputPath.substring(0, outputPath.length() - 1) + step.elementName + "/";

            int episodeNb = 0;
            while (episodeNb < step.maxEpisode) {
                System.out.println("episode " + episodeNb);
                EpisodeAnalysis ea = step.agent.runLearningEpisodeFrom(step.initialState, step.maxSteps);

                // Save to file
                ea.writeToFile(String.format("%se%03d", newPath, episodeNb), sp);
                episodeHistory.offer(ea);
                episodeNb++;
            }
            step.agent.printModel();
            //System.out.println("Episode saved");
            visualizeGridWorld(step.map, step.domain, step.sp, newPath);
        }
        return null;
    }


    public void visualizeGridWorld(int[][] map, Domain domain, StateParser sp, String outputPath) {
        Visualizer v = GridWorldVisualizer.getVisualizer(map);
        EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, sp, outputPath);
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

    class ScaffoldStep {
        protected Domain domain;
        protected State initialState;
        protected Policy policy;
        protected ModelLearner agent;
        protected StateParser sp;
        protected int[][] map;
        protected String elementName;
        protected int maxSteps;
        protected int maxEpisode;
        protected Model model;

        public ScaffoldStep(Domain domain, String elementName, ModelLearner agent, State initialState, StateParser sp, int[][] map, int maxEpisode, int maxSteps) {
            this.maxSteps = maxSteps;
            this.maxEpisode = maxEpisode;
            this.domain = domain;
            this.sp = sp;
            this.map = map;
            this.initialState = initialState;
            this.elementName = elementName;
            this.agent = agent;
            this.model = agent.getModel();
            if (!QComputablePlanner.class.isInstance(agent))
                throw new RuntimeException("Agent must implement QComputablePlanner.");
            if (!LearningAgent.class.isInstance(agent))
                throw new RuntimeException("Agent must implement LearningAgent.");
        }
    }
}
