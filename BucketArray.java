import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
    private Lock readLock = arrLock.readLock(), writeLock = arrLock.writeLock();
    private Condition readGate = readLock.newCondition(),
                      vacancy = readLock.newCondition(),
                      notEmpty = readLock.newCondition(),
                      afterHour = writeLock.newCondition(),
                      appendNew = writeLock.newCondition(),
                      inPrint = readLock.newCondition();
    private int numSearch = 0, numWrite = 0, janitorCount = 0, deletedCount = 0,
                numAppend = 0;
    private Queue<Integer> itemBuffer;

    public BucketArray() {
        // init with 1 cell
        buckets = new ArrayList<BucketNode>(1);
        itemBuffer = new PriorityQueue<Integer>();
    }

    public void insert(Integer item) {
        BucketNode bucket = search(item);

        /* A problem:
         * if multiple processes attempt to append an element, the buckets
         * their "prev" node would all be the same last node, failing the linked property
         */
        // A semaphore does not fully resolve this problem, as it will create buckets with a
        // singular element, nullifying the concurrency advantage of this data structure

        /* Write lock solution:
         * a writeLock functions just like a binary semaphore, we attach with the "multiWrite"
         * condition, that if there are multiple queued writes at the end node, instead create
         * buckets filled with all queued elements.
         * 
         * From the initial binary search, if we detect isFull & isTail, block all incoming inserts
         * and insert them with a corresponding newly created buckets (ceiling(m / 4))
         */

        // POTENTIAL ISSUE: not mutex with cleanup()

        if (bucket.isFullAndTail()) {
            // wait for all existing inserts to finish, and buffer the appending element
            numAppend++;

            // Priority queue.add is O(N)
            itemBuffer.add(item);

            while (numWrite > 0 || numSearch > 0) appendNew.awaitUninterruptibly();
            writeLock.lock();
            try {
                // determine how many buckets are needed
                int bucketCount = itemBuffer.size() / 4 + 1;
                // ArrayList<BucketNode> newArray = new ArrayList<>();
                // append each element to have linear complexity
                for (int i = 0; i < bucketCount; i++) {
                    BucketNode newNode = new BucketNode(this);
                    
                    /**
                     * TODO: (this problem has been resolved)
                     * This is a priority queue, no worries
                     */

                    // gradually fill all the nodes and append them in-order
                    while (!itemBuffer.isEmpty() && newNode.isFullAndTail()) {
                        newNode.insert(itemBuffer.poll());
                    }
                    buckets.get(buckets.size() - 1).next = newNode;
                    buckets.add(newNode);
                }
            } finally {
                writeLock.unlock();
            }

            // no need to clean up for new insertions,
            // likely no deleted nodes have been generated
            return;
        }
        
        // Queue starts here
        // TODO: implement semaphore for holding the read and write pointers in order

        readLock.lock();
        try {
            if (size > INIT_SIZE) {
                // surrender insertion and wait for vacancy in array (delete()).
                vacancy.awaitUninterruptibly();
            }
            bucket.insert(item);
            size++;
        } finally {
            readLock.unlock();
        }

        cleanup();
        // reset the deletion counter
    }

    public void delete(Integer item) {
        BucketNode bucket = search(item);
        deletedCount = bucket.delete(item) == true ? 1 : 0;

        if (deletedCount >= 6) cleanup();
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

        writeLock.lock();
        try {
            // FIFO delete op
            delSem.acquireUninterruptibly();

            // signal all incoming search process to surrender
            janitorCount = 1;
    
            // wait until current searches finish
            while (numWrite != 0 && numSearch != 0) afterHour.awaitUninterruptibly();
            
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
            deletedCount = 0;
        } finally {
            delSem.release();
            writeLock.unlock();
        }
    }


    /**
     * sparse binary search main body
     * CRITICAL SECTION
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
        readLock.lock();
        BucketNode bucket = null;
        try {
            // if cleanup() is queued, block all incoming reads
            // if new buckets need to be created, also block all incoming reads
            while (janitorCount > 0 || numAppend > 0) readGate.awaitUninterruptibly();

            int l = 0, r = buckets.size(); 
            // while (numWrite != 0) inSearch.awaitUninterruptibly();

            /* NOTE:
            * Since new buckets can only be appended, this should have no effect on the r index
            * of the binary search and should remain in-bound
            */

            numSearch++;
            // here readlock acquires / ensure acquires
            bucket = sparseSearch(l, r, item);
            numSearch--;
        } finally {
            readLock.unlock();;
        }
        
        // TODO: condition inSearch will turn off when all await has finished
        // IMPLEMENT A QUEUE FOR READS, they will ALL read concurrently as soon as vacancy negates.

        // if (numSearch == 0) inSearch.signalAll();
        
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
        readLock.lock();
        try {
            // while(numWrite != 0) inPrint.awaitUninterruptibly();
            // first test maximized concurrency, if concurrency is enough
            // add (attempted) linear waiting

            buckets.stream().forEach(item -> item.print());
        } finally {
            // inPrint.signalAll();
            readLock.unlock();
        }
        // finish
    }
}
