
package com.champbacon.pex.impl;

import com.champbacon.pex.ParseAction;
import com.champbacon.pex.CharMatcher;
import com.champbacon.pex.PEGVM;

public final class PEGByteCodeVM // implements PEGVM
{

    private static boolean DEBUG = true;

    public static final int INITIAL_STACK    = 16;
    public static final int INITIAL_CAPTURES = 4;

    private StackEntry[] stack = new StackEntry[INITIAL_STACK];
    private int stk;

    private Object[] captureStack = new Object[INITIAL_CAPTURES];
    private int captureTop = 0;

    private final int[] instructions;

    private final ParseAction[] actions;
    private final CharMatcher[] matchers;

    private int pc = 0;
    private int subjectPointer = 0;

    public PEGByteCodeVM(int[] instructions, CharMatcher[] matchers, ParseAction[] actions) {
        this.instructions = instructions;
        this.actions = actions;
        this.matchers = matchers;
    }

    private final StackEntry ensure1() {
        if (stk >= stack.length) doubleStack();
        StackEntry e = stack[stk];
        if (e == null) stack[stk] = e = new StackEntry();
        return e;
    }

    private final void doubleStack() {
        StackEntry[] newStack = new StackEntry[stack.length << 1];
        System.arraycopy(stack, 0, newStack, 0, stack.length);
        stack = newStack;
    }

    private final void doubleCaptures() {
        Object[] newCaptures = new Object[captureStack.length << 1];
        System.arraycopy(captureStack, 0, newCaptures, 0, captureStack.length);
        captureStack = newCaptures;
    }

    private void opCall() {
        StackEntry e = ensure1();

        e.setCaptureHeight(captureTop);
        e.setSubjectPosition(subjectPointer);
        e.setReturnAddress(pc + 1);

        stk++;
        pc = instructions[pc];
    }

    private void opRet() {
        StackEntry s = stack[stk];
        captureTop = s.getCaptureHeight();
        subjectPointer = s.getSubjectPosition();
        pc = s.getReturnAddress();
    }

    private void debug(int op) {


    }

    public Object[] execute(char[] in, Object context) {

        while (true) {
            final int op = instructions[pc++];

            if (DEBUG) debug(op);

            switch(op) {
                case OpCodes.CALL:               opCall();     break;
                case OpCodes.RET:                opRet();      break;

                case OpCodes.CHOICE:                           break;
                case OpCodes.COMMIT:                           break;

                case OpCodes.PARTIAL_COMMIT:                   break;
                case OpCodes.BACK_COMMIT:                      break;

                case OpCodes.JUMP:                             break;

                case OpCodes.FAIL_TWICE:                       break;
                case OpCodes.FAIL:                             break;
                case OpCodes.END:                              break;

                case OpCodes.MATCH_CHAR:                       break;
                case OpCodes.TEST_CHAR:                        break;
                case OpCodes.CHARSET:                          break;
                case OpCodes.TEST_CHARSET:                     break;
                case OpCodes.ANY:                              break;
                case OpCodes.TEST_ANY:                         break;
                case OpCodes.SPAN:                             break;

                case OpCodes.BEGIN_CAPTURE:                    break;
                case OpCodes.END_CAPTURE:                      break;
                case OpCodes.FULL_CAPTURE:                     break;
                case OpCodes.BEHIND:                           break;


            }


        }

    }
}
