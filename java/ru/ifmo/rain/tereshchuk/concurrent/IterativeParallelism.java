package ru.ifmo.rain.tereshchuk.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IterativeParallelism implements ListIP {

    private ParallelMapper parallelMapper = null;

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T> List<List<? extends T>> getSublists(int threads, List<? extends T> list) {
        if (threads > list.size()) {
            threads = list.size();
        }
        List<List<? extends T>> sublists = new ArrayList<>();
        int packSize = list.size() / threads;
        for (int i = 0, from = 0, to = packSize; i < threads; i++, from += packSize, to += packSize) {
            if (i == threads - 1) {
                to += list.size() % threads;
            }
            sublists.add(list.subList(from, to));
        }
        return sublists;
    }

    private <T, R> R parallelize(int threads, List<? extends T> list,
                                 Function<List<? extends T>, R> function,
                                 Function<? super List<R>, R> merger) throws InterruptedException {

        List<List<? extends T>> sublists = getSublists(threads, list);

        if (parallelMapper != null) {
            return merger.apply(parallelMapper.map(function, sublists));
        }

        List<Thread> tasks = new ArrayList<>();
        List<R> results = new ArrayList<>();

        for (int i = 0; i < sublists.size(); i++) {
            results.add(null);
        }

        for (int i = 0; i < sublists.size(); i++) {
            final int finalI = i;
            tasks.add(new Thread(() -> results.set(finalI, function.apply(sublists.get(finalI)))));
            tasks.get(i).start();
        }

        for (int i = 0; i < sublists.size(); i++) {
            tasks.get(i).join();
        }

        return merger.apply(results);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> function = l -> l.stream().max(comparator).orElse(null);
        return parallelize(threads, values, function, function);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> function = l -> l.stream().min(comparator).orElse(null);
        return parallelize(threads, values, function, function);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelize(threads, values,
                l -> l.stream().allMatch(predicate),
                l -> l.stream().allMatch(a -> a)
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelize(threads, values,
                l -> l.stream().anyMatch(predicate),
                l -> l.stream().anyMatch(a -> a)
        );
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelize(threads, values,
                l -> l.stream().map(Object::toString).collect(Collectors.joining()),
                l -> l.stream().collect(Collectors.joining())
        );
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelize(threads, values,
                l -> l.stream().filter(predicate).collect(Collectors.toList()),
                l -> l.stream().flatMap(List::stream).collect(Collectors.toList())
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelize(threads, values,
                l -> l.stream().map(f).collect(Collectors.toList()),
                l -> l.stream().flatMap(List::stream).collect(Collectors.toList())
        );
    }
}
