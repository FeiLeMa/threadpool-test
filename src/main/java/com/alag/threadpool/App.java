package com.alag.threadpool;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hello world!
 */
public class App {
    private static AtomicInteger counter =  new AtomicInteger(1);
    public static void main(String[] args) {
        App app = new App();
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(20);
        ExecutorService executor = new ThreadPoolExecutor(2,
                79,
                0,
                TimeUnit.SECONDS,
                workQueue,
                new ThreadPoolExecutor.AbortPolicy());
        Set<Future<Object>> futures = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Future<Object> future = app.exec(executor);
            futures.add(future);
        }
        executor.shutdown();
        Iterator<Future<Object>> iterator = futures.iterator();
        while (iterator.hasNext()) {
            Future future = iterator.next();
            try {
                Object o = future.get();
                System.out.println(o);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    public  Future<Object> exec(ExecutorService executor) {

        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Thread.sleep(1*1000);
                return counter.getAndIncrement();
            }
        });
    }
}
