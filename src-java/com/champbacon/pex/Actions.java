package com.champbacon.pex;

import clojure.lang.IFn;

/**
 * Created by ghadi on 11/13/15.
 */
public class Actions {
    public static class PushAction implements ParseAction {
        final Object val;

        public PushAction(Object val) {
            this.val = val;
        }

        public void execute(ValueStackManip vm) {
            vm.push(val);
        }
    }

    public static class UpdateStackTop implements ParseAction {
        IFn f;

        public UpdateStackTop(IFn f) {
            this.f = f;
        }

        public void execute(ValueStackManip vm) {
            Object[] captures = vm.getCurrentCaptures();

            int cur = vm.getCaptureEnd();
            captures[cur] = f.invoke(captures[cur]);
        }
    }

    final static ParseAction CLEAR_STRING_BUFFER = new ParseAction() {
        public void execute(ValueStackManip vm) {
            vm.setUserParseContext(new StringBuffer());
        }
    };

    final static ParseAction APPEND_STRING_BUFFER = new ParseAction() {
        public void execute(ValueStackManip vm) {
            StringBuffer buf = (StringBuffer) vm.getUserParseContext();
            char[] input = vm.getInput();
            buf.append(input[vm.getInputPosition() - 1]);
        }
    };

    final static ParseAction PUSH_STRING_BUFFER = new ParseAction() {
        public void execute(ValueStackManip vm) {
            vm.push(vm.getUserParseContext().toString());
        }
    };

}
