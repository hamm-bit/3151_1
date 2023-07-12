import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.locks.*;

public class BucketNode {
    public int count = 0;

    // Idx should not be used for accessing the array as it is not updated regularly.
    // for return value of search() only, its value is updated at the end of search
    // and is only valid for the immediate next operation.

    private LinkedList<Integer> bucket;
    private Condition inWrite, inRead, inPassOn;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock(), writeLock = lock.writeLock();

    public BucketNode next;

    public BucketNode() {
        bucket = new LinkedList<Integer>();
    }

    public Integer head() {
        return bucket.peekFirst() == null ? -1 : bucket.peekFirst();
    }

    private void addSorted(Integer item) {
        int idx = 0;
        while (bucket.get(idx) < item) {idx++;}
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
            if (count == 0) {
                bucket = new LinkedList<Integer>();
                bucket.add(item);
                return;
            } else if (count < 4) {
                addSorted(item);
            } else {
                addSorted(item);
                // insert this at the next node
                passOn(bucket.removeLast());
            }
            count++;
        } finally {
            writeLock.unlock();
        }
        
    }

    public boolean lookUp(Integer item) {
        boolean found = false;
        try {
            readLock.lock();
            for (int i = 0; i < count; i++) {
                if (bucket.get(i) == item) found = true;
            }
        } finally {
            readLock.unlock();
        }
        return found;
    }

    public boolean delete(Integer item) {
        // TODO: only one write can be concurrent (insert / delete)
        bucket.remove(item);
        count--;
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
        next.insert(item);
    }
}