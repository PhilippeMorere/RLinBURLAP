package com.pmorere.modellearning.grammar;

import burlap.behavior.statehashing.StateHashTuple;

/**
 * Created by philippe on 12/03/15.
 */
public abstract class ExpressionParser {
    protected Object root;
    protected StateHashTuple sh;

    public ExpressionParser(Object root) {
        this.root = root;
    }

    public abstract Object evaluateOperator(String symbol, Object[] args);

    public Object evaluateOperatorHelper(String symbol, Object[] args) {
        if (symbol.equals("AND"))
            return (Boolean) args[0] && (Boolean) args[1];
        else if (symbol.equals("OR"))
            return (Boolean) args[0] || (Boolean) args[1];
        else if (symbol.equals("NOT"))
            return !(Boolean) args[0];
        else {
            Object res = evaluateOperator(symbol, args);
            if (res == null)
                throw new RuntimeException("Function evaluateOperator should implement computation for symbol " + symbol);
            return res;
        }
    }

    public boolean evaluate(String exp) {
        return (Boolean) evaluateHelper(exp);
    }

    public void setStateHashTuple(StateHashTuple sh) {
        this.sh = sh;
    }

    private Object evaluateHelper(String exp) {
        if (!exp.contains("("))
            return root;
        int endOp = exp.indexOf("(");
        String symbol = exp.substring(0, endOp);
        String subExp = exp.substring(endOp + 1, exp.length() - 1);
        int parenthesisCount = 0;
        for (int i = 0; i < subExp.length(); i++) {
            if (subExp.charAt(i) == '(')
                parenthesisCount++;
            else if (subExp.charAt(i) == ')')
                parenthesisCount--;
            else if (subExp.charAt(i) == ',' && parenthesisCount == 0)
                return evaluateOperatorHelper(symbol, new Object[]{
                        evaluateHelper(subExp.substring(0, i)), evaluateHelper(subExp.substring(i + 1, subExp.length()))});
        }

        return evaluateOperatorHelper(symbol, new Object[]{evaluateHelper(subExp)});
    }

    public static void main(String[] args) {

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

        boolean result = expp.evaluate("AND(NOT(EMPTY(EAST(Agent))),OR(EMPTY(WEST(Agent)),EMPTY(EAST(Agent))))");

        System.out.println(result);
    }

}
