package com.champbacon.pex.impl;

import com.champbacon.pex.CharMatcher;
import com.champbacon.pex.PEGMatcher;
import com.champbacon.pex.ParseAction;

/**
 * Created by ghadi on 11/13/15.
 */
public class ParsingExpressionGrammar {

    final int[] instructions;
    final CharMatcher[] charMatchers;
    final ParseAction[] actions;

    public ParsingExpressionGrammar(int[] instructions, CharMatcher[] charMatchers, ParseAction[] actions) {
        this.instructions = instructions;
        this.charMatchers = charMatchers;
        this.actions = actions;
    }

    public PEGMatcher matcher(char[] input, Object context) {
        return new PEGByteCodeVM(this, input, context);
    }

}
