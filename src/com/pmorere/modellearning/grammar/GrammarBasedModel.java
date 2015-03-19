package com.pmorere.modellearning.grammar;

import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import com.pmorere.sokoban.SokobanDomain;

import java.util.*;

/**
 * Created by philippe on 12/03/15.
 */
public class GrammarBasedModel extends Model {
    private final TerminalFunction modeledTF;
    /**
     * The set of states marked as terminal states.
     */
    protected Set<StateHashTuple> terminalStates;

    protected Domain sourceDomain;

    protected StateHashFactory hashingFactory;

    protected ExpressionParser expressionParser;

    protected GrammarParser grammarParser;

    protected Map<ActionEffect, ExpressionsForEffect> allExpressions;

    public GrammarBasedModel(Domain sourceDomain, final StateHashFactory hashingFactory,
                             GrammarParser grammarParser, ExpressionParser expressionParser) {
        this.sourceDomain = sourceDomain;
        this.terminalStates = new HashSet<StateHashTuple>();
        this.allExpressions = new HashMap<ActionEffect, ExpressionsForEffect>();

        this.expressionParser = expressionParser;
        this.grammarParser = grammarParser;
        this.hashingFactory = hashingFactory;


        this.modeledTF = new TerminalFunction() {

            @Override
            public boolean isTerminal(State s) {
                return terminalStates.contains(hashingFactory.hashState(s));
            }
        };
    }

    @Override
    public RewardFunction getModelRF() {
        throw new RuntimeException("The reward function is not learnt by the model (yet)!");
    }

    @Override
    public TerminalFunction getModelTF() {
        return this.modeledTF;
    }

    @Override
    public boolean transitionIsModeled(State s, GroundedAction ga) {
        return !getTransitionProbabilities(s, ga).isEmpty();
    }

    @Override
    public boolean stateTransitionsAreModeled(State s) {
        for (GroundedAction ga : Action.getAllApplicableGroundedActionsFromActionList(sourceDomain.getActions(), s))
            if (!transitionIsModeled(s, ga))
                return false;
        return true;
    }

    @Override
    public List<AbstractGroundedAction> getUnmodeledActionsForState(State s) {
        List<AbstractGroundedAction> notKnown = new ArrayList<AbstractGroundedAction>();
        for (GroundedAction ga : Action.getAllApplicableGroundedActionsFromActionList(sourceDomain.getActions(), s))
            if (!transitionIsModeled(s, ga))
                notKnown.add(ga);
        return notKnown;
    }

    @Override
    public State sampleModelHelper(State s, GroundedAction ga) {
        return this.sampleTransitionFromTransitionProbabilities(s, ga);
    }

    @Override
    public List<TransitionProbability> getTransitionProbabilities(State s, GroundedAction ga) {
        List<Effect> applicableEffects = new ArrayList<Effect>();

        // Use the ExpressionParser on the current grammar to predict what's gonna happen
        expressionParser.setStateHashTuple(hashingFactory.hashState(s));
        for (Map.Entry<ActionEffect, ExpressionsForEffect> eff : allExpressions.entrySet())
            if (eff.getValue().ga.equals(ga))
                if (expressionParser.evaluate(eff.getValue().currentExpression))
                    applicableEffects.add(eff.getKey().ef);

        // Create new estimated state by applying all applicable effects to the previous state
        State sprime = s.copy();
        for (Effect effect : applicableEffects)
            sprime = effect.applyToState(sprime);

        // Add the estimated state as the only TransitionProbability
        List<TransitionProbability> transProb = new ArrayList<TransitionProbability>();
        transProb.add(new TransitionProbability(sprime, 1));

        return transProb;
    }

