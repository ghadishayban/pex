package com.champbacon.pex.impl;

import com.champbacon.pex.CharMatcher;

/**
 * Created by ghadi on 11/13/15.
 *
 * TODO Add a mask-based implementation & some combinators
 * https://github.com/sirthias/parboiled2/blob/master/parboiled-core/src/main/scala/org/parboiled2/CharPredicate.scala#L145
 */

public class Matchers {
    public static class SingleRangeMatcher implements CharMatcher {

        final int low;
        final int high;

        public SingleRangeMatcher(int low, int high) {
            if (high <= low + 1)
                throw new IllegalArgumentException("low must be <= high");

            this.low = low;
            this.high = high;
        }

        public boolean match(int ch) {
            if (ch < low) {
                return false;
            }

            if (ch < high) {
                return true;
            }
            return false;
        }
    }


    public static class RangeMatcher implements CharMatcher {

        final int[] chars;

        public RangeMatcher(int[] chars) {
            this.chars = chars;
        }

        public boolean match(int ch) {

            // Peek at the first few pairs.
            // Should handle ASCII well.
            for (int j = 0; j < chars.length && j <= 8; j += 2) {
                if (ch < chars[j]) {
                    return false;
                }
                if (ch < chars[j + 1]) {
                    return true;
                }
            }

            // Otherwise binary search.
            for (int lo = 0, hi = chars.length / 2; lo < hi; ) {
                int m = lo + (hi - lo) / 2;
                int c = chars[2 * m];
                if (c <= ch) {
                    if (ch < chars[2 * m + 1]) {
                        return true;
                    }
                    lo = m + 1;
                } else {
                    hi = m;
                }
            }
            return false;

        }
    }


}
