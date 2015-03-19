package com.pmorere.modellearning.grammarLearner.grammar;

/**
 * Created by philippe on 19/03/15.
 */
public class GrammarRule {
    public static final GrammarRule LOGIC_RULE_AND = new GrammarRule("AND",
            new String[]{GrammarParser.BOOLEAN, GrammarParser.BOOLEAN}, GrammarParser.BOOLEAN);
    public static final GrammarRule LOGIC_RULE_OR = new GrammarRule("OR",
            new String[]{GrammarParser.BOOLEAN, GrammarParser.BOOLEAN}, GrammarParser.BOOLEAN);
    public static final GrammarRule LOGIC_RULE_NOT = new GrammarRule("NOT",
            new String[]{GrammarParser.BOOLEAN}, GrammarParser.BOOLEAN);

    public final String name;
    public final String[] inputs;
    public final String output;

    public GrammarRule(String name, String[] inputs, String output) {
        this.name = name;
        this.inputs = inputs;
        this.output = output;
    }

    public boolean isApplicableOn(GrammarRule other) {
        if (inputs.length != 1)
            return false;
        return inputs[0].equals(other.output);
    }

    public boolean isApplicableOn(GrammarRule other, GrammarRule other2) {
        if (inputs.length != 2)
            return false;
        return inputs[0].equals(other.output) && inputs[1].equals(other2.output);
    }
}
