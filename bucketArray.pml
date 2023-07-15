#include "critical.h"

#define read
#define write

// Regular semaphore
inline wait(sem) {
    atomic {
        writeStatus; 
        sem--;
    }
}

inline signal(sem) {
    sem++
}

// Binary semaphore
inline waitBinary(sem) {
    atomic  {
        readStatus == 0 && sem;
        sem = false;
    }
}

inline signalBinary(sem) {
    sem = true;
}

int numSearch = 0;
int numWrite = 0;
int janitorCount = 0;
int deletedCount = 0;
int numAppend = 0;
bool writeStatus = true;
int readStatus = 0;
bool cleanUpStatus = true;

inline insertFull() {
    bit inNonCritical = 1;
    bit inCritical = 0;
    numAppend++;
    do
    :: numWrite > 0 || numSearch > 0  -> skip
    :: else -> break 
    od
    waitBinary(writeStatus);
csw: critical_section();
    numAppend--;
    signalBinary(writeStatus);
}

inline insert(){
    bit inNonCritical = 1;
    bit inCritical = 0;
    numWrite++;
    wait(readStatus);
csr: critical_section();
    numWrite--;
    signal(readStatus);
}

inline cleanup(){
    bit inNonCritical = 1;
    bit inCritical = 0;
    waitBinary(writeStatus);
    waitBinary(cleanUpStatus);
    janitorCount = 1;
    do 
    :: numWrite != 0 || numSearch != 0 -> skip 
    :: else -> break
    od
csw: critical_section();
    signalBinary(cleanUpStatus);
    signalBinary(writeStatus);
    janitorCount = 0;
}

inline search() {
    bit inNonCritical = 1;
    bit inCritical = 0;
    wait(readStatus);
    do
    :: janitorCount > 0 || numAppend > 0 -> skip;
    :: else -> break
    od
    numSearch++;
csr: critical_section();
    numSearch--;
    signal(readStatus);
}

inline print_sorted(){
    bit inNonCritical = 1;
    bit inCritical = 0;
    wait(readStatus);
csr: critical_section();
    signal(readStatus);
}

proctype A(){
    // insert/delete
    insert();
    search();
    insert();
    search();
    search();
    cleanup();
}

proctype B(){
    // insert/delete
    insert();
    search();
    insert();
    search();
    search();
    cleanup();
    search();
}

init { run A(); run B(); }

ltl mutex { [] !((A@csw && B@csw) || (A@csr && B@csw) || (A@csw && B@csr)) };
