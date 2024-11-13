package dev.fileformat.drako;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by lexchou on 4/24/2017.
 */
public class MulticastEvent<EventArg> {
    private List<EventCallback<EventArg>> callbacks;

    public static <EventArg> MulticastEvent<EventArg> Subscribe(MulticastEvent<EventArg> e, EventCallback<EventArg> callback) {
        MulticastEvent<EventArg> ret = e;
        if(ret == null)
            ret = new MulticastEvent<>();
        ret.subscribe(callback);
        return ret;
    }
    public void subscribe(EventCallback<EventArg> callback) {
        if(callbacks == null)
            callbacks = new ArrayList<>();
        callbacks.add(callback);
    }
    public void unsubscribe(EventCallback<EventArg> callback) {
        if(callbacks == null)
            return;
        callbacks.remove(callback);
    }
    public void invoke(Object sender, EventArg arg)
    {
        int size = callbacks.size();
        for(int i = 0; i < size; i++)
        {
            EventCallback<EventArg> callback = callbacks.get(i);
            callback.call(sender, arg);
        }
    }
}
