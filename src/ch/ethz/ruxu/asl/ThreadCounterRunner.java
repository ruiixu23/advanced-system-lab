package ch.ethz.ruxu.asl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

class ThreadCounterRunner implements Runnable {
    // The logger
    private static final Logger logger = LogManager.getLogger(ThreadCounterRunner.class);

    /**
     * Construct a new thread counter runner
     */
    ThreadCounterRunner() {}

    /**
     * Start running the thread counter runner
     */
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                ThreadCounterRunner.logger.debug("Thread counter stopping");
                break;
            }

            int count = 0;
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread.getState() == Thread.State.RUNNABLE) {
                    count++;
                }
            }

            ThreadCounterRunner.logger.debug(String.format("%d,%s,%d,%d",
                    System.currentTimeMillis(),
                    "Active threads count",
                    count,
                    Thread.activeCount()));
        }
    }
}
