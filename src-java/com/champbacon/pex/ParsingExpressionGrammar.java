package com.champbacon.pex;

import com.champbacon.pex.CharMatcher;
import com.champbacon.pex.PEGMatcher;
import com.champbacon.pex.ParseAction;
import com.champbacon.pex.impl.PEGByteCodeVM;

/**
 * Created by ghadi on 11/13/15.
 */
public class ParsingExpressionGrammar {

    public final int[] instructions;
    public final CharMatcher[] charMatchers;
    public final ParseAction[] actions;

    public ParsingExpressionGrammar(int[] instructions, CharMatcher[] charMatchers, ParseAction[] actions) {
        this.instructions = instructions;
        this.charMatchers = charMatchers;
        this.actions = actions;
    }

    public PEGMatcher matcher(char[] input, Object context) {
        return new PEGByteCodeVM(this, input, context);
    }

}
