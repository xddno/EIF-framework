// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

package com.emberinjector.framework.internal.patcher;

import com.emberinjector.framework.internal.Thrower;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class ReturnThrowMethodModifier extends MethodModifier
{
    public ReturnThrowMethodModifier(MethodModifierInfo info)
    {
        super(Type.ON_RETURN_THROW, info);
    }

    @Override
    public String getNewInstanceCode()
    {
        return String.format("new ReturnThrowMethodModifier(%s)", this.info.getNewInstanceCode());
    }

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor forwardTo, int access, String name, String descriptor)
    {
        MethodVisitor methodVisitor = new AdviceAdapter(Opcodes.ASM9, forwardTo, access, name, descriptor)
        {
            @Override
            protected void onMethodExit(int opcode)
            {
                String throwerClassName = Thrower.class.getName().replace('.', '/');

                // create Thrower
                mv.visitTypeInsn(Opcodes.NEW, throwerClassName);
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, throwerClassName, "<init>", "()V", false);
                int throwerVarIndex = availableVarIndex;
                availableVarIndex++;
                mv.visitVarInsn(Opcodes.ASTORE, throwerVarIndex);

                if (opcode == Opcodes.ATHROW)
                {
                    // stack: [exception]
                    // store exception into Thrower.thrown
                    mv.visitVarInsn(Opcodes.ALOAD, throwerVarIndex);
                    mv.visitInsn(Opcodes.SWAP);
                    // stack: [exception, Thrower]
                    mv.visitFieldInsn(Opcodes.PUTFIELD, throwerClassName, "thrown", "Ljava/lang/Throwable;");
                    // stack: []

                    // push default return value (for event handler signature)
                    pushDefaultReturnValue(mv);
                }

                // push Thrower
                mv.visitVarInsn(Opcodes.ALOAD, throwerVarIndex);
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

                if (opcode == Opcodes.ATHROW)
                {
                    popReturnValueBasedOnReturnType(mv);

                    // check Thrower.thrown
                    mv.visitVarInsn(Opcodes.ALOAD, throwerVarIndex);
                    mv.visitFieldInsn(Opcodes.GETFIELD, throwerClassName, "thrown", "Ljava/lang/Throwable;");

                    // if thrown != null, re-throw it (leave on stack for catch handler's ATHROW)
                    // if thrown == null, swallow exception and return normally
                    Label rethrow = new Label();
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitJumpInsn(Opcodes.IFNONNULL, rethrow);

                    // null → pop null and return normally
                    mv.visitInsn(Opcodes.POP);
                    int returnTypeSort = returnType.getSort();
                    if (returnTypeSort == org.objectweb.asm.Type.VOID) {
                        mv.visitInsn(Opcodes.RETURN);
                    } else {
                        pushDefaultReturnValue(mv);
                        visitReturnBasedOnReturnType(mv);
                    }

                    // non-null → leave on stack for catch handler
                    mv.visitLabel(rethrow);
                }
            }
        };
        return new AvailableIndexMethodVisitor(Opcodes.ASM9, methodVisitor);
    }

    private void pushDefaultReturnValue(MethodVisitor mv)
    {
        switch (returnType.getSort())
        {
            case org.objectweb.asm.Type.BOOLEAN:
            case org.objectweb.asm.Type.CHAR:
            case org.objectweb.asm.Type.BYTE:
            case org.objectweb.asm.Type.SHORT:
            case org.objectweb.asm.Type.INT:
                mv.visitInsn(Opcodes.ICONST_0);
                break;
            case org.objectweb.asm.Type.FLOAT:
                mv.visitInsn(Opcodes.FCONST_0);
                break;
            case org.objectweb.asm.Type.LONG:
                mv.visitInsn(Opcodes.LCONST_0);
                break;
            case org.objectweb.asm.Type.DOUBLE:
                mv.visitInsn(Opcodes.DCONST_0);
                break;
            case org.objectweb.asm.Type.ARRAY:
            case org.objectweb.asm.Type.OBJECT:
                mv.visitInsn(Opcodes.ACONST_NULL);
                break;
            case org.objectweb.asm.Type.VOID:
                break;
            default:
                throw new RuntimeException("incorrect return Type or not implemented");
        }
    }
}
