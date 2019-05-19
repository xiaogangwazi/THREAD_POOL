package ThreadPool1;
/**
 * 定义任务队列
 * 提供添加，取出方法
 * 规定任务队列的最大任务数量
 */

import java.util.LinkedList;

public class LinkedRunnableQueue  implements  RunnableQueue {

    private LinkedList<Runnable> linkedList=new LinkedList<>();
    private DenyPolicy denyPolicy;
    private ThreadPool threadPool;
   private int limit;
   public LinkedRunnableQueue(int  limit,DenyPolicy denyPolicy,ThreadPool threadPool){
       this.limit=limit;
       this.denyPolicy=denyPolicy;
       this.threadPool=threadPool;
   }
    @Override
    public void offer(Runnable runnable) {
        synchronized (linkedList) {
            if (linkedList.size() >= limit) {
                denyPolicy.reject(threadPool, runnable);
            } else {
                linkedList.addLast(runnable);
                linkedList.notifyAll();
            }
        }
    }

    @Override
    public Runnable take() throws InterruptedException {
       synchronized (linkedList){
           while(linkedList.isEmpty()){
               try {
                   linkedList.wait();
               } catch (InterruptedException e) {
                   System.out.println("wait 状态被中断，继续执行");
               }
           }
       }
        return linkedList.removeFirst();
    }
    public int size(){
      return  linkedList.size();
    }

}
