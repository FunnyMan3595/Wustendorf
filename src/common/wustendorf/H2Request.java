package wustendorf;

import cpw.mods.fml.relauncher.ILibrarySet;

public class H2Request implements ILibrarySet
{
    private static String[] libraries = { "h2-1.3.169.jar" };
    private static String[] checksums = { "98330537871f7ded493a9386a210ee64fe21814d" };

    @Override
    public String[] getLibraries()
    {
        return libraries;
    }

    @Override
    public String[] getHashes()
    {
        return checksums;
    }

    @Override
    public String getRootURL()
    {
        return "http://hsql.sourceforge.net/m2-repo/com/h2database/h2/1.3.169/%s";
    }

}
