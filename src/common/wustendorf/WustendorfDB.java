package wustendorf;

import net.minecraft.src.WorldServer;

import java.io.*;
import java.sql.*;
import java.util.*;

public class WustendorfDB {
    private Connection conn = null;
    private Statement direct = null;
    private PreparedStatement insertMarker = null;
    private PreparedStatement deleteAllTags = null;
    private PreparedStatement deleteMarker = null;
    private PreparedStatement countMarkers = null;
    private PreparedStatement getMarkerPos = null;
    private PreparedStatement getMarkerAt = null;
    private PreparedStatement getMarkerRange = null;
    private PreparedStatement setMarkerRange = null;
    private PreparedStatement strongestInRange = null;
    private PreparedStatement getTagValue = null;
    private PreparedStatement allTags = null;
    private PreparedStatement clearTagValue = null;
    private PreparedStatement setTagValue = null;

    public WustendorfDB(WorldServer world) {
        File dbFile = new File(world.getChunkSaveLocation(), "wustendorf");
        try {
            conn = DriverManager.getConnection("jdbc:h2:" + dbFile);
            direct = conn.createStatement();

            createTables();
            prepareStatements();
        } catch (Exception e) {
            System.out.println("Wustendorf: Unable to open database " + dbFile);
            e.printStackTrace();

            conn = null;
            direct = null;
        }
    }

    public void createTables() throws SQLException {
        String[] sql = new String[] {
            "CREATE TABLE IF NOT EXISTS Markers (marker_id INTEGER AUTO_INCREMENT UNIQUE, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, range INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(x, y, z));",
            "CREATE TABLE IF NOT EXISTS Tags (marker_id INTEGER NOT NULL, tag_name VARCHAR(32) NOT NULL, value INTEGER NOT NULL, PRIMARY KEY(marker_id, tag_name), FOREIGN KEY(marker_id) REFERENCES Markers(marker_id);"
        };

        for (String instruction : sql) {
            direct.executeUpdate(instruction);
        }
    }

    public void prepareStatements() throws SQLException {
        insertMarker = conn.prepareStatement("INSERT INTO Markers VALUES (NULL, ?, ?, ?, 0);");
        deleteAllTags = conn.prepareStatement("DELETE FROM Tags WHERE marker_id=?;");
        deleteMarker = conn.prepareStatement("DELETE FROM Markers WHERE marker_id=?;");
        countMarkers = conn.prepareStatement("SELECT COUNT(*) FROM Markers;");
        getMarkerAt = conn.prepareStatement("SELECT marker_id FROM Markers WHERE x=? AND y=? AND z=?;");
        getMarkerPos = conn.prepareStatement("SELECT x, y, z FROM Markers LIMIT 1 OFFSET ?;");
        getMarkerRange = conn.prepareStatement("SELECT range FROM Markers WHERE x=? AND y=? AND z=?;");
        setMarkerRange = conn.prepareStatement("UPDATE Markers SET range=? WHERE x=? AND y=? AND z=?;");
        strongestInRange = conn.prepareStatement("SELECT MAX(value) FROM Tags NATURAL JOIN Markers WHERE tag_name=? AND ABS(x-?)<=range AND ABS(z-?)<=range;");
        getTagValue = conn.prepareStatement("SELECT value FROM Tags NATURAL JOIN Markers WHERE tag_name=? AND x=? AND y=? AND z=?;");
        allTags = conn.prepareStatement("SELECT tag_name, value FROM Tags NATURAL JOIN Markers WHERE x=? AND y=? AND z=?;");
        clearTagValue = conn.prepareStatement("DELETE FROM Tags WHERE marker_id=? AND tag_name=?;");
        setTagValue = conn.prepareStatement("INSERT INTO Tags VALUES (?, ?, ?);");
    }

    protected void execute(PreparedStatement stmt, Object... params) {
        try {
            for (int i=0; i<params.length; i++) {
                stmt.setObject(i+1, params[i]);
            }

            stmt.executeUpdate();
        } catch (Exception e) {
            if (conn != null) {
                System.out.println("Wustendorf: Database error:");
                e.printStackTrace();
            }
        }
    }

    protected ResultSet getResults(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i=0; i<params.length; i++) {
            stmt.setObject(i+1, params[i]);
        }

        return stmt.executeQuery();
    }

    protected int getInt(PreparedStatement stmt, Object... params) {
        try {
            ResultSet results = getResults(stmt, params);

            if (!results.next()) {
                return -1;
            }

            return results.getInt(1);
        } catch (Exception e) {
            if (conn != null) {
                System.out.println("Wustendorf: Database error:");
                e.printStackTrace();
            }
        }

        return -1;
    }

    protected int[] getIntArray(int len, PreparedStatement stmt, Object... params) {
        try {
            ResultSet results = getResults(stmt, params);

            if (!results.next()) {
                return new int[0];
            }

            int[] output = new int[len];
            for (int i=0; i<len; i++) {
                output[i] = results.getInt(i+1);
            }

            return output;
        } catch (Exception e) {
            if (conn != null) {
                System.out.println("Wustendorf: Database error:");
                e.printStackTrace();
            }
            return new int[0];
        }
    }

    protected Map<String, Integer> getIntMap(PreparedStatement stmt, Object... params) {
        try {
            ResultSet results = getResults(stmt, params);

            Map<String, Integer> map = new HashMap<String, Integer>();

            while (results.next()) {
                String key = results.getString(1);
                int value = results.getInt(2);

                map.put(key, value);
            }

            return map;
        } catch (Exception e) {
            if (conn != null) {
                System.out.println("Wustendorf: Database error:");
                e.printStackTrace();
            }
        }

        return null;
    }

    public void addMarker(int x, int y, int z) {
        execute(insertMarker, x, y, z);
    }

    public void removeMarker(int x, int y, int z) {
        int marker = getInt(getMarkerAt, x, y, z);

        if (marker != -1) {
            execute(deleteAllTags, marker);
            execute(deleteMarker, marker);
        }
    }

    public int getRange(int x, int y, int z) {
        return getInt(getMarkerRange, x, y, z);
    }

    public void setRange(int value, int x, int y, int z) {
        execute(setMarkerRange, value, x, y, z);
    }

    public void clearTag(String tag, int x, int y, int z) {
        int marker = getInt(getMarkerAt, x, y, z);
        if (marker != -1) {
            execute(clearTagValue, marker, tag);
        }
    }

    public void setTag(int value, String tag, int x, int y, int z) {
        int marker = getInt(getMarkerAt, x, y, z);
        if (marker != -1) {
            execute(clearTagValue, marker, tag);
            execute(setTagValue, marker, tag, value);
        }
    }

    public int getMarkerCount() {
        return getInt(countMarkers);
    }

    public int[] getMarker(int index) {
        return getIntArray(3, getMarkerPos, index);
    }

    public int getTag(String tag, int x, int y, int z) {
        return getInt(getTagValue, tag, x, y, z);
    }

    public Map<String, Integer> getAllTags(int x, int y, int z) {
        return getIntMap(allTags, x, y, z);
    }

    public int getStrongestInRange(String tag, int x, int z) {
        return getInt(strongestInRange, tag, x, z);
    }

    public void close() {
        try {
            conn.close();
        } catch (Exception e) {
            System.out.println("Wustendorf: Error while closing database:");
            e.printStackTrace();
        }
    }

    public int getRegionLight(int x, int z) {
        return getStrongestInRange("light", x, z);
    }
}
