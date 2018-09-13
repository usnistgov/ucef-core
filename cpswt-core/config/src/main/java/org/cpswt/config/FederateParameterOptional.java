package org.cpswt.config;

import java.lang.annotation.*;

/**
 * Annotation to indicate an OPTIONAL parameter of a federate config.
 */
@Documented
@Target(ElementType.FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FederateParameterOptional {}
