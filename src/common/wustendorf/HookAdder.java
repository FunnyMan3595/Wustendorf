package wustendorf;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class HookAdder extends AdviceAdapter {
    public int hook; 
    public static final int HOOK_LIGHT_OVERRIDE = 0;
    public static final int HOOK_CONSIDER_KILL = 1;

    public HookAdder(int which_hook, MethodVisitor delegate, int access, String name, String desc) {
        super(Opcodes.ASM4, delegate, access, name, desc);
        hook = which_hook;
    }

    @SuppressWarnings("unchecked")
    protected void onMethodEnter() {
        try {
            Class wd = Wustendorf.class;
            if (hook == HOOK_LIGHT_OVERRIDE) {
                /* We're writing this code:
                int override = wustendorf.Wustendorf.overrideSkyLight(this, par2, par3, par4);
                if (override != -1) {
                    return override;
                }
                */
                loadThis();     // this
                loadArgs(1, 3); // our own arguments 2-4


                // Ask Wustendorf if it has an override.
                String world = "up";
                invokeStatic(Type.getType(wd),
                             new Method("overrideBlockLight",
                                        "(L" + world + ";III)I"));

                dup();         // Duplicate the return value.
                Label keep_going = new Label();
                visitJumpInsn(Opcodes.IFLT, keep_going); // If the override set
                returnValue();                         // Use it.
                visitLabel(keep_going);                // Otherwise, keep going
                pop();                                 // (and nix the copy)
            } else if (hook == HOOK_CONSIDER_KILL) {
                /* We're writing this code:
                wustendorf.Wustendorf.considerKillingCritter(this);
                */
                loadThis(); // this

                String entityliving = "jw";
                invokeStatic(Type.getType(wd),
                             new Method("considerKillingCritter",
                                        "(L" + entityliving + ";)V"));
            }
        } catch (Exception e) {
            System.out.println("Wustendorf: HOOK FAILED!");
            e.printStackTrace();
        }
    }
}
