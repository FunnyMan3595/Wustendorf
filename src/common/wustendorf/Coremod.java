package wustendorf;

import cpw.mods.fml.relauncher.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

@IFMLLoadingPlugin.TransformerExclusions("wustendorf.")
public class Coremod implements IFMLLoadingPlugin {
    public String[] getLibraryRequestClass() {
        return new String[] {
            "cpw.mods.fml.relauncher.CoreFMLLibraries",
            "wustendorf.H2Request"
        };
    }

    public String[] getASMTransformerClass() {
        return new String[] {"wustendorf.HookFinder"};
    }

    public String getModContainerClass() {
        return null;
    }

    public String getSetupClass() {
        return null;
    }

    public void injectData(Map<String, Object> data) { }
}
