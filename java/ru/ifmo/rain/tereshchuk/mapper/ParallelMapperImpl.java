package ru.ifmo.rain.tereshchuk.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> threads;
    private final Queue<Runnable> tasks;

    public ParallelMapperImpl(int numOfThreads) {

        threads = new ArrayList<>();
        tasks = new ArrayDeque<>();

        Runnable runnable = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Runnable curTask;
                    synchronized (tasks) {
                        while (tasks.isEmpty()) {
                            tasks.wait();
                        }
                        curTask = tasks.poll();
                    }
                    curTask.run();
                }
            } catch (InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < numOfThreads; i++) {
            threads.add(new Thread(runnable));
            threads.get(i).start();
        }
    }

    private class Counter {
        private int value = 0;

        private void increment() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {

        List<R> results = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            results.add(null);
        }

        Counter counter = new Counter();
        for (int i = 0; i < args.size(); i++) {
            final int finalI = i;
            Runnable runnable = () -> {
                results.set(finalI, f.apply(args.get(finalI)));
                synchronized (counter) {
                    counter.increment();
                    if (counter.getValue() == args.size()) {
                        counter.notify();
                    }
                }
            };
            synchronized (tasks) {
                tasks.add(runnable);
                tasks.notify();
            }
        }

        synchronized (counter) {
            while (counter.getValue() != args.size()) {
                counter.wait();
            }
        }

        return results;
    }

    @Override
    public void close() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException ignored) {};
    }
}