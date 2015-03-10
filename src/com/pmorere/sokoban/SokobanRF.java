package com.pmorere.sokoban;

/**
 * Created by philippe on 2/17/15.
 */

import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SokobanRF implements RewardFunction {
    private double pushRockReward;
    private double defaultReward;
    private Map<PropositionalFunction, Double> pfRewards = new HashMap<PropositionalFunction, Double>();

    public SokobanRF(double pushRockReward, double defaultReward) {
        this.pushRockReward = pushRockReward;
        this.defaultReward = defaultReward;
    }

    public void addPFReward(PropositionalFunction pf, double reward) {
        this.pfRewards.put(pf, reward);
    }

    @Override
    public double reward(State s, GroundedAction a, State sprime) {
        double reward = 0;
        boolean customReward = false;

        // Reward the agent if it pushed a rock
        List<ObjectInstance> rocks = s.getObjectsOfTrueClass(SokobanDomain.CLASSROCK);
        List<ObjectInstance> rocksPrime = sprime.getObjectsOfTrueClass(SokobanDomain.CLASSROCK);
        for (int i = 0; i < Math.min(rocks.size(), rocksPrime.size()); i++)
            if (rocks.get(i).getDiscValForAttribute(SokobanDomain.ATTX) != rocksPrime.get(i).getDiscValForAttribute(SokobanDomain.ATTX)
                    || rocks.get(i).getDiscValForAttribute(SokobanDomain.ATTY) != rocksPrime.get(i).getDiscValForAttribute(SokobanDomain.ATTY)) {
                reward += pushRockReward;
                customReward = true;
            }

        // Check if any of the reward associated with PFs apply
        for (Map.Entry<PropositionalFunction, Double> entry : pfRewards.entrySet()) {
            PropositionalFunction pf = entry.getKey();
            List<GroundedProp> gps = pf.getAllGroundedPropsForState(sprime);
            for (GroundedProp gp : gps)
                if (gp.isTrue(sprime)) {
                    reward += entry.getValue();
                    customReward = true;
                    break;
                }
        }

        if (customReward)
            return reward;
        else
            return defaultReward;

    }

}

