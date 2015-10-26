
package com.champbacon.pex;

public interface PEGVM {
    public Object getUserParseContext();
    public void setUserParseContext(Object ctx);

    public int getInputPosition();
    public char[] getInput();

    public int getCaptureStart();
    public int getCaptureEnd();
    public Object[] getCurrentCaptures();

    public Object[] getCaptures();

    public void push(Object v);

    public int match();
    public int match(int pos);
}