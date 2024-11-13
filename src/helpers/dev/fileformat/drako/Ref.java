package dev.fileformat.drako;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by lexchou on 7/26/2017.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
@Internal
@interface Ref {
}
