package com.champbacon.pex.impl;

import com.champbacon.pex.*;

public final class PEGByteCodeVM implements PEGMatcher, ValueStackManip
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
    private final CharMatcher[] charMatchers;

    private int pc = 0;

    private int getMatchEnd() {
        if (matchFailed) {
            return -1;
        }
        return subjectPointer;
    }

    private int subjectPointer;

    private final char[] input;

    public Object getUserParseContext() {
        return userParseContext;
    }

    public void setUserParseContext(Object userParseContext) {
        this.userParseContext = userParseContext;
    }

    private Object userParseContext;

    private boolean matchFailed = false;

    public PEGByteCodeVM(ParsingExpressionGrammar peg,
                         char[] input,
                         Object userParseContext) {
        this.instructions = peg.instructions;
        this.charMatchers = peg.charMatchers;
        this.actions = peg.actions;
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

    private void debugStack(StackEntry s) {
	if (DEBUG)
	    System.out.println(stk + " " + s);
    }

    private void opCall() {
        StackEntry e = ensure1();

        e.setCaptureHeight(captureTop);
        e.setReturnAddress(pc + 1);

	debugStack(e);

        stk++;
        pc = instructions[pc];
    }

    private void opRet() {
	    stk--;
        StackEntry s = stack[stk];
	debugStack(s);
 //        captureTop = s.getCaptureHeight();
//        subjectPointer = s.getSubjectPosition();
        pc = s.getReturnAddress();
    }

    private void opChoice() {
        StackEntry s = ensure1();
        s.setReturnAddress(instructions[pc]);
        s.setCaptureHeight(captureTop);
        s.setSubjectPosition(subjectPointer);

	    stk++;

	    debugStack(s);

        pc++;

    }

    private void opCommit() {
        stk--;
        pc = instructions[pc];
    }

    private void opPartialCommit() {
        StackEntry s = stack[stk-1];
        s.setSubjectPosition(subjectPointer);
        s.setCaptureHeight(captureTop);
        pc = instructions[pc];
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
            stk--;
            s = stack[stk];
	        //  debugStack(s);
        } while (s.isCall() && stk > 0);

	if (stk == 0) {
	    if (DEBUG) System.out.println("Grammar match failed, jumping to final instruction");

	    matchFailed = true;
	    pc = instructions.length - 1; // set to the final END instruction
	    return;
	}

        subjectPointer = s.getSubjectPosition();
        captureTop = s.getCaptureHeight();
        pc = s.getReturnAddress();
    }

    private void opMatchChar() {
        int ch = instructions[pc];
	    if (DEBUG) System.out.printf("Matching character %s", (char) ch);
        if (subjectPointer < input.length && input[subjectPointer] == ch) {
            if (DEBUG) {System.out.println("");}
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

    private void opBeginCapture() {
        StackEntry s = stack[stk-1];
        s.setCurrentCaptureBegin(subjectPointer);
    }

    private void opEndCapture() {
        StackEntry s = stack[stk-1];
        int captureBegin =  s.getCurrentCaptureBegin();
	s.clearOpenCapture();
        int captureEnd = subjectPointer;
        String cap = new String(input, captureBegin, captureEnd - captureBegin);

        if (captureTop >= captureStack.length) doubleCaptures();

        captureStack[captureTop] = cap;
        captureTop++;
    }

    private void opAction() {
        ParseAction a = actions[instructions[pc]];
        a.execute(this);
        pc++;
    }

    private void opCharset() {
        CharMatcher m = charMatchers[instructions[pc]];
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

    public int match() {
        return match(0);
    }

    public int match(int pos) {
        subjectPointer = pos;

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
                default: throw new IllegalStateException("unknown instruction: " + op + " at pc " + pc);
            }


        }

        return getMatchEnd();

    }

    public void reset() {

        unimplemented();
    }

    public int getCaptureStart() {
        StackEntry s = stack[stk - 1];
        return s.getCaptureHeight();
    }

    public int getCaptureEnd() {
        return captureTop;
    }

    public void setCaptureEnd(int i) {
	captureTop = i;
    }

    public Object[] getCurrentCaptures() {
        return captureStack;
    }

    public char[] getInput() {
        return input;
    }

    public int getInputPosition() {
        return subjectPointer;
    }

    public char getLastMatch() {
        return input[subjectPointer - 1];
    }

    public void push(Object v) {
        if (captureTop >= captureStack.length) doubleCaptures();
        captureStack[captureTop] = v;
        captureTop++;
    }

    public Object[] getCaptures() {
        if (matchFailed) {
            return null;
        }
        Object[] captures = new Object[captureTop];
        System.arraycopy(captureStack, 0, captures, 0, captureTop);
        return captures;
    }

}
