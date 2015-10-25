
package com.champbacon.pex.impl;

// import java.lang.CharSequence;
import com.champbacon.pex.ParseAction;
import com.champbacon.pex.CharMatcher;
import com.champbacon.pex.PEGVM;

public final class PEGByteCodeVM // implements PEGVM
{

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

    public PEGByteCodeVM(int[] instructions, CharMatcher[] matchers, ParseAction[] actions) {
        this.instructions = instructions;
        this.actions = actions;
        this.matchers = matchers;
    }

/*    public Object[] execute(CharSequence in, Object context) {
        // stack
        // ValueStack
        int pc = 0; // Fail marker when -1
        int subjectPosition;
        int captureBottom = 0;
        int captureTop = 0;

        vm:
        for(;;) {
            int op = instructions[pc];

            switch(op) {

                case 0:  // call
                    maybeGrowStack();

                    int target = instructions[pc+1];
                    int returnAddress = instructions[pc+2];

                    stack[sp] = returnAddress;
                    pc = target;

                case 1:  // ret


                case 2:  // choice
                case 3:  // commit
                case 4:  // partial-commit
                case 5:  // back-commit
                case 6: // jump
                    pc = instructions[pc+1];
                    break;
                case 7: // fail-twice
                    sp -= 3;
                    // continue, don't break
                case 8: // fail
                    pc = -1;
                    break;
                case 9: // end

                case 10:  // char
                    int testChar = instructions[pc+1];
                    int subjectPos = stack[sp]
                    int inputChar = in.charAt(subjectPosition);
                    if (inputChar == testChar) {
                        pc = -1;
                    } else {
                        pc = pc + 2;
                    }
                    break;
                case 11:  // test-char
                case 12:  // charset
                    context[]
                case 13:  // test-charset
                case 14: // any
                case 15: // test-any
                case 16: // span


                case 17: // begin-capture
                case 18: // end-capture
                case 19: // full-capture

                case 20: // behind
                case 21: // action
                    // IFn action = (IFn) cbs[instructions[pc+1]];
                    captureStack.subList()
                    // action.invoke(null, ); // parser, context

                default:

            }

            // backtrack?
            if (pc == -1) {
                // pop stack frames

            }

            }
        }
    } */
}
