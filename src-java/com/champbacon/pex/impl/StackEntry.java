package com.champbacon.pex.impl;

final class StackEntry {

    private int returnAddress;
    private int subjectPosition;
    private int captureHeight;

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
}