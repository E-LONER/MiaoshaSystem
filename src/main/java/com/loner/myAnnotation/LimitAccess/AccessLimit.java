package com.loner.myAnnotation.LimitAccess;


import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

//注解@Retention可以用来修饰注解，是注解的注解，称为元注解。
//有三个参数：CLASS  RUNTIME   SOURCE
//SOURCE：注解只保留在源文件，当Java文件编译成class文件的时候，注解被遗弃；如果只是做一些检查性的操作，比如 @Override 和 @SuppressWarnings，使用SOURCE 注解。
//CLASS：注解被保留到class文件，但jvm加载class文件时候被遗弃，这是默认的生命周期；如果要在编译时进行一些预处理操作，比如生成一些辅助代码（如 ButterKnife），就用 CLASS注解；
//RUNTIME：注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在；运行时去动态获取注解信息，那只能用 RUNTIME 注解
//生命周期长度 SOURCE < CLASS < RUNTIME ，所以前者能作用的地方后者一定也能作用。
@Retention(RUNTIME)
//@Target说明了Annotation所修饰的对象范围：Annotation可被用于 packages、types（类、接口、枚举、Annotation类型）、类型成员（方法、构造方法、成员变量、枚举值）、方法参数和本地变量（如循环变量、catch参数）。在Annotation类型的声明中使用了target可更加明晰其修饰的目标。
@Target(METHOD)
public @interface AccessLimit {
    int time();
    int maxAccess();
    boolean login() default true;
}
