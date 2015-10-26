
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
    private int stk = 0;

    private Object[] captureStack = new Object[INITIAL_CAPTURES];
    private int captureTop = 0;

    private final int[] instructions;

    private final ParseAction[] actions;
    private final CharMatcher[] matchers;

    private int pc = 0;
    private int subjectPointer = 0;

    private final char[] input;
    private Object userParseContext;

    private boolean matchFailed = false;

    public PEGByteCodeVM(int[] instructions,
                         CharMatcher[] matchers,
                         ParseAction[] actions,
                         char[] input,
                         Object userParseContext) {
        this.instructions = instructions;
        this.actions = actions;
        this.matchers = matchers;
        this.input = input;
        this.userParseContext = userParseContext;
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
        e.setReturnAddress(pc + 1);

        stk++;
        pc = instructions[pc];
    }

    private void opRet() {
	stk--;
        StackEntry s = stack[stk];
//        captureTop = s.getCaptureHeight();
//        subjectPointer = s.getSubjectPosition();
        pc = s.getReturnAddress();
    }

    private void opChoice() {
        StackEntry s = ensure1();
        s.setReturnAddress(instructions[pc]);
        s.setCaptureHeight(captureTop);
        s.setSubjectPosition(subjectPointer);

        pc++;

    }

    private void opCommit() {
        stk--;
        pc++;
    }

    private void opPartialCommit() {
        StackEntry s = stack[stk-1];
        s.setSubjectPosition(subjectPointer);
        s.setCaptureHeight(captureTop);
    }

    // VALIDATE SEMANTICS
    private void opBackCommit() {
        stk--;
        StackEntry s = stack[stk];
        subjectPointer = s.getSubjectPosition();
        captureTop = s.getCaptureHeight();
    }

    private void opJump() {
        pc = instructions[pc];
    }

    private void opFailTwice() {
        stk--;
        opFail();
    }

    private void opFail() {
	if (DEBUG) System.out.println("Fail");

        // pop off any plain CALL frames
        StackEntry s;
        do {
	    if (DEBUG) System.out.print(stk + " ");
            stk--;
            s = stack[stk];
	    if (DEBUG) System.out.println(s);
        } while (s.isCall() && stk > 0);

	if (stk == 0) {
	    if (DEBUG) System.out.println("Grammar match failed");

	    matchFailed = true;
	    pc = instructions.length - 1; // set to the final END instruction
	    return;
	}

        subjectPointer = s.getSubjectPosition();
        captureTop = s.getCaptureHeight();
        pc = s.getSubjectPosition();
    }

    private void opMatchChar() {
        int ch = instructions[pc];
	if (DEBUG) System.out.printf("Matching character %s", (char) ch);
        if (subjectPointer < input.length && input[subjectPointer] == ch) {
	    if (DEBUG) System.out.println("");
            pc++;
            subjectPointer++;
        } else {
	    if (DEBUG) System.out.println(" no match");

            opFail();
        }
    }

/*    private void opTestChar() {
        int ch = instructions[pc];
        if (subjectPointer < input.length && input[subjectPointer] == ch) {
            pc++;
            subjectPointer++;
        } else {
            opFail();
        }
    }
    */

    private void opAny() {
        if (subjectPointer < input.length) {
            subjectPointer++;
        } else opFail();
    }

    // Determine Capture Stack shape
    private void opBeginCapture() {
        StackEntry s = stack[stk-1];
        s.setCurrentCaptureBegin(subjectPointer);
    }

    private void opEndCapture() {
        StackEntry s = stack[stk-1];
        int captureBegin =  s.getCurrentCaptureBegin();
        int captureEnd = subjectPointer;
        String cap = new String(input, captureBegin, captureEnd - captureBegin);

        if (captureTop >= captureStack.length) doubleCaptures();

        captureStack[captureTop] = cap;
        captureTop++;
    }

    private void opAction() {
        ParseAction a = actions[pc];
        // a.execute(this);
        pc++;
        unimplemented();
    }

    private void opCharset() {
        CharMatcher m = matchers[pc];
        if (subjectPointer < input.length && m.match(input[subjectPointer])) {
            pc++;
            subjectPointer++;
        } else opFail();
    }

    private void debug() {
	if (subjectPointer >= input.length) return;
        System.out.printf(
                "{:pc %3d :op %2d :subj [\"%s\" %5d] :captop %2d :stk %2d}%n",
                pc,
                instructions[pc],
                input[subjectPointer], subjectPointer,
                captureTop,
		stk);
    }

    private void unimplemented() {
        throw new UnsupportedOperationException();
    }

    public void execute() {

        vm:
        while (true) {
            if (DEBUG) debug();

            final int op = instructions[pc++];

            switch(op) {
                case OpCodes.CALL:            opCall();           break;
                case OpCodes.RET:             opRet();            break;

                case OpCodes.CHOICE:          opChoice();         break;
                case OpCodes.COMMIT:          opCommit();         break;

                case OpCodes.PARTIAL_COMMIT:  opPartialCommit();  break;
                case OpCodes.BACK_COMMIT:     opBackCommit();     break;

                case OpCodes.JUMP:            opJump();           break;

                case OpCodes.FAIL_TWICE:      opFailTwice();      break;
                case OpCodes.FAIL:            opFail();           break;
                case OpCodes.END:                                 break vm;

                case OpCodes.MATCH_CHAR:      opMatchChar();      break;
                case OpCodes.CHARSET:         opCharset();        break;
                case OpCodes.ANY:             opAny();            break;
                case OpCodes.TEST_CHAR:       unimplemented();    break;
                case OpCodes.TEST_CHARSET:    unimplemented();    break;
                case OpCodes.TEST_ANY:        unimplemented();    break;
                case OpCodes.SPAN:            unimplemented();    break;

                case OpCodes.BEGIN_CAPTURE:   opBeginCapture();   break;
                case OpCodes.END_CAPTURE:     opEndCapture();     break;
                case OpCodes.FULL_CAPTURE:    unimplemented();    break;
                case OpCodes.BEHIND:          unimplemented();    break;
                case OpCodes.END_OF_INPUT:    unimplemented();    break;

                case OpCodes.ACTION:          opAction();    break;
            }


        }

    }

    public Object[] getCaptures() {
        Object[] captures = new Object[captureTop];
        System.arraycopy(captureStack, 0, captures, 0, captureTop);
        return captures;
    }
}
