package com.whut.training.aspect.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要记录服务层调用日志的类或方法。
 *
 * <p>默认会被 {@link com.whut.training.aspect.logging.ServiceLogAspect} 识别并记录入参与耗时。
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceLog {
}
