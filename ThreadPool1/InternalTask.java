package ThreadPool1;

/**
 * 不断执行任务队列的执行单元
 */
public class InternalTask implements Runnable{
    private RunnableQueue linkedRunnableQueue;
    private boolean running =true;
    public InternalTask(RunnableQueue linkedRunnableQueue){
        this.linkedRunnableQueue=linkedRunnableQueue;
    }
    @Override
    public void run() {
        while(running&&!Thread.currentThread().isInterrupted()){
            Runnable take = null;
            try {
                take = linkedRunnableQueue.take();
                take.run();
            } catch (InterruptedException e) {
                running=false;
                break;
            }

        }
    }

    public void stop(){
        running=false;
    }
}
