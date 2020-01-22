package de.hpi.tdgt.fibers;

public class Fiber {
    static {
        System.loadLibrary("native");
    }

    // Declare a native method sayHello() that receives no arguments and returns void
    public native void sayHello();
}
