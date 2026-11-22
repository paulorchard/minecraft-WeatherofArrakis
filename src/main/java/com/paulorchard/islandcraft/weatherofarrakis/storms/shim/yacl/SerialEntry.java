package com.paulorchard.islandcraft.weatherofarrakis.storms.shim.yacl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Stand-in for YACL's @SerialEntry (no effect here). Not adapted from upstream. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SerialEntry {
    String value() default ""; String comment() default "";
}
