package ru.ifmo.rain.balahnin.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import static java.lang.Thread.interrupted;

/**
 * Concurrent execute tasks
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<ExecutedVector> tasksQueue;
    private volatile int identifier;
    private ArrayList<Thread> threadPool;

    /**
     * Create ParallelMapperImpl with 1 thread
     */
    public ParallelMapperImpl() {
        this(1);
    }

    /**
     * Create ParallelMapperImpl with {@code threads} threads
     *
     * @param threads count of threads must be > 0 (throws IllegalArgumentException otherwise
     */
    public ParallelMapperImpl(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("count threads must be > 0, now " + threads);
        }
        synchronized (this) {
            identifier = 0;
            tasksQueue = new LinkedList<>();
            threadPool = new ArrayList<>();
            for (int i = 0; i < threads; ++i) {
                createThreadWork();
            }
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        int identifier;
        ExecutedVector<T, R> task;
        synchronized (this) {
            identifier = this.identifier++;
        }
        task = new ExecutedVector<>(list, function, identifier);
        synchronized (tasksQueue) {
            tasksQueue.offer(task);
            tasksQueue.notifyAll();
        }
        return task.getResult();
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        for (Thread thread : threadPool) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
        synchronized (this) {
            threadPool.clear();
            tasksQueue.clear();
            identifier = 0;
        }
    }

    private void createThreadWork() {
        threadPool.add(new Thread(() -> {
            try {
                while (!interrupted()) {
                    synchronized (tasksQueue) {
                        while (tasksQueue.isEmpty()) {
                            tasksQueue.wait();
                        }
                    }
                    ExecutedVector curTask;
                    synchronized (tasksQueue) {
                        if (!tasksQueue.isEmpty()) {
                            curTask = tasksQueue.peek();
                        } else {
                            return;
                        }
                    }
                    if (curTask.isAllExecuted()) {
                        synchronized (tasksQueue) {
                            if (!tasksQueue.isEmpty() && tasksQueue.peek() != null &&
                                    curTask.getIdentifier() == tasksQueue.peek().getIdentifier()) {
                                tasksQueue.poll();
                            }
                        }
                    }
                    if (interrupted()) {
                        return;
                    }
                    curTask.calc();
                }
            } catch (InterruptedException ignore) {
            } finally {
                Thread.currentThread().interrupt();
            }
        }));
        threadPool.get(threadPool.size() - 1).start();
    }
}
