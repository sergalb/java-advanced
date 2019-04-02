package ru.ifmo.rain.balahnin.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

class ExecutedVector<T, R> {
    private final int identifier;
    private final List<? extends T> data;
    private final List<R> result;
    private int countCalculated;
    private int lastExecuted;
    private final Function<? super T, ? extends R> function;

    ExecutedVector(List<? extends T> data, Function<? super T, ? extends R> function, int identifier) {
        this.data = data;
        this.function = function;
        countCalculated = 0;
        lastExecuted = 0;
        this.identifier = identifier;
        result = new ArrayList<>(Collections.nCopies(data.size(), null));
    }

    void calc() {
        int ind;
        synchronized (this) {
            if (lastExecuted >= data.size()) {
                return;
            }
            ind = lastExecuted++;
        }
        result.set(ind, function.apply(data.get(ind)));
        synchronized (this) {
            ++countCalculated;
            if (countCalculated == data.size()) {
                notify();
            }
        }
    }

    List<R> getResult() throws InterruptedException {
        synchronized (this) {
            while (countCalculated != data.size()) {
                wait();
            }
            return result;
        }
    }

    synchronized int getIdentifier() {
        return identifier;
    }

    synchronized boolean isAllExecuted() {
        return lastExecuted == data.size();
    }
}