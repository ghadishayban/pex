
package com.champbacon.pex.impl;

/* stack frame contents:
  [return-address, capture-height, subject-position ...*]

*/


import java.lang.CharSequence;
import java.util.ArrayList;

import clojure.lang.IFn;

public final class PEGVirtualMachine {

    public static int INITIAL_STACK = 3 * 16;
    public static int INITIAL_CAPTURES = 4;

    private int[] stack;
    private int sp;

    private ArrayList[] captureStack = new ArrayList[Object](INITIAL_CAPTURES);
    private int captureTop = 0;

    final int[] instructions;
    final Object[] cbs;

    public PEGVirtualMachine(int[] instructions, Object[] cbs) {
        this.instructions = instructions;
        this.cbs = cbs;
    }

    private void maybeGrowStack() {
        if (sp == stack.length) {
            int[] ns = new int[stack.length*2];
            System.arraycopy(stack, 0, ns, 0, stack.length);
            stack = ns;
        }
    }

    private void maybeGrowCaptures() {
    }

    public Object[] execute(CharSequence in, Object context) {
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
    }
}
