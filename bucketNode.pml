#include "critical.h"

bool writeStatus = true;
int readStatus = 0;
bool cleanUpStatus = true;

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

inline insert() {
    bit inNonCritical = 1;
    bit inCritical = 0;
    waitBinary(writeStatus);
    waitBinary(cleanUpStatus);
csw: critical_section();
    signalBinary(cleanUpStatus);
    signalBinary(writeStatus);
}

inline lookup() {
    bit inNonCritical = 1;
    bit inCritical = 0;
    wait(readStatus);
csr: critical_section();
    signal(readStatus);
}

inline delete() {
    bit inNonCritical = 1;
    bit inCritical = 0;
    waitBinary(writeStatus);
    waitBinary(cleanUpStatus);
csw: critical_section();
    signalBinary(cleanUpStatus);
    signalBinary(writeStatus);
}

inline print() {
    bit inNonCritical = 1;
    bit inCritical = 0; 
    wait(readStatus);
csr: critical_section();
    signal(readStatus);
}

proctype A() {
    insert();
    insert();
    lookup();
    insert();
    lookup();
    delete();
}

proctype B() {
    insert();
    insert();
    lookup();
    insert();
    lookup();
    delete();
    delete();
}

init { run A(); run B(); };
ltl mutex { [] !((A@csw && B@csw) || (A@csr && B@csw) || (A@csw && B@csr)) };

