/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.tools.api.impl;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;

/**
 * Adapts members selected by {@link ApiMemberSelector}, stripping out method implementations and replacing them
 * with returning the default value for the method's return type.
 * Constructors throw {@link Error}.
 *
 * All members (including but not limited to stripped and stubbed methods) are delegated to a {@link ClassWriter}
 * responsible for writing new API classes.
 */
public class MethodStubbingApiMemberAdapter extends ClassVisitor {

    private final String exceptionClassName;

    public MethodStubbingApiMemberAdapter(ClassWriter cv) {
        this(cv, "java/lang/Error");
    }

    public MethodStubbingApiMemberAdapter(ClassWriter cv, String exceptionClassName) {
        super(Opcodes.ASM9, cv);
        this.exceptionClassName = exceptionClassName;
    }

    @SuppressWarnings("ReferenceEquality")
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if ((access & ACC_ABSTRACT) == 0) {
            mv.visitCode();

            if (name.equals("<init>")) {
                mv.visitTypeInsn(NEW, exceptionClassName);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(
                    INVOKESPECIAL, exceptionClassName, "<init>", "()V", false);
                mv.visitInsn(ATHROW);
                mv.visitMaxs(2, 0);
            } else {
                Type returnType = Type.getReturnType(desc);
                if (returnType == Type.VOID_TYPE) {
                    mv.visitInsn(Opcodes.RETURN);
                } else if (returnType == Type.BOOLEAN_TYPE
                    || returnType == Type.BYTE_TYPE
                    || returnType == Type.CHAR_TYPE
                    || returnType == Type.SHORT_TYPE
                    || returnType == Type.INT_TYPE) {
                    mv.visitInsn(Opcodes.ICONST_0);
                    mv.visitInsn(Opcodes.IRETURN);
                } else if (returnType == Type.LONG_TYPE) {
                    mv.visitInsn(Opcodes.LCONST_0);
                    mv.visitInsn(Opcodes.LRETURN);
                } else if (returnType == Type.FLOAT_TYPE) {
                    mv.visitInsn(Opcodes.FCONST_0);
                    mv.visitInsn(Opcodes.FRETURN);
                } else if (returnType == Type.DOUBLE_TYPE) {
                    mv.visitInsn(Opcodes.DCONST_0);
                    mv.visitInsn(Opcodes.DRETURN);
                } else {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitInsn(Opcodes.ARETURN);
                }

                mv.visitMaxs(0, 0);
            }

            mv.visitEnd();
        }
        return mv;
    }
}
