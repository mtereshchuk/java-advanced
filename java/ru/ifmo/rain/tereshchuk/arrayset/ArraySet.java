package ru.ifmo.rain.tereshchuk.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final List<E> array;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(new ArrayList<E>());
    }

    private ArraySet(ArraySet<E> arraySet, int fromIndex, int toIndex) {
        this.array = arraySet.array.subList(fromIndex, toIndex);
        this.comparator = arraySet.comparator;
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        this.comparator = comparator;
        TreeSet<E> treeSet = new TreeSet<E>(comparator);
        treeSet.addAll(collection);
        array = Collections.unmodifiableList(new ArrayList<E>(treeSet));
    }

    @Override
    public Iterator<E> iterator() {
        return array.iterator();
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    private int find(E element) {
        return Collections.binarySearch(array, element, comparator);
    }

    @Override
    public boolean contains(Object o) {
        return find((E) o) >= 0;
    }

    private int findAndFixIndex(E element) {
        int index = find(element);
        return index >= 0 ? index : -(index + 1);
    }

    private SortedSet<E> subSet(E fromElement, E toElement, boolean inclusiveEnd) {
        int fromIndex, toIndex;
        if (isEmpty()) {
            fromIndex = toIndex = 0;
        } else {
            fromIndex = fromElement == first() ? 0 : findAndFixIndex(fromElement);
            toIndex = toElement == last() ? size() - 1 : findAndFixIndex(toElement);
            if (inclusiveEnd) {
                toIndex++;
            }
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("position of first argument is greater than second");
        }
        return new ArraySet<E>(this, fromIndex, toIndex);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return subSet(isEmpty() ? null : first(), toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return subSet(fromElement, isEmpty() ? null : last(), true);
    }

    private E get(int index) {
        if (isEmpty()) {
            throw new NoSuchElementException("set is empty");
        }
        return array.get(index);
    }

    @Override
    public E first() {
        return get(0);
    }

    @Override
    public E last() {
        return get(size() - 1);
    }
}