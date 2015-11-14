package com.champbacon.pex.impl;

public interface OpCodes {

    final int CALL            = 0;
    final int RET             = 1;
    final int CHOICE          = 2;
    final int COMMIT          = 3;
    final int PARTIAL_COMMIT  = 4;
    final int BACK_COMMIT     = 5;
    final int JUMP            = 6;
    final int FAIL_TWICE      = 7;
    final int FAIL            = 8;
    final int END             = 9;

    final int MATCH_CHAR      = 10;
    final int TEST_CHAR       = 11;
    final int CHARSET         = 12;
    final int TEST_CHARSET    = 13;
    final int ANY             = 14;
    final int TEST_ANY        = 15;
    final int SPAN            = 16;

    final int BEGIN_CAPTURE   = 17;
    final int END_CAPTURE     = 18;
    final int FULL_CAPTURE    = 19;
    final int BEHIND          = 20;

    final int END_OF_INPUT    = 21;

    final int ACTION          = 22;
    // APPLY_AND_PUSH
    // PUSH
    // SET_VAL
}