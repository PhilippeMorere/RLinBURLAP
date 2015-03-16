package com.pmorere.modellearning.grammar;

import java.util.*;

/**
 * Created by philippe on 12/03/15.
 */
public class GrammarParser {
    public static final String BOOLEAN = "boolean";

    protected Map<String, Rule> rules;
    protected Map<String, Integer> ruleOutputCount;
    protected Map.Entry<String, Rule> root;

    public GrammarParser() {
        rules = new HashMap<String, Rule>();
        ruleOutputCount = new HashMap<String, Integer>();
    }

    public void addRule(String name, String input, String output) {
        this.addRule(name, new String[]{input}, output);
    }

    public void addRule(String name, String[] inputs, String output) {
        this.rules.put(name, new Rule(inputs, output));
        if (!this.ruleOutputCount.containsKey(output))
            this.ruleOutputCount.put(output, 0);
        this.ruleOutputCount.put(output, this.ruleOutputCount.get(output) + 1);
    }

    public void addRule(String name, String output) {
        this.addRule(name, (String[]) null, output);
        for (Map.Entry<String, Rule> entry : this.rules.entrySet())
            if (entry.getKey().equals(name)) {
                this.root = entry;
                break;
            }
    }


    public List<String> generateAllExpsFromGrammar(int level) {
        System.out.println("Generating from level " + level + " grammar...");
        List<String> expressions = generateAllExpsFromGrammarHelper(level);

        // Force the expressions to return a boolean
        for (Iterator<String> iterator = expressions.iterator(); iterator.hasNext(); ) {
            String exp = iterator.next();
            if (!returnTypeOf(exp).equals(BOOLEAN))
                iterator.remove();
        }

        return expressions;
    }

    public List<String> generateNExpsFromGrammar(int n) {
        // Build the N expressions
        List<String> expressions = new ArrayList<String>();
        while (expressions.size() < n)
            try {
                String exp = generateExpFromGrammar(null, 0);
                if(!expressions.contains(exp))
                    expressions.add(exp);
            } catch (TooLongException ex) {
            }
        return expressions;
    }

    private String generateExpFromGrammar(String output, int level) throws TooLongException {
        //System.out.println(output);
        Map.Entry<String, Rule> rule = null;
        if (output == null)
            // pick a grammar rule that returns a boolean
            rule = getRuleForOutput(BOOLEAN, level);
        else
            rule = getRuleForOutput(output, level);

        if (rule.getValue().inputs == null || rule.getValue().inputs.length == 0)
            return rule.getKey();
        else if (rule.getValue().inputs.length == 1)
            return rule.getKey() + "(" + generateExpFromGrammar(rule.getValue().inputs[0], level + 1) + ")";
        else
            return rule.getKey() + "(" + generateExpFromGrammar(rule.getValue().inputs[0], level + 1) + "," +
                    generateExpFromGrammar(rule.getValue().inputs[1], level + 1) + ")";
    }

    private Map.Entry<String, Rule> getRuleForOutput(String output, int level) throws TooLongException {
        // Early termination if the expression is too deep
        double terminationProb = ((double) level) / (double) (3 + level);
        if (Math.random() < terminationProb && output.equals(root.getValue().output))
            return root;

        if (level > 5)
            throw new TooLongException();

        // Random rule pick
        int rand = (int) (Math.random() * this.ruleOutputCount.get(output));
        int i = 0;
        for (Map.Entry<String, Rule> entry : this.rules.entrySet())
            if (entry.getValue().output.equals(output)) {
                if (rand == i)
                    return entry;
                i++;
            }
        return null;
    }

