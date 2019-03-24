package ru.ifmo.rain.balahnin.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private volatile int identifier;
    private volatile int lastCalculated;
    private volatile boolean isStoped;
    private ArrayList<Thread> threadPool;
    private Queue<ExecutedVector> tasksQueue;
    public ParallelMapperImpl(int threads) {
        synchronized (this) {
            identifier = 0;
            lastCalculated = 0;
            tasksQueue = new ConcurrentLinkedQueue<>();
            threadPool = new ArrayList<>();
            threadPool.addAll(0, Collections.nCopies(threads, new Thread(() -> {
                while (!isStoped) {
                    while (tasksQueue.isEmpty()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Objects.requireNonNull(tasksQueue.peek()).calc();
                }
            })));
            tasksQueue = new LinkedList<>();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        ExecutedVector task = new ExecutedVector<T, R>(list, function);
        tasksQueue.offer(task);
        while (task.isReady()) {
            task.wait();
        }

        return null;
    }

    @Override
    public void close() {

    }

    private class ExecutedVector<T, R> {
        private List<? extends T> data;
        private volatile int countCalculated;
        private volatile int lastExecuted;
        private Function<? super T, ? extends R> function;

        void calc() {
            int ind;
            synchronized (this) {
                ind = lastExecuted++;
                if (ind > data.size()) {
                    return;
                }
            }
            function.apply(data.get(ind));
            synchronized (this) {
                ++countCalculated;
                if (countCalculated == data.size()) {
                    notify();
                }
            }
        }

        public List<? extends T> getResultList() throws InterruptedException {
            while (countCalculated != data.size()) {
                wait();
            }
            return data;
        }

        public synchronized boolean isReady() {
            return countCalculated == data.size();
        }

        public ExecutedVector(List<? extends T> data, Function<? super T, ? extends R> function) {
            this.data = data;
            this.function = function;
            countCalculated = 0;
            lastExecuted = 0;
        }
    }


}
