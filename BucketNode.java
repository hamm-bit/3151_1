import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.locks.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.BlockingQueue;

public class BucketNode {
    public int count = 0;

    // Idx should not be used for accessing the array as it is not updated regularly.
    // for return value of search() only, its value is updated at the end of search
    // and is only valid for the immediate next operation.

    private LinkedList<Integer> bucket;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock(), writeLock = lock.writeLock();
    private Semaphore WSem = new Semaphore(1);
    // DSem = new Semaphore(1);
    // private Semaphore RSem = new Semaphore(1);
    
    // private BlockingQueue<Integer> q = new LinkedBlockingDeque<>();
    public BucketNode next = null;
    public BucketArray higher;
    
    // begin .text
    // ==========================================

    public BucketNode(BucketArray array) {
        bucket = new LinkedList<Integer>();
        higher = array;
    }

    public Integer head() {
        return bucket.peekFirst() == null ? -1 : bucket.peekFirst();
    }

    private void addSorted(Integer item) {
        int idx = 0;
        while (idx < count && bucket.get(idx) < item) {idx++;}
        bucket.add(idx, item);
    }

    
    // /**
    //  * Pass-on insert, falls under same shared condition and queue write
    //  * only erectable by other BucketNodes
    //  */
    // public void passOnInsert(Integer item) {
    //     try {
    //         // TODO: change this to a semaphore/listening monitor
    //         writeLock.lock();

    //     } finally {

    //     }
    // }


    public void insert(Integer item) {
        // TODO: change this to a semaphore/listening monitor
        try {
            writeLock.lock();

            // if consecutive writes, kept lock until no more consecutive
            /*
             * first_item = q.pop();
             *  
             *  do {
             */
        
            System.out.println("node_insert: writelock engaged");

            // WSem.acquireUninterruptibly();

            System.out.println("node_insert: mutex acquired");

            if (count == 0) {

                System.out.println("node_insert: case empty bucket");

                bucket = new LinkedList<Integer>();
                bucket.add(item);

            } else if (count < 4) {
                addSorted(item);
            } else {
                addSorted(item);
                // insert this at the next node
                passOn(bucket.removeLast());
                count--;
            }


            System.out.println("node_insert: node insertion complete");


            count++;
            // if consecutive writes, kept lock until no more consecutive
            // } while (q.peekFirst().isWrite());

        } finally {
            // WSem.release();
            writeLock.unlock();
        }
        
        System.out.println("node_insert: writelock Unlocked");

    }

    public boolean lookUp(Integer item) {
        // ensure that no write operations are taking place
        readLock.lock();
        
        boolean found = false;
        // unlimited concurrency
        try {
            System.out.println("node_lookUp: lock acquired");
            System.out.println(head());
            found = bucket.contains(item);
            // for (int i = 0; i < count; i++) {
            //     System.out.println("nonde_lookUp: searching");
            //     if (bucket.get(i).equals(item)) found = true;
            // }
        } finally {
            readLock.unlock();
        }
        return found;
    }

    public boolean delete(Integer item) {
        // TODO: only one write can be concurrent (insert / delete)
        writeLock.lock();
        // mutex sem
        try {
            System.out.println("node_delete: lock acquired");
            // remove the item in a traditional fashion
            // for some reason LL.remove behaves bizarrely for non-existing element
            if (bucket.remove( (Integer) item )) {
                System.out.println("node_delete: item removed");
                count--;
            } else {
                System.out.println("node_delete: item not present");
            }

        } finally {
            writeLock.unlock();
        }
        return count == 0;
    }

    // public Integer insertHead(Integer item) {
    //     if (count < 4) {
    //         bucket.addFirst(item);
    //     } else {
    //         Integer tail = bucket.removeLast();
    //         // insert the tail node at the next node
    //         return passOn(tail);
    //     }
    // }

    public void passOn(Integer item) {
        // queue this as a write to the next bucket
        if (next == null) {
            // signal main function to insert
            // this has the advantage of no need to create a new bucket,
            // since one may have already been created during this call.
            higher.appendInsert(item);
            return;
        }
        next.insert(item);
    }

    public boolean isFullAndTail() {
        return count == 4 && next == null;
    }

    public void print() {
        readLock.lock();
        System.out.println("bucket: ");
        try {
            bucket.stream().forEach(item -> System.out.printf("%d ",item));
            System.out.println();
        } finally {
            readLock.unlock();
        }
    }
}
