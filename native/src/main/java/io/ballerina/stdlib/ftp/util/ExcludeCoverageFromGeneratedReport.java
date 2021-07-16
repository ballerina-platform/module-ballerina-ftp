package io.ballerina.stdlib.ftp.util;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used to exclude a method from Jacoco code coverage.
 */

@Retention(RUNTIME)
@Target(METHOD)
/**
 * Annotation to exclude certain methods from Jacoco Report
 */
public @interface ExcludeCoverageFromGeneratedReport {
}
