package com.pmorere.sokoban;

/**
 * Created by philippe on 2/17/15.
 */

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

import java.util.List;

public class SokobanStateParser implements StateParser {

    protected Domain domain;


    public SokobanStateParser(int width, int height){
        SokobanDomain generator = new SokobanDomain(width, height);
        this.domain = generator.generateDomain();
    }

    public SokobanStateParser(Domain domain){
        this.domain = domain;
    }

    @Override
    public String stateToString(State s) {

        StringBuffer sbuf = new StringBuffer(256);

        ObjectInstance a = s.getObjectsOfTrueClass(SokobanDomain.CLASSAGENT).get(0);
        List<ObjectInstance> locs = s.getObjectsOfTrueClass(SokobanDomain.CLASSLOCATION);
        List<ObjectInstance> rocks = s.getObjectsOfTrueClass(SokobanDomain.CLASSROCK);

        String xa = SokobanDomain.ATTX;
        String ya = SokobanDomain.ATTY;
        String lt = SokobanDomain.ATTLOCTYPE;

        sbuf.append(a.getDiscValForAttribute(xa)).append(" ").append(a.getDiscValForAttribute(ya));
        for(ObjectInstance l : locs){
            sbuf.append(", ").append(l.getDiscValForAttribute(xa)).append(" ").append(l.getDiscValForAttribute(ya)).append(" ").append(l.getDiscValForAttribute(lt));
        }
        for(ObjectInstance r : rocks){
            sbuf.append(", ").append(r.getDiscValForAttribute(xa)).append(" ").append(r.getDiscValForAttribute(ya)).append(" R");
        }


        return sbuf.toString();
    }

    @Override
    public State stringToState(String str) {

        String [] obcomps = str.split(", ");

        String [] acomps = obcomps[0].split(" ");
        int ax = Integer.parseInt(acomps[0]);
        int ay = Integer.parseInt(acomps[1]);

        int nl = obcomps.length - 1;

        State s = SokobanDomain.getOneAgentNLocationState(domain, nl);
        SokobanDomain.setAgent(s, ax, ay);

        for(int i = 1; i < obcomps.length; i++){
            String [] lcomps = obcomps[i].split(" ");
            int lx = Integer.parseInt(lcomps[0]);
            int ly = Integer.parseInt(lcomps[1]);

            if(lcomps.length < 3){
                SokobanDomain.setLocation(s, i-1, lx, ly);
            }
            else if(lcomps[2].equals("R")) {
                SokobanDomain.addRock(domain, s, lx, ly);
            }else{
                int lt = Integer.parseInt(lcomps[2]);
                SokobanDomain.setLocation(s, i-1, lx, ly, lt);
            }

        }


        return s;
    }

}

