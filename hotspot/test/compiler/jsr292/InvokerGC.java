/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8067247
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /compiler/whitebox /
 * @run main/bootclasspath/othervm -Xcomp -Xbatch
 *      -XX:CompileCommand=compileonly,InvokerGC::test
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      InvokerGC
 */

import java.lang.invoke.*;
import sun.hotspot.WhiteBox;

public class InvokerGC {
    static final WhiteBox WB = WhiteBox.getWhiteBox();

    static MethodHandle mh;
    static {
        try {
            mh = MethodHandles.lookup().findStatic(InvokerGC.class, "dummy", MethodType.methodType(void.class));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static void dummy() {}

    static void test() {
        try {
            mh.invoke();
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    public static void main(String[] args) throws Throwable {
        mh.invoke(); // Pre-generate an invoker for ()V signature

        test(); // trigger method compilation
        test();

        WB.fullGC(); // WB.fullGC has always clear softref policy.

        test();

        WB.clearInlineCaches(true); // Preserve static stubs.

        test(); // Trigger call site re-resolution. Invoker LambdaForm should stay the same.

        System.out.println("TEST PASSED");
    }
}
