package com.pmorere.modellearning.grammarLearner.grammar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippe on 19/03/15.
 */
public class ChunkGrammarParser extends GrammarParser {

    /**
     * Chunks are simple expressions containing no logic, such as EMPTY(EAST(Agent)) or ROCK(WEST(WEST(Agent)))*
     */
    List<String> chunks;

    public void addChunck(String chunk) {
        chunks.add(chunk);
    }

    /**
     * This method generates N expressions from the grammar. These expressions are constructed from 2 layers of
     * logic rules, then randomly chosen chunks.
     */
    @Override
    public List<String> generateNExpsFromGrammar(int n) {
        // Build the N expressions
        List<String> expressions = new ArrayList<String>();
        while (expressions.size() < n) {
            String exp = generateExpFromGrammar(2);
            if (!expressions.contains(exp))
                expressions.add(exp);
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
