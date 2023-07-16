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
    public int size = 0, numBuckets = 1;
    private ArrayList<BucketNode> buckets;
    private Semaphore delSem = new Semaphore(1, true);
    private ReadWriteLock arrLock = new ReentrantReadWriteLock();
    private Lock readLock = arrLock.readLock(), writeLock = arrLock.writeLock();
    private Condition /*readGate = readLock.newCondition(),*/
                      /*vacancy = readLock.newCondition(),*/
                      /* notEmpty = readLock.newCondition(),*/
                      afterHour = writeLock.newCondition(),
                      appendNew = writeLock.newCondition()
                      /*, inPrint = readLock.newCondition() */;
    private int numSearch = 0, numWrite = 0, janitorCount = 0, deletedCount = 0,
                numAppend = 0;
    private Queue<Integer> itemBuffer;

    public BucketArray() {
        // init with 1 cell
        buckets = new ArrayList<BucketNode>();
        BucketNode init = new BucketNode(this);
        // init.insert(0);
        buckets.add(init);
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


        
        // Queue starts here
        // TODO: implement semaphore for holding the read and write pointers in order
        numWrite++;

        while (size > INIT_SIZE);
        // surrender insertion and wait for vacancy in array (delete()).
        readLock.lock();
        try {
                // if (size > INIT_SIZE) {readLock.unlock();}
                // else {readLock.lock(); break;}
             
            
            System.out.println("insert: passing to BucketNode");

            bucket.insert(item);
            size++;
        } finally {
            numWrite--;
            readLock.unlock();
            System.out.println("insert: readlock unlocked");
        }

        
        System.out.println("insert: proceeding to cleanup");

        cleanup();
        // reset the deletion counter
    }


    public void appendInsert(Integer item) {
        System.out.println("appendInsert: passOn detected on final array element");

        // wait for all existing inserts to finish, and buffer the appending element
        numAppend++;

        // Priority queue.add is O(N)
        itemBuffer.add(item);
        
        System.out.println("appendInsert: number of append thread detected, buffering item");

        // while (numWrite > numAppend || numSearch > 0) /*appendNew.awaitUninterruptibly()*/ ;

        System.out.println("appendInsert: spinlock cleared");

        /*
         * NEW discovery
         * This does not actually require mutex, if we simply do not update the binary search range
         * until all nodes have been appended.
         */

        // writeLock.lock();

        System.out.println("appendInsert: writelock engaged");
 
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
            while (!itemBuffer.isEmpty() && !newNode.isFullAndTail()) {
                newNode.insert(itemBuffer.poll());
            }
            buckets.get(numBuckets - 1).next = newNode;
            buckets.add(newNode);
            numBuckets += bucketCount;
        }
        numAppend--;
        // writeLock.unlock();

        // no need to clean up for new insertions,
        // likely no deleted nodes have been generated
        return;
    }



    public void delete(Integer item) {
        BucketNode bucket = search(item);
        deletedCount += bucket.delete(item) == true ? 1 : 0;

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
    

    public void cleanup() {
        // TODO: a semaphore for one cleanup() concurrent at a time
        /**
         * TODO:
         * also implement a lock which all pointers that queues the cleanup method
         * cleanup() should wait for all searches to finish
         * and blocks all following search until cleanup has completed
         */

        // All buckets after the cleanup should have nearestLeft = nearestRight = 0

        System.out.println("cleanup: attempting to acquire writelock");

        writeLock.lock();

        System.out.println("cleanup: writelock engaged");

        try {
            // FIFO delete op
            delSem.acquireUninterruptibly();

            System.out.println("cleanup: mutex acquired");

            // signal all incoming search process to surrender
            janitorCount = 1;
    
            // wait until current searches finish

            

            while (numWrite != 0 || numSearch != 0) {
                System.out.printf("cleanup: waiting for numWrite: %d == 0, numSearch %d == 0", numWrite, numSearch);
                afterHour.awaitUninterruptibly();
            }
            
            // TODO: cleanup() main body
            for (int i = numBuckets - 2; i >= 0; i--) {
                // start with second last to avoid accidentally removing concurrent appendInsert() nodes
                BucketNode curr = buckets.get(i), prevNext = buckets.get(i + 1);
                // inductively update the link to the next node each time.
                while (curr.count != 0 && i >= 1) {prevNext = curr; curr = buckets.get(i-1); i--;}
                // get to the previous node, change its next to the previous next
                if (i == 0 && curr.count == 0) {
                    // detach head
                    buckets.remove(i);
                } else if (i >=1) {
                    // remove an intermediate element
                    curr = buckets.get(i-1);
                    curr.next = prevNext;
                    buckets.remove(i);
                } // here we can ignore the final element
            }

            buckets.removeIf(node -> (node.count == 0));
            // This is quadratic time complexity
            // however there's no good way to delete a chunk off an array in linear time
            deletedCount = 0;
            janitorCount = 0;
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
        // Not an approprioate terminating condition
        if (r - l<= 1) {
            System.out.println("search terminated: no match");
            return buckets.get(r).head() > item ? buckets.get(l) : buckets.get(r);
        }

        int m = (l + r) / 2;
        System.out.printf("sparseSearch: m = %d\n", m);
        BucketNode curr = buckets.get(m);
        if (curr.count == 0) {
            System.out.println("sparseSearch: empty node detected =========");
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

        System.out.println("sparseSearch: SEARCHING ==============================");

        curr = buckets.get(m);
        if (curr.head() == item) return curr;       
        else if (curr.head() > item) return sparseSearch(l, m, item);
        else return sparseSearch(m, r, item);
        // return null;
    }


    /**
     * monitor-ed wrapper for binary search
     * 
     * @return
     */
    private BucketNode search(int item) {
        BucketNode bucket = null;
        // sparse binary search
        while (janitorCount > 0 || numAppend > 0);
        readLock.lock();
        try {
            // if cleanup() is queued, block all incoming reads
            // if new buckets need to be created, also block all incoming reads

            int l = 0, r = numBuckets - 1; 
            System.out.printf("search: prepping binary search, l = %d, r = %d\n", l, r);
            // while (numWrite != 0) inSearch.awaitUninterruptibly();

            /* NOTE:
            * Since new buckets can only be appended, this should have no effect on the r index
            * of the binary search and should remain in-bound
            */

            numSearch++;
            // here readlock acquires / ensure acquires
            bucket = sparseSearch(l, r, item);
            System.out.printf("search: obtained bucket head: %d\n", bucket.head());
            numSearch--;
        } finally {
            readLock.unlock();
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

        System.out.printf("print_sorted: number of buckets: %d\n", numBuckets);

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
