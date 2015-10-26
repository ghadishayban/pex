package com.champbacon.pex;

public interface ValueStackManip {
    public Object getUserParseContext();
    public void setUserParseContext(Object ctx);

    public int getInputPosition();
    public char[] getInput();

    public int getCaptureStart();
    public int getCaptureEnd();
    public int setCaptureEnd(int i);
    public Object[] getCurrentCaptures();
    public void push(Object v);
}
