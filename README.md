# THREAD_POOL
手写线程池 更好理解线程池的原理

线程池的特点：
线程池由有限个线程组成，或者说是由创建者可控制的线程数量。
每个线程的任务都是一样的，从任务队列里面不断的取出任务并执行。
任务队列设置上限。
线程池自动的维护线程数量。
实现：
定义线程池接口：
package ThreadPool1;

/**
 * 定义线程池接口
 */
public interface ThreadPool {

    void execute(Runnable runnable);
    int getInitSize();
    int getCoreSize();
    int getMaxSize();
    int getActiveCount();
    boolean isShutDown();
    void shutDown();

}


定义任务队列接口：
package ThreadPool1;

/**
 * 定义任务队列接口
 */
public interface RunnableQueue {

    void offer(Runnable runnable);
    Runnable take() throws InterruptedException;
    int size();

}


定义拒绝策略：（当任务队列达到上限的时候做的反应）
package ThreadPool1;

/**
 * 定义拒绝策略
 * 实现三种策略
 * 第一种直接丢掉任务
 * 第二种抛出异常
 * 第三种将任务由提供者的线程处理
 */
public interface DenyPolicy {

    public void reject(ThreadPool threadPool,Runnable runnable);


    class DisCardDenyPolicy implements DenyPolicy{

        @Override
        public void reject(ThreadPool threadPool,Runnable runnable) {

        }
    }
    class AbortDenyPolicy implements  DenyPolicy{

        @Override
        public void reject(ThreadPool threadPool, Runnable runnable) {
            new DenyPolicyException("the runnable "+runnable+"will be abort");
        }
    }
    class  RunnerDenyPolicy implements  DenyPolicy{

        @Override
        public void reject(ThreadPool threadPool, Runnable runnable) {
            if(!threadPool.isShutDown()){
                runnable.run();
            }

        }
    }
}

定义线程工厂接口：
package ThreadPool1;

public interface ThreadFactory {
    Thread createThread(Runnable runnable);
}

定义线程封装类：（包括线程和操作单元，操作单元是不断从任务队列中取出任务执行的封装类）
package ThreadPool1;

public class ThreadTask {
    Thread thread;
    InternalTask internalTask;
    public ThreadTask(Thread thread ,InternalTask internalTask){
        this.internalTask=internalTask;
        this.thread=thread;
    }
}

