package com.pmorere.frostbite;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;

/**
 * Created by philippe on 05/05/15.
 */
public class FrostbiteTF implements TerminalFunction {
    private PropositionalFunction onIce;
    private PropositionalFunction inWater;
    private PropositionalFunction iglooBuilt;

    public FrostbiteTF(Domain domain) {
        this.inWater = domain.getPropFunction(FrostbiteDomain.PFINWATER);
        this.onIce = domain.getPropFunction(FrostbiteDomain.PFONICE);
        this.iglooBuilt = domain.getPropFunction(FrostbiteDomain.PFIGLOOBUILT);
    }

    @Override
    public boolean isTerminal(State s) {
        if (inWater.somePFGroundingIsTrue(s))
            return true;
        return iglooBuilt.somePFGroundingIsTrue(s) && onIce.somePFGroundingIsTrue(s);
    }
}
