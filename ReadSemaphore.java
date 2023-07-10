import java.util.concurrent.Semaphore;
// import java.util.concurrent.Callable;

public class ReadSemaphore {
    private Semaphore rSem;

    public ReadSemaphore(int v) {
        rSem = new Semaphore(v, true);
    }

    public void P() {
        rSem.acquireUninterruptibly();
    }

    public void V() {
        rSem.release();
    }
}
