package wustendorf;

import java.io.*;
import java.util.*;

import cpw.mods.fml.relauncher.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class HookFinder extends ClassVisitor implements IClassTransformer {
    // The master table of classes and their hooks.
    public Map<String, Map<String, Integer>> class_table;

    // The current class's hooks.
    public Map<String, Integer> hook_table = null;

    public ClassWriter writer = null;

    public HookFinder() {
        super(Opcodes.ASM4);

        class_table = new HashMap<String, Map<String, Integer>>();

        String world_class = "%conf:OBF_WORLD%";
        String compute_block_light = "%conf:OBF_COMPUTE_BLOCK_LIGHT%(IIIIII)I";

        String entityliving_class = "%conf:OBF_ENTITY_LIVING%";
        String update = "%conf:OBF_ON_LIVING_UPDATE%()V";

        Map<String, Integer> world = new HashMap<String, Integer>();
        world.put(compute_block_light, HookAdder.HOOK_LIGHT_OVERRIDE);
        class_table.put(world_class, world);

        Map<String, Integer> entityliving = new HashMap<String, Integer>();
        entityliving.put(update, HookAdder.HOOK_CONSIDER_KILL);
        class_table.put(entityliving_class, entityliving);
    }

    public byte[] transform(String name, byte[] bytes) {
        hook_table = class_table.get(name);

        if (hook_table == null) {
            return bytes;
        }

        try {
            ClassReader reader = new ClassReader(bytes);
            writer = new ClassWriter(reader, 0);

            reader.accept(this, ClassReader.EXPAND_FRAMES);

            byte[] output = writer.toByteArray();

            /*
            FileOutputStream fos = new FileOutputStream("test-" + name + ".class");
            fos.write(output);
            fos.close();
            */

            return output;
        } catch (Exception e) {
            System.out.println("Wustendorf: Class transform FAILED!");
            e.printStackTrace();

            return bytes;
        }
    }

    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        System.out.println("Wustendorf: Recognized class " + name + ".");

        writer.visit(version, access, name, signature, superName, interfaces);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return writer.visitAnnotation(desc, visible);
    }

    public void visitAttribute(Attribute attr) {
        writer.visitAttribute(attr);
    }

    public void visitEnd() {
        hook_table = null;
        writer.visitEnd();
    }

    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        return writer.visitField(access, name, desc, signature, value);
    }

    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
        writer.visitInnerClass(name, outerName, innerName, access);
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor write_it = writer.visitMethod(access, name, desc,
                                                    signature, exceptions);

        if (hook_table != null) {
            Integer hook = hook_table.get(name + desc);
            if (hook != null) {
                System.out.println("Wustendorf: Adding hook " + hook + ".");
                try {
                    return new HookAdder(hook, write_it, access, name, desc);
                } catch (Exception e) {
                    System.out.println("Wustendorf: HOOK FAILED!");
                    e.printStackTrace();
                }
            }
        }

        return write_it;
    }

    public void visitOuterClass(String owner, String name, String desc) {
        writer.visitOuterClass(owner, name, desc);
    }

    public void visitSource(String source, String debug) {
        writer.visitSource(source, debug);
    }
}