定义线程池实现类：
package ThreadPool1;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicThreadPool extends Thread implements ThreadPool{

    private int initSize;//初始线程数
    private int coreSize;//核心线程数
    private int maxSize;//最大线程数
    private int activeCount;//活跃线程数
    private long keepAliveTime ;//线程自动更新时间
    private RunnableQueue runnableQueue;//任务队列
    private boolean shutDown=false;//线程池关闭标识
    private   ThreadFactory threadFactory;//线程工厂
    private TimeUnit timeUnit;
    private Queue<ThreadTask> queue = new ArrayDeque<>();//线程缓存队列，主要作用是为了删除线程
    private static final ThreadFactory DEFAULT_THREAD_FACTORY=new DefaultThreadFactory();//默认的线程工厂
    private static final DenyPolicy DEFAULT_DENY_POLICY= new DenyPolicy.DisCardDenyPolicy();//默认的拒绝策略

    /**
    *构造函数
    **/
        public BasicThreadPool(int initSize, int maxSize, int coreSize, int queueSize){
            this(initSize,coreSize,maxSize,queueSize,5,TimeUnit.SECONDS,DEFAULT_THREAD_FACTORY,DEFAULT_DENY_POLICY);
        }


    /**
    *包含线程工厂，TimeUtil keepAliveTime 的构造函数
    **/
    public BasicThreadPool(int initSize, int coreSize, int maxSize, int queueSize, long keepAliveTime, TimeUnit timeUnit, ThreadFactory threadFactory, DenyPolicy denyPolicy){
       this.initSize=initSize;
       this.coreSize=coreSize;
       this.maxSize=maxSize;
       this.runnableQueue=new LinkedRunnableQueue(queueSize,denyPolicy,this);
        this.threadFactory=threadFactory;
        this.timeUnit=timeUnit;
        this.keepAliveTime=keepAliveTime;
        init();
    }


    /**
     * 初始化创建线程池
     */
    private void init(){
        start();
            for(int i=0;i<initSize;i++){
                newThread();
            }
    }

    /**
     *
     * 提交任务
     * @param runnable
     */
    @Override
    public void execute(Runnable runnable) {
      if(shutDown){
          throw  new RuntimeException("the thread pool is shut down");
      }else{
          runnableQueue.offer(runnable);
      }

    }
    /**
     * 創建新的线程
     */
    public void  newThread(){
        InternalTask internalTask = new InternalTask(runnableQueue);
        Thread thread = threadFactory.createThread(internalTask);
        ThreadTask threadTask = new ThreadTask(thread,internalTask);
        queue.offer(threadTask);
        activeCount++;
        thread.start();
    }

    /**
     * 删除线程
     */
    public void removeThread(){
        ThreadTask remove = queue.remove();
        remove.internalTask.stop();
        activeCount--;

    }

    /**
     * 自动维护线程数
     * 每个keepAliveTime时间检查线程数
     */
    @Override
    public void run() {
        while (!shutDown&&!isInterrupted()) {
            try {
                timeUnit.sleep(keepAliveTime);
            } catch (InterruptedException e) {
                shutDown = true;
                break;
            }
            synchronized (this) {
                if (shutDown){
                    break;
                }
                if (queue.size() > 0 && activeCount < coreSize) {
                    for (int i = initSize; i < coreSize; i++) {
                        newThread();
                    }
                    continue;
                }
                if (queue.size() > 0 && activeCount < maxSize) {
                    for (int i = coreSize; i < maxSize; i++) {
                        newThread();
                    }
                }
                if (queue.size() > 0 && activeCount > coreSize) {
                    for (int i = coreSize; i < activeCount; i++) {
                        removeThread();
                    }
                }


            }
        }
    }

    @Override
    public int getInitSize() {
        if(shutDown){
            throw  new IllegalStateException("tne thread pool is destroy");
        }
        return initSize;
    }

    @Override
    public int getCoreSize() {
        if(shutDown){
            throw  new IllegalStateException("tne thread pool is destroy");
        }
        return coreSize;
    }

    @Override
    public int getMaxSize() {
        if(shutDown){
            throw  new IllegalStateException("tne thread pool is destroy");
        }
        return maxSize;
    }

    @Override
    public int getActiveCount() {
        synchronized (this) {
            if (shutDown) {
                throw new IllegalStateException("tne thread pool is destroy");
            }
            return activeCount;
        }
    }

    @Override
    public boolean isShutDown() {
        return shutDown;
    }

    @Override
    public void shutDown() {
        synchronized (this) {
            if(shutDown)return;
            shutDown = true;
            queue.forEach(threadTask -> {
                threadTask.internalTask.stop();
                threadTask.thread.interrupt();
            });
            this.interrupt();
        }
    }
    public int getQueueSize(){
        synchronized (this){
                if(shutDown){
                    throw new IllegalStateException("tne thread pool is destroy");
                }
                return queue.size();
        }
    }

    public int getLinkedListSize(){
        synchronized (this){
            if(shutDown){

            }
            return runnableQueue.size();
        }
    }


/**
*默认的线程工厂
**/
    private static class DefaultThreadFactory  implements  ThreadFactory{
        private static  final AtomicInteger GROUP_COUNTER=new AtomicInteger(1);
        private static  final  ThreadGroup group  = new ThreadGroup("MyThreadOPool"+GROUP_COUNTER.getAndDecrement());
        private  static  final AtomicInteger COUNTER = new AtomicInteger(0);
        @Override
        public Thread createThread(Runnable runnable ) {
            return new Thread(group,runnable,"thread-pool-"+COUNTER.getAndDecrement());

        }
    }
}

