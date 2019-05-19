package ThreadPool1;

/**
 * 定义任务队列接口
 */
public interface RunnableQueue {

    void offer(Runnable runnable);
    Runnable take() throws InterruptedException;
    int size();

}
