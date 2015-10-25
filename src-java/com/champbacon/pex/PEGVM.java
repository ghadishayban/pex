
package com.champbacon.pex;

public interface PEGVM {
    public Object getContext();
    public Object setContext(Object c);

    public Object getInput();

    public int getInputPosition();

    public int getCapturePosition();
    public Object[] getCaptureStack();
}