package de.hpi.tdgt.fibers;

public class Fiber {
    static {
        System.loadLibrary("native");
        init_fibers();
    }
    private static native void init_fibers();
    // Declare a native method sayHello() that receives no arguments and returns void
    private native int create_fiber();
    private int id;
    public Fiber(){
        id = create_fiber();
    }

    private native void run_fiber(int id, Runnable runnable);

    public void switch_to(Runnable runnable){
        run_fiber(this.id, runnable);
    }
}
