package ThreadPool1;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicThreadPool extends Thread implements ThreadPool{

    private int initSize;
    private int coreSize;
    private int maxSize;
    private int activeCount;
    private long keepAliveTime ;
    private RunnableQueue runnableQueue;
    private boolean shutDown=false;
    private   ThreadFactory threadFactory;
    private TimeUnit timeUnit;
    private Queue<ThreadTask> queue = new ArrayDeque<>();
    private static final ThreadFactory DEFAULT_THREAD_FACTORY=new DefaultThreadFactory();
    private static final DenyPolicy DEFAULT_DENY_POLICY= new DenyPolicy.DisCardDenyPolicy();


        public BasicThreadPool(int initSize, int maxSize, int coreSize, int queueSize){
            this(initSize,coreSize,maxSize,queueSize,5,TimeUnit.SECONDS,DEFAULT_THREAD_FACTORY,DEFAULT_DENY_POLICY);
        }



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
