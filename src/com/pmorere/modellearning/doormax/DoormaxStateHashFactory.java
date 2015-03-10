package com.pmorere.modellearning.doormax;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 19/02/15.
 */
public class DoormaxStateHashFactory implements StateHashFactory {

    List<PropositionalFunction> pfs;
    /**
     * Initializes this hashing factory to compute hash codes with all attributes of all object classes.
     */
    public DoormaxStateHashFactory() {
        pfs = new ArrayList<PropositionalFunction>();
    }

    public void addPFtoHash(PropositionalFunction pf){
        pfs.add(pf);
    }

    public void addAllPFtoHash(List<PropositionalFunction> pf){
        pfs.addAll(pf);
    }

    @Override
    public StateHashTuple hashState(State s) {
        return new BooleanStateHashTuple(s);
    }

    protected class BooleanStateHashTuple extends StateHashTuple{
        private String hashStr;

        /**
         * Initializes the StateHashTuple with the given {@link burlap.oomdp.core.State} object.
         *
         * @param s the state object this object will wrap
         */
        public BooleanStateHashTuple(State s) {
            super(s);
        }

        @Override
        public void computeHashCode() {
            int hash = 0;
            hashStr = "";
            int offset = 1;
            for(PropositionalFunction pf : pfs){
                List<GroundedProp> gps = pf.getAllGroundedPropsForState(s);
                for(GroundedProp gp : gps) {
                    if (gp.isTrue(s)){
                        hash += offset;
                        hashStr = "1" + hashStr;
                    } else
                        hashStr = "0" + hashStr;
                    offset *= 2;
                }
            }

            hashCode = hash;
            needToRecomputeHashCode = false;
        }

        public String hashCodeStr(){
            if(needToRecomputeHashCode){
                this.computeHashCode();
            }
            return hashStr;
        }
    }
}
