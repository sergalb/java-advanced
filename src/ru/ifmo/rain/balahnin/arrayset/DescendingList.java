package ru.ifmo.rain.balahnin.arrayset;

import java.util.AbstractList;
import java.util.List;

public class DescendingList<T> extends AbstractList<T> {
    private List<T> data;

    DescendingList(List<T> data) {
        this.data = data;

    }

    @Override
    public T get(int index) {
        return data.get(size() - index - 1);
    }

    @Override
    public int size() {
        return data.size();
    }
}
