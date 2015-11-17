package com.champbacon.pex.impl;

import clojure.lang.IFn;
import com.champbacon.pex.ParseAction;
import com.champbacon.pex.ValueStackManip;

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

            int cur = vm.getCaptureEnd() - 1;
            captures[cur] = f.invoke(captures[cur]);
        }
    }

    public static class FoldCaptures implements ParseAction {
        private final IFn f;

        public FoldCaptures(IFn f) {
            this.f = f;
        }

        public void execute(ValueStackManip vm) {
            int low = vm.getCaptureStart();
            int high = vm.getCaptureEnd();
            Object[] caps = vm.getCurrentCaptures();

            Object ret = f.invoke();
            for(int i = low; i<high; i++) {
                ret = f.invoke(ret, caps[i]);
            }
            vm.setCaptureEnd(low);
            vm.push(f.invoke(ret));
        }
    }

    public static class ReplaceCaptures implements ParseAction {
        private final IFn f;

        public ReplaceCaptures(IFn f) {
            this.f = f;
        }

        public void execute(ValueStackManip vm) {
            int low = vm.getCaptureStart();
            int high = vm.getCaptureEnd();
            Object[] caps = vm.getCurrentCaptures();

            Object ret = f.invoke(caps, low, high);
            vm.setCaptureEnd(low);
            vm.push(ret);
        }
    }

    public final static ParseAction CLEAR_STRING_BUFFER = new ParseAction() {
        public void execute(ValueStackManip vm) {
            // or allocate a new one
            ((StringBuffer) vm.getUserParseContext()).setLength(0);
        }
    };

    public final static ParseAction APPEND_STRING_BUFFER = new ParseAction() {
        public void execute(ValueStackManip vm) {
            StringBuffer buf = (StringBuffer) vm.getUserParseContext();
            char[] input = vm.getInput();
            buf.append(input[vm.getInputPosition() - 1]);
        }
    };

    public final static ParseAction PUSH_STRING_BUFFER = new ParseAction() {
        public void execute(ValueStackManip vm) {
            vm.push(vm.getUserParseContext().toString());
        }
    };

}
