import clojure.lang.IFn;

package com.champbacon.pex.impl;

public class PEGVirtualMachine {
    // stack;
    captures;
    captureTop;

    final long[] instructions;
    final IFn[] actions;
    
    public PEGVirtualMachine(long[] instructions, IFn[] actions, long[] charSpans) {
	this.instructions = instructions;
	this.actions = actions; 
    }


    public execute(char[] in, Object context) {
	// stack
	// ValueStack
	int pc = 0;
	int subject = 0;

	for(;;) {
	    long op = instructions[pc];
	    switch(op) {
	    case 0:  // call
	    case 1:  // ret
	    case 2:  // choice
	    case 3:  // commit
	    case 4:  // partial-commit
	    case 5:  // back-commit
	    case 6: // jump
	    case 7: // fail-twice
	    case 8: // fail
	    case 9: // end

	    case 10:  // char
	    case 11:  // test-char
	    case 12:  // charset
	    case 13:  // test-charset
	    case 14: // any
	    case 15: // test-any
	    case 16: // span

	    case 17: // begin-capture
	    case 18: // end-capture
	    case 19: // full-capture

	    case 20: // behind


	    default:
		
	    }
	}
    }
}
