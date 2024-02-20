package org.fhm.ioc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Classname DemoComponent
 * @Description TODO Customize the mark annotation of inject into IOC
 * @Date 2024/2/20-2:48 PM
 * @Author tanbo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DemoComponent {

    String value();

}