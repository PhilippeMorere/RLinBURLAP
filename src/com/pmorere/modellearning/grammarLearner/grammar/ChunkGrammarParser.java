package com.pmorere.modellearning.grammarLearner.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by philippe on 19/03/15.
 */
public class ChunkGrammarParser extends GrammarParser {

    private int grammarLevel = 0;
    /**
     * Chunks are simple expressions containing no logic, such as EMPTY(EAST(Agent)) or ROCK(WEST(WEST(Agent)))*
     */
    List<String> chunks = new ArrayList<String>();

    public void addChunck(String chunk) {
        chunks.add(chunk);
    }

    public void setGrammarLevel(int level) {
        grammarLevel = level;
    }

    /**
     * This method generates N expressions from the grammar. These expressions are constructed from 2 layers of
     * logic rules, then randomly chosen chunks.
     */
    @Override
    public List<String> generateNExpsFromGrammar(int n) {
        // Build the N expressions
        List<String> expressions = new ArrayList<String>();
        int tries = 0;
        Random rn = new Random();
        while (expressions.size() < n && (expressions.size() + 1) * 3 >= tries) {
            int level = rn.nextInt(grammarLevel + 1);
            String exp = generateExpFromGrammar(level);
            if (!expressions.contains(exp))
                expressions.add(exp);
            tries++;
        }
        return expressions;
    }

    private String generateExpFromGrammar(int level) {
        if (level == 0)
            return chunks.get((int) (Math.random() * chunks.size()));

        GrammarRule rule = this.logicRules.get((int) (Math.random() * logicRules.size()));
        if (rule.inputs.length == 1)
            return rule.name + "(" + generateExpFromGrammar(level - 1) + ")";
        else if (rule.inputs.length == 2)
            return rule.name + "(" + generateExpFromGrammar(level - 1) + "," + generateExpFromGrammar(level - 1) + ")";
        else
            throw new RuntimeException("Can only generate expressions with logic rules having 1 or 2 inputs");
    }
}
