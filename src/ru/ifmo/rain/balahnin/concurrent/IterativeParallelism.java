package ru.ifmo.rain.balahnin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import ru.ifmo.rain.balahnin.mapper.ParallelMapperImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Concurrent apples own function
 */
public class IterativeParallelism implements ListIP {
    private final ParallelMapper parallelMapper;

    /**
     * Default constructor doing nothing
     */
    public IterativeParallelism() {
        parallelMapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

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
        List<String> strings = calc(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                reduceStream -> reduceStream.collect(Collectors.toList()));
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
        return calc(threads, values,
                stream -> stream.filter(predicate),
                reduceStream -> reduceStream.flatMap(s -> s).collect(Collectors.toList()));
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
        return calc(threads, values,
                stream -> stream.map(f),
                reduceStream -> reduceStream.flatMap(s -> s).collect(Collectors.toList()));
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
        return minimum(threads, values, comparator.reversed());
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
        return calc(threads, values,
                stream -> stream.min(comparator).orElse(null),
                reduceStream -> reduceStream.min(comparator).orElse(null));
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
        return !any(threads, values, predicate.negate());
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
        return calc(threads, values,
                stream -> stream.anyMatch(predicate),
                resultStream -> resultStream.anyMatch(bool -> bool));
    }

    private <T, R, U> U calc(int threads, List<? extends T> values,
                             Function<Stream<? extends T>, ? extends R> function,
                             Function<Stream<? extends R>, U> reduceFunction) throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("threads count must be > 0, now" + threads);
        }
        if (values == null || values.isEmpty()) {
            throw new NoSuchElementException("given list of values is empty (or null)");
        }
        if (values.size() < threads) {
            threads = values.size();
        }
        int lengthPart = values.size() / threads;
        int reminder = values.size() % threads;
        int curInd = 0;

        List<Stream<? extends T>> streamList = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            int hasReminder = (i < reminder) ? 1 : 0;
            List<? extends T> part = values.subList(curInd, curInd + lengthPart + hasReminder);
            curInd += lengthPart + hasReminder;
            streamList.add(part.stream());
        }

        U ans;
        if (parallelMapper == null) {
            try (ParallelMapper workingMapper = new ParallelMapperImpl(threads)) {
                ans = reduceFunction.apply(workingMapper.map(function, streamList).stream());
            }
        } else {
            ans = reduceFunction.apply(parallelMapper.map(function, streamList).stream());
        }
        return ans;
    }
}