    private List<String> generateAllExpsFromGrammarHelper(int level) {
        Map<String, Rule> absRules = new HashMap<String, Rule>();
        absRules.putAll(rules);
        if (level != 0)
            for (String expression : generateAllExpsFromGrammarHelper(level - 1))
                absRules.put(expression, new Rule(null, returnTypeOf(expression)));


        List<String> expressions = new ArrayList<String>();
        for (Map.Entry<String, Rule> entry : absRules.entrySet())
            if (entry.getValue().inputs == null) {
                expressions.addAll(generatePossibleExpressions(entry, null));
                for (Map.Entry<String, Rule> entry2 : absRules.entrySet())
                    if (entry2.getValue().inputs == null && !entry2.getKey().equals(entry.getKey()))
                        expressions.addAll(generatePossibleExpressions(entry, entry2));
            }

        return expressions;
    }

    private String returnTypeOf(String exp) {
        if (!exp.contains("("))
            return rules.get(exp).output;
        int endOp = exp.indexOf("(");
        String symbol = exp.substring(0, endOp);
        return rules.get(symbol).output;
    }

    private List<String> generatePossibleExpressions(Map.Entry<String, Rule> subExp1, Map.Entry<String, Rule> subExp2) {
        List<String> expressions = new ArrayList<String>();
        if (subExp2 == null) {
            for (Map.Entry<String, Rule> entry : this.rules.entrySet()) {
                if (entry.getValue().inputs != null && entry.getValue().inputs.length == 1 && entry.getValue().isApplicableOn(subExp1.getValue()))
                    expressions.add(entry.getKey() + "(" + subExp1.getKey() + ")");
            }
        } else {
            for (Map.Entry<String, Rule> entry : this.rules.entrySet()) {
                if (entry.getValue().inputs != null && entry.getValue().inputs.length == 2 &&
                        entry.getValue().isApplicableOn(subExp1.getValue(), subExp2.getValue()))
                    expressions.add(entry.getKey() + "(" + subExp1.getKey() + "," + subExp2.getKey() + ")");
            }
        }
        return expressions;
    }

    protected class Rule {
        public final String[] inputs;
        public final String output;

        public Rule(String[] inputs, String output) {
            this.inputs = inputs;
            this.output = output;
        }

        public boolean isApplicableOn(Rule other) {
            if (inputs.length != 1)
                return false;
            return inputs[0].equals(other.output);
        }

        public boolean isApplicableOn(Rule other, Rule other2) {
            if (inputs.length != 2)
                return false;
            return inputs[0].equals(other.output) && inputs[1].equals(other2.output);
        }
    }

    public static void main(String[] args) {
        GrammarParser gp = new GrammarParser();

        gp.addRule("Agent", "place");
        gp.addRule("EAST", "place", "place");
        gp.addRule("WEST", "place", "place");
        gp.addRule("NORTH", "place", "place");
        gp.addRule("SOUTH", "place", "place");
        gp.addRule("EMPTY", "place", BOOLEAN);
        gp.addRule("AND", new String[]{BOOLEAN, BOOLEAN}, BOOLEAN);
        gp.addRule("OR", new String[]{BOOLEAN, BOOLEAN}, BOOLEAN);
        gp.addRule("NOT", BOOLEAN, BOOLEAN);

        ExpressionParser expp = new ExpressionParser("Agent") {
            @Override
            public Object evaluateOperator(String symbol, Object[] args) {
                if (symbol.equals("AND"))
                    return (Boolean) args[0] && (Boolean) args[1];
                else if (symbol.equals("OR"))
                    return (Boolean) args[0] || (Boolean) args[1];
                else if (symbol.equals("NOT"))
                    return !(Boolean) args[0];
                else if (symbol.equals("EMPTY"))
                    return true;
                else if (symbol.equals("EAST"))
                    return "LOC";
                else if (symbol.equals("WEST"))
                    return "LOC";
                else if (symbol.equals("SOUTH"))
                    return "LOC";
                else if (symbol.equals("EAST"))
                    return "LOC";
                return null;
            }
        };

        List<String> expressions = gp.generateNExpsFromGrammar(20);
        for (String expression : expressions)
            System.out.println(expression + "     Evaluates to   " + expp.evaluate(expression));

        //for (int i = 0; i < 4; i++)
        //    System.out.println(i + ": " + gp.generateAllExpsFromGrammar(i).size());
    }

    private class TooLongException extends Throwable {
    }
}