    @Override
    public void updateModel(State s, GroundedAction ga, State sprime, double r, boolean sprimeIsTerminal) {
        StateHashTuple sh = hashingFactory.hashState(s);

        if (sprimeIsTerminal)
            this.terminalStates.add(hashingFactory.hashState(sprime));

        // For each effect in Effects(S'- S) add a new ExpressionForEffect if it doesn't exist
        List<Effect> activeEffects = new Effects(s, sprime).effects;
        for (Effect effect : activeEffects) {
            ActionEffect ae = new ActionEffect(ga, effect);
            if (!allExpressions.containsKey(ae))
                allExpressions.put(ae, new ExpressionsForEffect(effect, ga));

            // Add the state to the corresponding state memory
            allExpressions.get(ae).addStateToMemory(sh);
        }

        // For all possible effects after this action, process expressions that predicted right and wrong
        for (Map.Entry<ActionEffect, ExpressionsForEffect> entry : allExpressions.entrySet())
            if (entry.getKey().ga.equals(ga)) {
                // Remove the potential grammars that didn't predict it
                entry.getValue().filterOutWrongExpressions(sh, entry.getValue().stateMemory.contains(sh));

                // Check if the current expression hasn't been ruled out
                entry.getValue().checkCurrentExpression();
            }
    }

    public void printRules() {
        for (Map.Entry<ActionEffect, ExpressionsForEffect> entry : allExpressions.entrySet())
            System.out.println("Effect " + entry.getKey().ef + " happens for action " + entry.getKey().ga + " if: " + entry.getValue().currentExpression);

    }

    @Override
    public void resetModel() {
        // Empty grammar lists
        for (Map.Entry<ActionEffect, ExpressionsForEffect> entry : allExpressions.entrySet())
            entry.getValue().reset();
    }

    public static void main(String[] args) {
        final GridWorldDomain gwd = new GridWorldDomain(11, 11);
        gwd.setMapToFourRooms();

        Domain d = gwd.generateDomain();
        State s = gwd.getOneAgentOneLocationState(d);
        gwd.setAgent(s, 0, 0);
        gwd.setLocation(s, 0, 10, 10);

        DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
        hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
                d.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

        // Set up the grammar
        GrammarParser gp = new GrammarParser();
        gp.addRule("Agent", "place");
        gp.addRule("EAST", "place", "place");
        gp.addRule("WEST", "place", "place");
        gp.addRule("NORTH", "place", "place");
        gp.addRule("SOUTH", "place", "place");
        gp.addRule("EMPTY", "place", GrammarParser.BOOLEAN);
        //gp.addRule("AND", new String[]{GrammarParser.BOOLEAN, GrammarParser.BOOLEAN}, GrammarParser.BOOLEAN);
        //gp.addRule("OR", new String[]{GrammarParser.BOOLEAN, GrammarParser.BOOLEAN}, GrammarParser.BOOLEAN);
        gp.addRule("NOT", GrammarParser.BOOLEAN, GrammarParser.BOOLEAN);

        ExpressionParser ep = new ExpressionParser("Agent") {

            int[][] map = gwd.getMap();

            @Override
            public Object evaluateOperator(String symbol, Object[] args) {
                if (symbol.equals("AND"))
                    return (Boolean) args[0] && (Boolean) args[1];
                else if (symbol.equals("OR"))
                    return (Boolean) args[0] || (Boolean) args[1];
                else if (symbol.equals("NOT"))
                    return !(Boolean) args[0];
                else if (symbol.equals("EMPTY")) {
                    Pos pos = getXY((String) args[0]);
                    if (pos.x >= gwd.getWidth() || pos.x < 0 || pos.y >= gwd.getHeight() || pos.y < 0)
                        return false;
                    return map[pos.x][pos.y] == 0;
                } else if (symbol.equals("EAST")) {
                    Pos pos = getXY((String) args[0]);
                    return (pos.x + 1) + "," + pos.y;
                } else if (symbol.equals("WEST")) {
                    Pos pos = getXY((String) args[0]);
                    return (pos.x - 1) + "," + pos.y;
                } else if (symbol.equals("SOUTH")) {
                    Pos pos = getXY((String) args[0]);
                    return pos.x + "," + (pos.y - 1);
                } else if (symbol.equals("NORTH")) {
                    Pos pos = getXY((String) args[0]);
                    return pos.x + "," + (pos.y + 1);
                }
                throw new RuntimeException("Unsupported symbol " + symbol);
            }

            private Pos getXY(String arg) {
                int x, y;
                if (arg.equals("Agent")) {
                    ObjectInstance agent = sh.s.getObjectsOfTrueClass(GridWorldDomain.CLASSAGENT).get(0);
                    x = agent.getDiscValForAttribute(GridWorldDomain.ATTX);
                    y = agent.getDiscValForAttribute(GridWorldDomain.ATTY);
                } else {
                    String[] coord = arg.split(",");
                    x = Integer.valueOf(coord[0]);
                    y = Integer.valueOf(coord[1]);
                }
                return new Pos(x, y);
            }

            class Pos {
                public int x, y;

                public Pos(int x, int y) {
                    this.x = x;
                    this.y = y;
                }
            }
        };

        GrammarBasedModel model = new GrammarBasedModel(d, hashingFactory, gp, ep);


        // Try it out
        List<Action> actions = d.getActions();
        List<GroundedAction> gas = new ArrayList<GroundedAction>();
        for (Action a : actions)
            gas.add(new GroundedAction(a, new String[]{SokobanDomain.CLASSAGENT}));

        // North
        for (int i = 0; i < 100; i++) {
            GroundedAction ga = gas.get((int) (Math.random() * gas.size()));
            ObjectInstance agent = s.getObjectsOfTrueClass(GridWorldDomain.CLASSAGENT).get(0);
            State sp = ga.executeIn(s);
            model.updateModel(s, ga, sp, -0.1, false);
            model.printRules();
            s = sp;
        }
    }


