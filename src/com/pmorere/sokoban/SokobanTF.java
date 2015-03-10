package com.pmorere.sokoban;

import burlap.oomdp.core.*;

import java.util.List;

/**
 * Created by philippe on 24/02/15.
 */
public class SokobanTF implements TerminalFunction {
    PropositionalFunction rockAtLoc;
    PropositionalFunction rockIsBlocked;
    public SokobanTF(Domain domain) {
        this.rockAtLoc = domain.getPropFunction(SokobanDomain.PFROCKATLOCATION);
        this.rockIsBlocked = domain.getPropFunction(SokobanDomain.PFROCKISBLOCKED);
    }

    @Override
    public boolean isTerminal(State s) {
        List<GroundedProp> gps = this.rockAtLoc.getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            if (gp.isTrue(s)) {
                return true;
            }
        }

        gps = this.rockIsBlocked.getAllGroundedPropsForState(s);
        for (GroundedProp gp : gps) {
            if (gp.isTrue(s)) {
                return true;
            }
        }

        return false;
    }
}
