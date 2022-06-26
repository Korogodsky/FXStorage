
package fxstorage;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javafx.concurrent.Task;

public class Progress extends Task<Void>{
    
    private volatile static int filesTotal;
    private volatile static int filesDone;
    private volatile static Progress instance;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock wLock = lock.writeLock();

    private Progress() {}
    
    public synchronized static Progress getInstance(){
        if(instance == null){
            instance = new Progress();
        }
        return instance;
    }

    @Override
    protected Void call() throws Exception {
        return null;
    }

    public void incTotal(FormController controller){
        controller.showProgress();
        wLock.lock();
        try {
            filesTotal++;
        } finally {
            wLock.unlock();
        }
        updateProgress(filesDone, filesTotal);
    }
    
    public void incDone(FormController controller){
        wLock.lock();
        try {
            filesDone++;
            if(filesDone == filesTotal){
                filesDone = filesTotal = 0;
                controller.hideProgress();
            }else{
                updateProgress(filesDone, filesTotal);
            }
        } finally {
            wLock.unlock();
        }
    }
    
}
