package dev.fileformat.drako;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by lexchou on 4/24/2017.
 */
public interface EventCallback<EventArg> {
    void call(Object sender, EventArg arg);
}
