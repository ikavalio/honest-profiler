// Somewhat originally dervied from:
// http://www.codeproject.com/Articles/43510/Lock-Free-Single-Producer-Single-Consumer-Circular

// Multiple Producer, Single Consumer Queue

#ifndef CIRCULAR_QUEUE_H
#define CIRCULAR_QUEUE_H

#include "stacktraces.h"
#include <string.h>

#if __GNUC__ == 4 && __GNUC_MINOR__ < 6 && !defined(__APPLE__) && !defined(__FreeBSD__) 
  #include <cstdatomic>
#else
  #include <atomic>
#endif

#include <cstddef>

const size_t Size = 1024;

// Capacity is 1 larger than size to make sure
// we can use input = output as our "can't read" invariant
// and advance(output) = input as our "can't write" invariant
// effective the gap acts as a sentinel
const size_t Capacity = Size + 1;

class QueueListener {
public:
    virtual void record(const JVMPI_CallTrace &item) = 0;

    virtual ~QueueListener() {
    }
};

const int COMMITTED = 1;
const int UNCOMMITTED = 0;

struct TraceHolder {
    std::atomic<int> is_committed;
    JVMPI_CallTrace trace;
};

class CircularQueue {
public:
    explicit CircularQueue(QueueListener &listener, int maxFrameSize)
            : listener_(listener), input(0), output(0), maxFrames(maxFrameSize) {
        memset(buffer, 0, sizeof(buffer));
        for (int i = 0; i < Capacity; ++i)
            frame_buffer_[i] = new JVMPI_CallFrame[maxFrameSize]();
    }

    ~CircularQueue() {
        for (int i = 0; i < Capacity; ++i)
            delete[] frame_buffer_[i];
    }

    bool push(const JVMPI_CallTrace &item);

    bool pop();

private:

    QueueListener &listener_;

    std::atomic<size_t> input;
    std::atomic<size_t> output;
    int maxFrames;

    TraceHolder buffer[Capacity];
    JVMPI_CallFrame *frame_buffer_[Capacity];

    size_t advance(size_t index) const;

    void write(const JVMPI_CallTrace &item, const size_t slot);
};

#endif /* CIRCULAR_QUEUE_H */
