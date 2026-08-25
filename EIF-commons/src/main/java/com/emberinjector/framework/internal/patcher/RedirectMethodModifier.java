// MIT License. Copyright (c) EIF-framework author. See NOTICE.md.
// SPDX-License-Identifier: MIT

package com.emberinjector.framework.internal.patcher;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class RedirectMethodModifier extends MethodModifier
{
    public final String targetOwner;
    public final String targetName;
    public final String targetDesc;

    public RedirectMethodModifier(MethodModifierInfo info, String targetOwner, String targetName, String targetDesc)
    {
        super(Type.ON_REDIRECT, info);
        this.targetOwner = targetOwner;
        this.targetName = targetName;
        this.targetDesc = targetDesc;
    }

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor forwardTo, int access, String name, String descriptor)
    {
        return new AdviceAdapter(Opcodes.ASM9, forwardTo, access, name, descriptor)
        {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
            {
                if (owner.equals(targetOwner) && name.equals(targetName) && desc.equals(targetDesc))
                {
                    pushParametersAndCallEventHandler(mv);
                }
                else
                {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        };
    }

    @Override
    public String getNewInstanceCode()
    {
        return String.format("new RedirectMethodModifier(%s, \"%s\", \"%s\", \"%s\")",
                info.getNewInstanceCode(), targetOwner, targetName, targetDesc);
    }
}
