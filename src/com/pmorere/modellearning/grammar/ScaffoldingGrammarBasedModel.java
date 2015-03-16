package com.pmorere.modellearning.grammar;

import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.GroundedAction;
import com.pmorere.modellearning.scaffolding.ScaffoldableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 16/03/15.
 */
public class ScaffoldingGrammarBasedModel extends GrammarBasedModel implements ScaffoldableModel {
    List<Model> subModels;

    public ScaffoldingGrammarBasedModel(Domain sourceDomain, final StateHashFactory hashingFactory,
                                        GrammarParser grammarParser, ExpressionParser expressionParser) {
        super(sourceDomain, hashingFactory, grammarParser, expressionParser);
        subModels = new ArrayList<Model>();
    }

    public void addSubModel(Model subModel) {
        this.subModels.add(subModel);
    }

    @Override
    public List<TransitionProbability> getTransitionProbabilities(State s, GroundedAction ga) {
        // This model has no submodels, use the classic function
        if (subModels.isEmpty())
            return super.getTransitionProbabilities(s, ga);

        // If the transition is modeled at this level, use the classic function
        List<TransitionProbability> classicProbs = super.getTransitionProbabilities(s, ga);
        if (!classicProbs.isEmpty())
            return classicProbs;

        // Get transition probabilities from submodels
        List<TransitionProbability> groupedTransProb = new ArrayList<TransitionProbability>();
        for (Model sub : subModels) {
            if (sub.transitionIsModeled(s, ga))
                groupedTransProb.addAll(sub.getTransitionProbabilities(s, ga));
        }

        // Check the consistency of the transitions from the submodels
        double sum = 0;
        for (TransitionProbability transProb : groupedTransProb)
            sum += transProb.p;
        for (TransitionProbability transProb : groupedTransProb)
            transProb.p /= sum;

        return groupedTransProb;
    }


    @Override
    public void updateModel(State s, GroundedAction ga, State sprime, double r, boolean sprimeIsTerminal) {
        List<TransitionProbability> transProbs = getTransitionProbabilities(s, ga);

        boolean rightTransFound = false;
        for (TransitionProbability transProb : transProbs) {
            if (transProb.s.equals(sprime)) {
                rightTransFound = true;
                break;
            }
        }

        // The combination of the submodels was wrong, update this model
        if (!rightTransFound)
            super.updateModel(s, ga, sprime, r, sprimeIsTerminal);
    }
}
