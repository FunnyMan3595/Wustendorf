package wustendorf;

import cpw.mods.fml.relauncher.ILibrarySet;

public class FlywayRequest implements ILibrarySet
{
    private static String[] libraries = { "flyway-core-2.0.2.jar" };
    private static String[] checksums = { "635d665d076e0961a9885ff46e4a8c05426f2a49" };

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
        return "http://flyway.googlecode.com/files/%s";
    }

}
