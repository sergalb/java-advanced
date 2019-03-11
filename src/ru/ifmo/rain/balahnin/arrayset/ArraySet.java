package ru.ifmo.rain.balahnin.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet(Collection<? extends T> data, Comparator<? super T> comparator) {
        this.comparator = comparator;
        Set<T> set = new TreeSet<>(comparator);
        set.addAll(data);
        this.data = new ArrayList<>(set);
    }

    private ArraySet(List<T> data, Comparator<?super T>comparator) {
        this.data = data;
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends T> data) {
        this(data, null);
    }

    public ArraySet(Comparator<? super T> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public  ArraySet() {
        this(Collections.emptyList(), null);
    }

    @Override
    public T lower(T t) {
        return getValue(t, false, false);
    }

    @Override
    public T floor(T t) {
        return getValue(t, true, false);
    }

    @Override
    public T ceiling(T t) {
        return getValue(t, true, true);
    }

    @Override
    public T higher(T t) {
        return getValue(t, false, true);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableCollection(data).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new DescendingList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        int fromIndex = getIndex(fromElement, fromInclusive, true);
        int toIndex = getIndex(toElement, toInclusive, false);

        if (fromIndex == -1 || toIndex == - 1 || fromIndex > toIndex) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(data.subList(fromIndex, toIndex + 1), comparator);
    }


    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return subSet(data.get(0), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return subSet(fromElement, inclusive, data.get(data.size() - 1), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (T) Objects.requireNonNull(o), comparator) >= 0;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (comparator != null && comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (data.isEmpty()) {
            throw new NoSuchElementException("ArraySet is empty");
        }
        return data.get(0);
    }

    @Override
    public T last() {
        if (data.isEmpty()) {
            throw new NoSuchElementException("ArraySet is empty");
        }
        return data.get(data.size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }

    private int getIndex(T element, boolean inclusive, boolean lesser) {
        int index = Collections.binarySearch(data, element, comparator);
        if (index < 0) {
            index = -index - 1;

            if (inclusive) {
                if (!lesser) {
                    index--;
                }
            } else {
                index += lesser ? 0 : -1;
            }
        } else {
            if (!inclusive) {
                index += lesser ? 1 : -1;
            }
        }
        return index >= 0 && index < data.size() ? index : -1;
    }

    private T getValue(T element, boolean inclusive, boolean lesser) {
        int index = getIndex(element, inclusive, lesser);
        if (index == -1) return null;
        return data.get(index);
    }

}
