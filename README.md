# threadpool-test
[toc]
### 创建线程三种便捷方法

| Method | desc |
| --- | --- |
| newFixedThreadPool(int nThreads) | 创建固定大小的线程池 |
| newSingleThreadExecutor() | 创建只有一个线程的线程池 |
| newCachedThreadPool() | 创建一个无限数量线程的线程池，任何提交的任务都将立刻执行 |

**以上便捷的创建线程池的方法，只适用于小程序,对于长期运行在服务端的程序，应该使用ThreadPoolExecutor去实例化一个线程池**
实际上以上便捷创建线程的方法，也都是调用的ThreadPoolExecutor这个类的构造方法创建的

### 完整参数创建
```java
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }
```
**完整的构造方法参数如下**

```java
int corePoolSize,
int maximumPoolSize,
long keepAliveTime,
TimeUnit unit,
BlockingQueue<Runnable> workQueue,
ThreadFactory threadFactory,
RejectedExecutionHandler handler
```

| property | desc |
| --- | --- |
| int corePoolSize | 线程池长期维持的线程数，即使线程处于Idle状态，也不会回收 |
| int maximumPoolSize | 线程数的上限 |
| long keepAliveTime | 非核心线程不工作状态最长存活时间，超过它们就会被回收 |
| TimeUnit unit | 时间工具类 |
| BlockingQueue<Runnable> workQueue | 任务的排队队列 |
| ThreadFactory threadFactor | 新线程的产生方式 |
| RejectedExecutionHandler handler | 拒绝策略 |

```java
corePoolSize和maximumPoolSize 设置不当会影响效率和线程耗尽
workQueue 设置不当导致OOM
handler 设置不当导致提交任务时抛出异常
```

### 线程池的工作顺序
> If fewer than corePoolSize threads are running, the Executor always prefers adding a new thread rather than queuing.
If corePoolSize or more threads are running, the Executor always prefers queuing a request rather than adding a new thread.
If a request cannot be queued, a new thread is created unless this would exceed maximumPoolSize, in which case, the task will be rejected.

> 如果运行的线程小于corePoolSize，则执行程序总是喜欢添加新线程而不是排队。
如果正在运行线程大于corePoolSize，那么执行器总是倾向于对请求进行排队，而不是添加新线程。
如果一个请求不能排队，那么将创建一个新线程，除非这个线程的大小超过maximumPoolSize，在这种情况下，任务将被拒绝。

```
corePoolSize -> 任务队列 -> maximumPoolSize -> 拒绝策略

就是说这个线程池的线程数小于corePoolSize，就会创建新的线程，大于的话就会排队等待前面的任务线程执行完成，不能排队就会创建新线程，如果线程已经超过maximumPoolSize，任务就会被拒绝。
```

### Runnable和Callable

| submit method | have result |
| --- | --- |
| Future<T> submit(Callable<T> task) | yes |
| void execute(Runnable command)	 | no |
| Future<?> submit(Runnable task)	 | no,虽然可以get()获取返回结果但总是为null |

### 正确使用线程池
#### example
```java
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }
#================================================================
ExecutorService executorService = new ThreadPoolExecutor(2,
                2, 
                0,
                TimeUnit.SECONDS, 
                new ArrayBlockingQueue<>(512), // 使用有界队列，避免OOM
                new ThreadPoolExecutor.DiscardPolicy());
```


### 拒绝策略
**当队列满了，就会拒绝新的任务**

| 拒绝策略 | 行为 |
| --- | --- |
| AbortPolicy\[ə'bɔrt]['pɑləsi] | 抛出RejectedExecutionException,线程池默认拒绝策略 |
| DiscardPolicy[dɪsˈkɑːrd] | 什么也不做，直接忽略 |
| DiscardOldestPolicy | 丢弃执行队列中最老的任务，尝试为当前提交的任务腾出位置 |
| CallerRunsPolicy | 直接由提交任务者执行这个任务
 |
 
 
 
 
 ### example
 ```java
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

Exception in thread "main" java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.FutureTask@6193b845 rejected from java.util.concurrent.ThreadPoolExecutor@2e817b38[Running, pool size = 79, active threads = 79, queued tasks = 20, completed tasks = 0]
	at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:2063)
	at java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:830)
	at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1379)
	at java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:134)
	at com.alag.threadpool.App.exec(App.java:44)
	at com.alag.threadpool.App.main(App.java:23)
```