    protected class ActionEffect {
        GroundedAction ga;
        Effect ef;

        public ActionEffect(GroundedAction ga, Effect ef) {
            this.ga = ga;
            this.ef = ef;

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActionEffect that = (ActionEffect) o;

            if (!ef.equals(that.ef)) return false;
            if (!ga.equals(that.ga)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = ga.hashCode();
            result = 31 * result + ef.hashCode();
            return result;
        }
    }

    protected class ExpressionsForEffect {

        protected List<String> potentialExpressions;

        protected String currentExpression;

        protected Set<StateHashTuple> stateMemory;

        protected Effect effect;

        protected GroundedAction ga;

        protected int grammarLevel = 0;

        public ExpressionsForEffect(Effect effect, GroundedAction ga) {
            this.effect = effect;
            this.ga = ga;
            this.stateMemory = new HashSet<StateHashTuple>();
            this.setNewCurrentExpression(null);
        }

        protected void setNewCurrentExpression(String subExpression) {
            while (potentialExpressions == null || potentialExpressions.isEmpty()) {
                // Generate new expressions from grammar

                if (grammarLevel == 3 && subExpression == null)
                    return;//throw new RuntimeException("Grammar level 3!");
                if (subExpression == null)
                    potentialExpressions = grammarParser.generateAllExpsFromGrammar(grammarLevel++);
                    //potentialExpressions = grammarParser.generateNExpsFromGrammar(100);
                else
                    potentialExpressions = grammarParser.generateAllExpsFromSubExpression(subExpression);


                // Go through the state memory of similar action (different effect or not)
                for (Map.Entry<ActionEffect, ExpressionsForEffect> entry1 : allExpressions.entrySet())
                    if (entry1.getKey().ga.equals(ga))
                        for (StateHashTuple memorizedState : entry1.getValue().stateMemory)
                            filterOutWrongExpressions(memorizedState, this.stateMemory.contains(memorizedState));
            }
            currentExpression = potentialExpressions.get(0);
        }

        protected void reset() {
            potentialExpressions.clear();
            currentExpression = null;
        }

        protected void checkCurrentExpression() {
            // If the current expression is not in the potential ones, find a new current expression
            if (!potentialExpressions.contains(currentExpression))
                setNewCurrentExpression(null);
        }

