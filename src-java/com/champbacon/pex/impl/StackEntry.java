package com.champbacon.pex.impl;

final class StackEntry {

    static final int NO_OPEN_CAPTURE = -1;
    static final int IS_CALL = -1;
    private int returnAddress;
    private int subjectPosition = IS_CALL;
    private int captureHeight;
    private int currentCaptureBegin = NO_OPEN_CAPTURE;

    public int getCaptureHeight() {
        return captureHeight;
    }

    public void setCaptureHeight(int captureHeight) {
        this.captureHeight = captureHeight;
    }

    public int getSubjectPosition() {
        return subjectPosition;
    }

    public void setSubjectPosition(int subjectPosition) {
        this.subjectPosition = subjectPosition;
    }

    public int getReturnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(int returnAddress) {
        this.returnAddress = returnAddress;
    }

    public boolean isCall() {
        return subjectPosition == -1;
    };

    public void setCurrentCaptureBegin(int subjectPosition) {
        if (currentCaptureBegin == NO_OPEN_CAPTURE) {
            currentCaptureBegin = subjectPosition;
        } else throw new IllegalStateException("Nested capture within a single rule.");
    }

    public void clearOpenCapture() {
	currentCaptureBegin = NO_OPEN_CAPTURE;
    }

    public int getCurrentCaptureBegin() {
        if (currentCaptureBegin != NO_OPEN_CAPTURE)
            return currentCaptureBegin;
        else throw new IllegalStateException("No open capture.");
    }

    public String toString() {
	return String.format("#frame {:sub %5s :captop %3s :ret %5s}",
			     subjectPosition,
			     captureHeight,
			     returnAddress);
    }
}
