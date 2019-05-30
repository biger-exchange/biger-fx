package com.biger.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Utils {
    public static List<Throwable> getCausalChain(Throwable throwable) {
        List<Throwable> causes = new ArrayList<>(4);
        causes.add(throwable);

        // Keep a second pointer that slowly walks the causal chain. If the fast pointer ever catches
        // the slower pointer, then there's a loop.
        Throwable slowPointer = throwable;
        boolean advanceSlowPointer = false;

        Throwable cause;
        while ((cause = throwable.getCause()) != null) {
            throwable = cause;
            causes.add(throwable);

            if (throwable == slowPointer) {
                throw new IllegalArgumentException("Loop in causal chain detected.", throwable);
            }
            if (advanceSlowPointer) {
                slowPointer = slowPointer.getCause();
            }
            advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
        }
        return Collections.unmodifiableList(causes);
    }
}
