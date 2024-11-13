package dev.fileformat.drako;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by lexchou on 11/2/2017.
 * If this annotation exists on a class, means the class is an immutable data class
 * If this annotation exists on a method, means it's pure and will not modify the state of this object
 * If this annotation exists on a parameter, means the method will not modify the state of this parameter
 * This is only used by the bridging library for immutable analyzing
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Internal
@interface Pure {
}
