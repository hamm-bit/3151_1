import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;

import java.util.concurrent.locks.*;
import java.util.concurrent.Semaphore;
import java.util.stream.*;

public class BucketArray {
    public static final int INIT_SIZE = 1024;
    public int size = INIT_SIZE;
    private ArrayList<BucketNode> buckets;
    private Semaphore delSem = new Semaphore(1, true);
    private ReadWriteLock arrLock = new ReentrantReadWriteLock();
    private Condition inSearch = arrLock.readLock().newCondition(),
                      inWrite = arrLock.writeLock().newCondition(),
                      inPrint = arrLock.readLock().newCondition();
    private int numSearch = 0, numWrite = 0;

    public BucketArray() {
        buckets = new ArrayList<BucketNode>();
    }

    public void insert(Integer item) {
        BucketNode bucket = search(item);
        // Queue starts here
        // TODO: implement semaphore for holding the read and write pointers in order

        bucket.insert(item);
        size++;
        cleanup();
    }

    public void delete(Integer item) {
        BucketNode bucket = search(item);
        bucket.delete(item);

            /*
             * NOTE: new idea HAS BEEN IMPLEMENTED
             * utilize the search function's latest updates to perform a one-dim nearest neighbour search
             * since each search would yield local space
             * This does mean that each cleanup() would also have to update the index of each bucket.
             */
            

            // updateNearest should also have a binary semaphore
            // buckets must not vanish when deleting
            // new buckets can only be appended so it wouldn't affect the correctness
            // TODO: implement a lock for bucket deletion
            // TODO: implement an update function that stays sensitive to the 
    }
    

    private void cleanup() {
        // TODO: a semaphore for one cleanup() concurrent at a time
        /**
         * TODO:
         * also implement a lock which all pointers that queues the cleanup method
         * cleanup() should wait for all searches to finish
         * and blocks all following search until cleanup has completed
         */

        // All buckets after the cleanup should have nearestLeft = nearestRight = 0

        try {
            // FIFO delete op
            delSem.acquireUninterruptibly();
            // wait until all search finishes
            while (numSearch != 0) inWrite.awaitUninterruptibly();
            
            // TODO: cleanup() main body
            for (int i = size; i >= 0; i--) {
                BucketNode curr = buckets.get(i), prevNext = null;
                // inductively update the link to the next node each time.
                while (curr.count == 0) {curr = buckets.get(i); i--;}
                curr.next = prevNext;
                prevNext = curr;
            }

            buckets.removeIf(node -> (node.count == 0));
            // This is quadratic time complexity
            // however there's no good way to delete a chunk off an array in linear time
        } finally {
            delSem.release();
            inWrite.signal();
        }
    }


    /**
     * sparse binary search main body
     */
    private BucketNode sparseSearch(int l, int r, Integer item) {
        if (l >= r) {
            return buckets.get(l);
        }

        int m = (l + r) / 2;
        BucketNode curr = buckets.get(m);
        if (curr.count == 0) {
            int left = m - 1, right = m + 1;
            while (true) {
                // NOTE: this can break
                // buckets which both ends are empty
                if (left < l && right > r)
                    return curr;
                if (left >= l && buckets.get(left).count != 0) {
                    m = left;
                    break;
                }
                else if (right <= r && buckets.get(right).count != 0) {
                    m = right;
                    break;
                }
                left--;
                right++;
            }
        }

        curr = buckets.get(m);        
        if (curr.head() > item) return sparseSearch(l, m, item);
        else if (curr.head() < item) return sparseSearch(m, r, item);
        return curr;
    }


    /**
     * monitor-ed wrapper for binary search
     * 
     * @return
     */
    private BucketNode search(int item) {
        // sparse binary search
        BucketNode bucket = null;
        // TODO: GET A READLOCK HERE M8
        int l = 0, r = buckets.size(); 
        while (numWrite != 0) inSearch.awaitUninterruptibly();
        numSearch++;
        // here readlock acquires / ensure acquires
        bucket = sparseSearch(l, r, item);
        numSearch--;
        
        // TODO: condition inSearch will turn off when all await has finished
        // IMPLEMENT A QUEUE FOR READS, they will ALL read concurrently as soon as inWrite negates.

        if (numSearch == 0) inSearch.signalAll();
        
        return bucket;
    }

    public boolean member(int item) {
        BucketNode bucket = search(item);
        if (bucket == null) {System.out.println("bucket is missing ;-;"); return false;}
        return bucket.lookUp(item);
    }

    public void print_sorted() {
        // This should be a special read state, which would wait for all regular read and writes to finish
        // then iterate through the printer

        while(numSearch != 0 && numWrite != 0) inPrint.awaitUninterruptibly();
        // BLOCK ALL INCOMING QUERY
        // TODO: another semaphore bruh

        buckets.stream().forEach(item -> item.print());
        inPrint.signalAll();
        // finish
    }
}
