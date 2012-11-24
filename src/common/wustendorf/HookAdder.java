package wustendorf;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class HookAdder extends AdviceAdapter {
    public int hook; 
    public static final int HOOK_LIGHT_OVERRIDE = 0;
    public static final int HOOK_LIGHT_DISPLAY = 1;
    public static final int HOOK_LIGHT_OVERRIDE_CACHE = 10;
    public static final int HOOK_LIGHT_DISPLAY_CACHE = 11;
    public static final int HOOK_CONSIDER_KILL = 100;

    public HookAdder(int which_hook, MethodVisitor delegate, int access, String name, String desc) {
        super(Opcodes.ASM4, delegate, access, name, desc);
        hook = which_hook;
    }

    @SuppressWarnings("unchecked")
    protected void onMethodEnter() {
        try {
            Class wd = Wustendorf.class;
            if (hook == HOOK_LIGHT_OVERRIDE || hook == HOOK_LIGHT_DISPLAY || hook == HOOK_LIGHT_OVERRIDE_CACHE || hook == HOOK_LIGHT_DISPLAY_CACHE) {
                /* We're writing this code:
                int override = wustendorf.Wustendorf.overrideSkyLight(this, par2, par3, par4);
                if (override != -1) {
                    return override;
                }
                */
                loadThis();     // this

                if (hook == HOOK_LIGHT_OVERRIDE_CACHE || hook == HOOK_LIGHT_DISPLAY_CACHE) {
                    getField(Type.getType("L%conf:OBF_CHUNK_CACHE%;"),
                             "%conf:OBF_CHUNK_CACHE_WORLD%",
                             Type.getType("L%conf:OBF_WORLD%;"));
                }
                loadArgs(0, 3); // our own arguments 1-3


                // Ask Wustendorf if it has an override.
                String world = "%conf:OBF_WORLD%";
                if (hook == HOOK_LIGHT_OVERRIDE || hook == HOOK_LIGHT_OVERRIDE_CACHE) {
                    invokeStatic(Type.getType(wd),
                                 new Method("overrideLight",
                                            "(L" + world + ";III)I"));
                } else { // if (hook == HOOK_LIGHT_DISPLAY) {
                    invokeStatic(Type.getType(wd),
                                 new Method("overrideLightDisplay",
                                            "(L" + world + ";III)I"));
                }

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

                String entityliving = "%conf:OBF_ENTITY_LIVING%";
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
