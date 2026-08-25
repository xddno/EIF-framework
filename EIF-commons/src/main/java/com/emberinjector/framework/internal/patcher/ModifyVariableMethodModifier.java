// MIT License. Copyright (c) EIF-framework author. See NOTICE.md.
// SPDX-License-Identifier: MIT

package com.emberinjector.framework.internal.patcher;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class ModifyVariableMethodModifier extends MethodModifier
{
    public final int ordinal;
    public final String variableName;

    public ModifyVariableMethodModifier(MethodModifierInfo info, int ordinal, String variableName)
    {
        super(Type.ON_MODIFY_VARIABLE, info);
        this.ordinal = ordinal;
        this.variableName = variableName;
    }

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor forwardTo, int access, String name, String descriptor)
    {
        return new AdviceAdapter(Opcodes.ASM9, forwardTo, access, name, descriptor)
        {
            private int currentOrdinal = 0;

            private boolean isStore(int opcode)
            {
                return opcode == Opcodes.ISTORE || opcode == Opcodes.FSTORE
                    || opcode == Opcodes.DSTORE || opcode == Opcodes.LSTORE
                    || opcode == Opcodes.ASTORE;
            }

            @Override
            public void visitVarInsn(int opcode, int varIndex)
            {
                if (isStore(opcode))
                {
                    if (currentOrdinal == ordinal)
                    {
                        int storeIndex = availableVarIndex;
                        availableVarIndex++;
                        mv.visitVarInsn(opcode, varIndex);
                        mv.visitVarInsn(Opcodes.ALOAD, storeIndex);
                        pushParametersAndCallEventHandler(mv);
                        currentOrdinal++;
                        return;
                    }
                    currentOrdinal++;
                }
                super.visitVarInsn(opcode, varIndex);
            }
        };
    }

    @Override
    public String getNewInstanceCode()
    {
        return String.format("new ModifyVariableMethodModifier(%s, %d, \"%s\")",
                info.getNewInstanceCode(), ordinal, variableName);
    }
}
