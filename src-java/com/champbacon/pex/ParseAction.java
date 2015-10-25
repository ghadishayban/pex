package com.champbacon.pex;

public interface ParseAction {

    public void execute(PEGVM vm);
    // subjectPosition, context, captureList, capturePosition
}