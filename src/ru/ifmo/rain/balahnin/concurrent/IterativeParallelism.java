package ru.ifmo.rain.balahnin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    /**
     * Join values to string.
     *
     * @param threads number or concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        List<String> strings = calc(threads, values, stream -> stream.map(Object::toString).collect(Collectors.joining()));
        return String.join("", strings);
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        List<Stream<? extends T>> filterFunc = calc(threads, values, stream -> stream.filter(predicate));
        return filterFunc.stream().flatMap(s -> s).collect(Collectors.toList());
    }

    /**
     * Mas values.
     *
     * @param threads number or concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        List<Stream<? extends U>> mapFunc = calc(threads, values, stream -> stream.map(f));
        return mapFunc.stream().flatMap(s -> s).collect(Collectors.toList());
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        List<T> matches = calc(threads, values, stream -> stream.max(comparator).orElse(null));
        return matches.stream().max(comparator).orElse(null);
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        List<T> matches = calc(threads, values, stream -> stream.min(comparator).orElse(null));
        return matches.stream().min(comparator).orElse(null);
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<Boolean> matches = calc(threads, values, stream -> stream.allMatch(predicate));
        return matches.stream().allMatch(bool -> bool);
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<Boolean> matches = calc(threads, values, stream -> stream.anyMatch(predicate));
        return matches.stream().anyMatch(bool -> bool);
    }

    private <T, R> List<R> calc(int threads, List<? extends T> values,
                                Function<Stream<? extends T>, ? extends R> function) throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("threads count must be > 0, now" + threads);
        }
        if (values.size() < threads) {
            threads = values.size();
        }
        int lengthPart = values.size() / threads;
        int reminder = values.size() % threads;
        int curInd = 0;

        ArrayList<Thread> threadPool = new ArrayList<>();
        List<R> ans = new ArrayList<>(Collections.nCopies(threads, null));

        for (int i = 0; i < threads; ++i) {

            int hasReminder = (i < reminder) ? 1 : 0;
            List<? extends T> part = values.subList(curInd, curInd + lengthPart + hasReminder);
            curInd += lengthPart + hasReminder;

            int finalI = i;
            Thread worker = new Thread(() -> ans.set(finalI, function.apply(part.stream())));
            threadPool.add(worker);
            worker.start();
        }
        for (Thread worker : threadPool) {
            worker.join();
        }
        return ans;
    }
}
