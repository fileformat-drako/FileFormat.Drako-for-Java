package dev.fileformat.drako;


/**
 * Created by lexchou on 10/12/2017.
 */
@Internal
class Stopwatch {
    private long previousTime;
    private long startTime;
    private boolean running;


    public void start() {
        running = true;
        long now = System.currentTimeMillis();
        previousTime += (now - startTime);
        startTime = now;
    }
    public void restart() {
        previousTime = 0;
        startTime = System.currentTimeMillis();
        running = true;
    }
    public void stop() {
        running = false;
        long now = System.currentTimeMillis();
        previousTime += (now - startTime);
    }
    public long elapsedMilliseconds() {
        long ret = previousTime;
        if(running)
            ret += (System.currentTimeMillis() - startTime);
        return ret;
    }
}
