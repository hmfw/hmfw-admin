package com.example.admin.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 写操作方法上，由 {@link LogAspect} 拦截并落 sys_oper_log。
 *
 * <p>只记写操作：查询类接口不加该注解，避免日志表被 GET 刷爆。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /** 模块标题，如 "用户管理" */
    String title() default "";

    /** 业务类型 */
    BusinessType businessType() default BusinessType.OTHER;

    /** 是否记录请求参数 */
    boolean saveRequestData() default true;
}
