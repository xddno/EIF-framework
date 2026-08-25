// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

package com.emberinjector.framework.internal.patcher;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.*;


// event handler format:
// public Object eventHandler(Object constant)
// return value overrides the constant pushed on stack
// return value must be one of String, Integer, Float, Double, Long, MethodType, MethodHandle, Class
// return value can't be null

public class LDCConstantModifier extends MethodModifier
{

    public LDCConstantModifier(MethodModifierInfo info)
    {
        super(Type.ON_LDC_CONSTANT, info);
    }

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor forwardTo, int access, String name, String descriptor)
    {
        MethodVisitor methodVisitor = new MethodVisitor(Opcodes.ASM9, forwardTo)
        {
            @Override
            public void visitLdcInsn(Object value)
            {
                super.visitLdcInsn(value);
                if (!(value instanceof String
                        || value instanceof  Integer
                        || value instanceof Float
                        || value instanceof Double
                        || value instanceof Long
                        || value instanceof org.objectweb.asm.Type
                        || value instanceof Handle))
                    return;

                if (value instanceof Integer)
                {
                    int intprimIndex = availableVarIndex;
                    availableVarIndex++;
                    mv.visitVarInsn(Opcodes.ISTORE, intprimIndex);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/Integer");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitVarInsn(Opcodes.ILOAD, intprimIndex);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
                }
                else if (value instanceof Float)
                {
                    int floatprimIndex = availableVarIndex;
                    availableVarIndex++;
                    mv.visitVarInsn(Opcodes.FSTORE, floatprimIndex);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/Float");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitVarInsn(Opcodes.FLOAD, floatprimIndex);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V", false);
                }
                else if (value instanceof Long)
                {
                    int longprimIndex = availableVarIndex;
                    availableVarIndex++;
                    mv.visitVarInsn(Opcodes.LSTORE, longprimIndex);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitVarInsn(Opcodes.LLOAD, longprimIndex);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V", false);
                }
                else if (value instanceof Double)
                {
                    int doubleprimIndex = availableVarIndex;
                    availableVarIndex++;
                    mv.visitVarInsn(Opcodes.DSTORE, doubleprimIndex);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/Double");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitVarInsn(Opcodes.DLOAD, doubleprimIndex);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V", false);
                }

                mv.visitMethodInsn(Opcodes.INVOKESTATIC, info.eventMethodClass, info.eventMethodName, info.eventMethodSignature, false);

                if (value instanceof Integer)
                {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                }
                else if (value instanceof Float)
                {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                }
                else if (value instanceof Long)
                {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                }
                else if (value instanceof Double)
                {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                }
                else if (value instanceof org.objectweb.asm.Type)
                {
                    int sort = ((org.objectweb.asm.Type)value).getSort();
                    if (sort == OBJECT || sort == ARRAY)
                        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Class");
                    else if (sort == METHOD)
                        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/invoke/MethodType");
                    else throw new RuntimeException("LDCConstantModifier: constant is a Type, but not supported");
                }
                else if (value instanceof Handle)
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/invoke/MethodHandle");
            }
        };
        return this.new AvailableIndexMethodVisitor(Opcodes.ASM9, methodVisitor);
    }

    @Override
    public String getNewInstanceCode()
    {
        return String.format("new LDCConstantModifier(%s)", this.info.getNewInstanceCode());
    }
}
