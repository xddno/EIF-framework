// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

package com.emberinjector.framework.internal.patcher;

import com.emberinjector.framework.internal.Canceler;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class EntryMethodModifier extends MethodModifier
{
    public EntryMethodModifier(MethodModifierInfo info)
    {
        super(Type.ON_ENTRY, info);
    }

    @Override
    public String getNewInstanceCode()
    {
        return String.format("new EntryMethodModifier(%s)", this.info.getNewInstanceCode());
    }

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor forwardTo, int access, String name, String descriptor)
    {
        return new AdviceAdapter(Opcodes.ASM9, forwardTo, access, name, descriptor)
        {
            @Override
            protected void onMethodEnter()
            {
                if (USE_REFLECTION)
                {
                    reflectionImplementation(mv);
                    return;
                }

                String cancelerClassName = Canceler.class.getName().replace('.', '/');

                // new Canceler()
                mv.visitTypeInsn(Opcodes.NEW, cancelerClassName);
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, cancelerClassName, "<init>", "()V", false);
                int cancelerVarIndex = availableVarIndex;
                availableVarIndex++;
                mv.visitVarInsn(Opcodes.ASTORE, cancelerVarIndex);

                // push Canceler
                mv.visitVarInsn(Opcodes.ALOAD, cancelerVarIndex);
                // push this (if non-static)
                if (!info.isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                // push all original method parameters
                pushArguments(mv);

                // call event handler
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        info.eventMethodClass, info.eventMethodName,
                        info.eventMethodSignature, false);

                // cancel if required to
                Label pop = new Label();

                mv.visitVarInsn(Opcodes.ALOAD, cancelerVarIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, cancelerClassName, "cancel", "Z");
                mv.visitJumpInsn(Opcodes.IFEQ, pop);

                visitReturnBasedOnReturnType(mv);

                mv.visitLabel(pop);
                popReturnValueBasedOnReturnType(mv);
            }
        };
    }

    private void reflectionImplementation(MethodVisitor mv)
    {
    }
}
