// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

package com.emberinjector.framework.internal;

import com.emberinjector.framework.internal.patcher.MethodModifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface EventHandler
{
    MethodModifier.Type type();
    String targetClass();
    String targetMethodName();
    String targetMethodDescriptor();
    boolean targetMethodIsStatic() default false;

    String redirectTargetOwner() default "";
    String redirectTargetName() default "";
    String redirectTargetDescriptor() default "";

    int modifyVariableOrdinal() default 0;
    String modifyVariableName() default "";
}
