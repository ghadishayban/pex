
package com.champbacon.pex;

public interface PEGMatcher {

    public int match();
    public int match(int pos);

    public void reset();

    public Object[] getCaptures();
}
