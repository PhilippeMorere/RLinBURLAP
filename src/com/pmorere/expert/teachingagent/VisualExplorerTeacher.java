package com.pmorere.expert.teachingagent;

import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import com.pmorere.modellearning.doormax.Doormax;

/**
 * Created by philippe on 03/03/15.
 */
public class VisualExplorerTeacher extends VisualExplorer {
    public static final String ENDOFTEACHING = "__ENDOFTEACHING__";
    private Doormax agent;
    private RewardFunction rf;
    private TerminalFunction tf;
    private Model model;

    public VisualExplorerTeacher(Domain domain, Visualizer painter, State baseState, Doormax agent, RewardFunction rf, TerminalFunction tf) {
        super(domain, painter, baseState);
        this.agent = agent;
        this.rf = rf;
        this.tf = tf;
        this.model = agent.getModel();
    }

    @Override
    protected void executeAction(String [] comps){
        String actionName = comps[0];

        //construct parameter list as all that remains
        String params[];
        if(comps.length > 1){
            params = new String[comps.length-1];
            for(int i = 1; i < comps.length; i++){
                params[i-1] = comps[i];
            }
        }
        else{
            params = new String[0];
        }

        if(actionName.equals(ENDOFTEACHING)){
            System.out.println("End of teaching");
            setVisible(false);
            dispose();
        }


        Action action = domain.getAction(actionName);
        if(action == null){
            System.out.println("Unknown action: " + actionName);
        }
        else{
            GroundedAction ga = new GroundedAction(action, params);
            State nextState = ga.executeIn(curState);
            if(this.currentEpisode != null){
                this.currentEpisode.recordTransitionTo(ga, nextState, this.trackingRewardFunction.reward(curState, ga, nextState));
            }

            // Update agent's model
            boolean isTerminal = tf.isTerminal(nextState);
            if (!this.model.transitionIsModeled(curState, ga)) {
                double r = rf.reward(curState, ga, nextState);
                this.model.updateModel(curState, ga, nextState, r, isTerminal);
            }

            if(isTerminal) {
                curState = baseState;
                nextState = baseState;
            }

            numSteps++;
            this.updateState(nextState);
        }
    }

    public void addEndKeyAction(String key) {
        keyActionMap.put(key, ENDOFTEACHING);
    }
}