        protected void addStateToMemory(StateHashTuple sh) {
            // Add the new state to the memory
            stateMemory.add(sh);
        }

        public void filterOutWrongExpressions(StateHashTuple sh, boolean effectIsActive) {
            // Remove all potential expressions that are false
            expressionParser.setStateHashTuple(sh);
            for (Iterator<String> iterator = potentialExpressions.iterator(); iterator.hasNext(); ) {
                String exp = iterator.next();
                if (expressionParser.evaluate(exp) != effectIsActive)
                    iterator.remove();
            }
        }
    }

    protected enum EffectTypes {
        TOOGLE, INC, DEC, NOEFFECT
    }

    protected class Effects {
        protected List<Effect> effects;

        public Effects(State s, State sprime) {
            effects = new ArrayList<Effect>();
            for (ObjectInstance obj : s.getAllObjects()) {
                List<Value> vs = obj.getValues();
                List<Value> vps = sprime.getObject(obj.getName()).getValues();

                for (int i = 0; i < vps.size(); i++)
                    compareTwoValues(vs.get(i), vps.get(i), obj.getName());
            }
            if (effects.isEmpty())
                effects.add(new Effect(EffectTypes.NOEFFECT, "noeffect", "noeffect"));
        }

        private void compareTwoValues(Value v1, Value v2, String objName) {
            if (!v1.attName().equals(v2.attName()))
                throw new RuntimeException("Can't compare two different attributes");
            Attribute a1 = v1.getAttribute();
            Attribute a2 = v2.getAttribute();
            if (a1.type != a2.type)
                throw new RuntimeException("Can't compare two attributes of different type");
            switch (a1.type) {
                case INT:
                    if (v1.getDiscVal() == v2.getDiscVal())
                        break;
                    if (v1.getDiscVal() == v2.getDiscVal() - 1)
                        effects.add(new Effect(EffectTypes.INC, objName, v1.attName()));
                    else if (v1.getDiscVal() == v2.getDiscVal() + 1)
                        effects.add(new Effect(EffectTypes.DEC, objName, v1.attName()));
                    else
                        throw new RuntimeException("Can only handle increment and decrement of 1 for int values");
                    break;
                case BOOLEAN:
                    if (v1.getBooleanValue() != v2.getBooleanValue())
                        effects.add(new Effect(EffectTypes.TOOGLE, objName, v1.attName()));
                    break;
                case DISC:
                    // TODO: support this one
                    break;
                default:
                    throw new RuntimeException("Attribute " + a1.name + " of object " + objName +
                            ": Comparaison of type " + a1.type + " is not supported.");
            }
        }
    }

    protected class Effect {
        protected EffectTypes type;
        protected String objectName;
        protected String attName;

        public Effect(EffectTypes type, String objectName, String attName) {
            this.type = type;
            this.objectName = objectName;
            this.attName = attName;
            if (objectName.contains("rock"))
                System.out.println(this);
        }

        @Override
        public String toString() {
            return "Effect{" +
                    "type=" + type.name() +
                    ", objectName='" + objectName + '\'' +
                    ", attName='" + attName + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Effect effect = (Effect) o;

            if (!attName.equals(effect.attName)) return false;
            if (!objectName.equals(effect.objectName)) return false;
            if (type != effect.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + objectName.hashCode();
            result = 31 * result + attName.hashCode();
            return result;
        }

        public State applyToState(State s) {
            ObjectInstance obj = s.getObject(objectName);
            switch (type) {
                case TOOGLE:
                    obj.setValue(attName, !obj.getBooleanValue(attName));
                    break;
                case INC:
                    obj.setValue(attName, obj.getDiscValForAttribute(attName) + 1);
                    break;
                case DEC:
                    obj.setValue(attName, obj.getDiscValForAttribute(attName) - 1);
                    break;
                case NOEFFECT:
                    break;
            }
            return s;
        }

        public Effect copy() {
            return new Effect(type, objectName, attName);
        }
    }
}
