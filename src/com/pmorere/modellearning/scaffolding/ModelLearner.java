package com.pmorere.modellearning.scaffolding;

import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.Model;

/**
 * Created by philippe on 16/03/15.
 */
public interface ModelLearner extends LearningAgent{

    public abstract Model getModel();

    public abstract void printModel();
}
