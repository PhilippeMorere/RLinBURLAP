package com.pmorere.frostbite;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

/**
 * Created by philippe on 05/05/15.
 */
public class FrostbiteRF implements RewardFunction {
    public double goalReward = 1000.0;
    public double lostReward = -1000.0;
    public double defaultReward = -1.0;
    private PropositionalFunction onIce;
    private PropositionalFunction onPlatform;
    private PropositionalFunction inWater;
    private PropositionalFunction iglooBuilt;

    public FrostbiteRF(Domain domain) {
        this.onPlatform = domain.getPropFunction(FrostbiteDomain.PFONPLATFORM);
        this.inWater = domain.getPropFunction(FrostbiteDomain.PFINWATER);
        this.onIce = domain.getPropFunction(FrostbiteDomain.PFONICE);
        this.iglooBuilt = domain.getPropFunction(FrostbiteDomain.PFIGLOOBUILT);
    }

    @Override
    public double reward(State s, GroundedAction a, State sprime) {
        if (inWater.somePFGroundingIsTrue(sprime))
            return lostReward;
        if (iglooBuilt.somePFGroundingIsTrue(sprime) && onIce.somePFGroundingIsTrue(s))
            return goalReward;
        return defaultReward;
    }
}
