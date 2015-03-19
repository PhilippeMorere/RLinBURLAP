package com.pmorere.modellearning.grammarLearner.grammar;

import java.util.*;

/**
 * Created by philippe on 12/03/15.
 */
public class GrammarParser {

    /**
     * Contains all the grammar rules *
     */
    protected Map<String, GrammarRule> rules;

    /**
     * Contains all the logic grammar rules such as AND, OR, NOT, ...*
     */
    protected List<GrammarRule> logicRules;

    /**
     * Keeping track of the number of rules that share a common output*
     */
    protected Map<String, Integer> ruleOutputCount;

    /**
     * Entry rule for every expression, such as entry rule has no input *
     */
    protected GrammarRule entryRule;

    /**
     * Defining a few common logic rules*
     */
    public static final String BOOLEAN = "boolean";

    public GrammarParser() {
        rules = new HashMap<String, GrammarRule>();
        logicRules = new ArrayList<GrammarRule>();
        ruleOutputCount = new HashMap<String, Integer>();
        ruleOutputCount.put(BOOLEAN, 0);
    }

    public void addRule(String name, String input, String output) {
        this.addRule(name, new String[]{input}, output);
    }

    public void addLogic(GrammarRule logicRule) {
        this.addLogic(logicRule);
        ruleOutputCount.put(BOOLEAN, ruleOutputCount.get(BOOLEAN) + 1);
    }

    public void addRule(String name, String[] inputs, String output) {
        this.rules.put(name, new GrammarRule(name, inputs, output));
        if (!this.ruleOutputCount.containsKey(output))
            this.ruleOutputCount.put(output, 0);
        this.ruleOutputCount.put(output, this.ruleOutputCount.get(output) + 1);
    }

    public void addRule(String name, String output) {
        this.addRule(name, (String[]) null, output);
        for (Map.Entry<String, GrammarRule> entry : this.rules.entrySet())
            if (entry.getKey().equals(name)) {
                this.entryRule = entry.getValue();
                break;
            }
    }

    public List<String> generateAllExpsFromGrammar(int level) {
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
                if (!expressions.contains(exp))
                    expressions.add(exp);
            } catch (TooLongException ex) {
            }
        return expressions;
    }

    private String generateExpFromGrammar(String output, int level) throws TooLongException {
        //System.out.println(output);
        GrammarRule rule = null;
        if (output == null)
            // pick a grammar rule that returns a boolean
            rule = getRuleForOutput(BOOLEAN, level);
        else
            rule = getRuleForOutput(output, level);

        if (rule.inputs == null || rule.inputs.length == 0)
            return rule.name;
        else if (rule.inputs.length == 1)
            return rule.name + "(" + generateExpFromGrammar(rule.inputs[0], level + 1) + ")";
        else
            return rule.name + "(" + generateExpFromGrammar(rule.inputs[0], level + 1) + "," +
                    generateExpFromGrammar(rule.inputs[1], level + 1) + ")";
    }

    private GrammarRule getRuleForOutput(String output, int level) throws TooLongException {
        // Early termination if the expression is too deep
        double terminationProb = ((double) level) / (double) (3 + level);
        if (Math.random() < terminationProb && output.equals(entryRule.output))
            return entryRule;

        if (level > 5)
            throw new TooLongException();

        // Random rule pick
        int rand = (int) (Math.random() * this.ruleOutputCount.get(output));
        int i = 0;
        for (Map.Entry<String, GrammarRule> entry : this.rules.entrySet())
            if (entry.getValue().output.equals(output)) {
                if (rand == i)
                    return entry.getValue();
                i++;
            }
        return null;
    }

    private List<String> generateAllExpsFromGrammarHelper(int level) {
        Map<String, GrammarRule> absRules = new HashMap<String, GrammarRule>();
        absRules.putAll(rules);
        if (level != 0)
            for (String expression : generateAllExpsFromGrammarHelper(level - 1))
                absRules.put(expression, new GrammarRule(expression, null, returnTypeOf(expression)));


        List<String> expressions = new ArrayList<String>();
        for (Map.Entry<String, GrammarRule> entry : absRules.entrySet())
            if (entry.getValue().inputs == null) {
                expressions.addAll(generatePossibleExpressions(entry, null));
                for (Map.Entry<String, GrammarRule> entry2 : absRules.entrySet())
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

    private List<String> generatePossibleExpressions(Map.Entry<String, GrammarRule> subExp1, Map.Entry<String, GrammarRule> subExp2) {
        List<String> expressions = new ArrayList<String>();
        if (subExp2 == null) {
            for (Map.Entry<String, GrammarRule> entry : this.rules.entrySet()) {
                if (entry.getValue().inputs != null && entry.getValue().inputs.length == 1 && entry.getValue().isApplicableOn(subExp1.getValue()))
                    expressions.add(entry.getKey() + "(" + subExp1.getKey() + ")");
            }
        } else {
            for (Map.Entry<String, GrammarRule> entry : this.rules.entrySet()) {
                if (entry.getValue().inputs != null && entry.getValue().inputs.length == 2 &&
                        entry.getValue().isApplicableOn(subExp1.getValue(), subExp2.getValue()))
                    expressions.add(entry.getKey() + "(" + subExp1.getKey() + "," + subExp2.getKey() + ")");
            }
        }
        return expressions;
    }

    public List<String> generateAllExpsFromSubExpression(String subExpression) {
        List<String> otherSubExp = generateAllExpsFromGrammar(2);
        List<String> exps = new ArrayList<String>();
        for (Map.Entry<String, GrammarRule> entry : this.rules.entrySet())
            if (entry.getValue().inputs != null && entry.getValue().inputs.length == 2 &&
                    entry.getValue().inputs[0].equals(BOOLEAN) && entry.getValue().inputs[1].equals(BOOLEAN))
                for (String other : otherSubExp)
                    exps.add(entry.getKey() + "(" + subExpression + "," + other + ")");
        return exps;
    }

    public static void main(String[] args) {
        GrammarParser gp = new GrammarParser();

        gp.addRule("Agent", "place");
        gp.addRule("EAST", "place", "place");
        gp.addRule("WEST", "place", "place");
        gp.addRule("NORTH", "place", "place");
        gp.addRule("SOUTH", "place", "place");
        gp.addRule("EMPTY", "place", BOOLEAN);
        gp.addLogic(GrammarRule.LOGIC_RULE_AND);
        gp.addLogic(GrammarRule.LOGIC_RULE_OR);
        gp.addLogic(GrammarRule.LOGIC_RULE_NOT);

        ExpressionParser expp = new ExpressionParser("Agent") {
            @Override
            public Object evaluateOperator(String symbol, Object[] args) {
                if (symbol.equals("EMPTY"))
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

        List<String> bigExps = gp.generateAllExpsFromSubExpression("AND(EMPTY(Agent),OR(EMPTY(Agent),EMPTY(NORTH(Agent))))");
        for (String expression : bigExps)
            System.out.println(expression);
    }

    private class TooLongException extends Throwable {
    }
}
