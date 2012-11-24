package wustendorf;

import net.minecraft.src.WorldServer;

import java.io.*;
import java.sql.*;
import java.util.*;

public class WustendorfDB {
    private Connection conn = null;
    private Statement direct = null;
    private Map<String, PreparedStatement> statementCache = new HashMap<String, PreparedStatement>();

    public WustendorfDB(WorldServer world) {
        File dbFile = new File(world.getChunkSaveLocation(), "wustendorf");
        try {
            conn = DriverManager.getConnection("jdbc:h2:" + dbFile);
            direct = conn.createStatement();

            createTables();
        } catch (Exception e) {
            System.out.println("Wustendorf: Unable to open database " + dbFile);
            e.printStackTrace();

            conn = null;
            direct = null;
        }
    }

    public void createTables() throws SQLException {
        String[] sql = new String[] {
            "CREATE TABLE IF NOT EXISTS Markers (marker_id INTEGER AUTO_INCREMENT UNIQUE, marker_x INTEGER NOT NULL, marker_y INTEGER NOT NULL, marker_z INTEGER NOT NULL, range INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(marker_x, marker_y, marker_z));",
            "CREATE TABLE IF NOT EXISTS Tags (marker_id INTEGER NOT NULL, tag_name VARCHAR(32) NOT NULL, value INTEGER NOT NULL, PRIMARY KEY(marker_id, tag_name));",
            "CREATE TABLE IF NOT EXISTS Rooms (marker_id INTEGER NOT NULL, room_id INTEGER NOT NULL, x_min INTEGER NOT NULL, x_max INTEGER NOT NULL, y_min INTEGER NOT NULL, y_max INTEGER NOT NULL, z_min INTEGER NOT NULL, z_max INTEGER NOT NULL, room_x INTEGER NOT NULL, room_y INTEGER NOT NULL, room_z INTEGER NOT NULL, PRIMARY KEY(marker_id, room_id));",
            "CREATE TABLE IF NOT EXISTS ImportantBlocks (marker_id INTEGER NOT NULL, room_id INTEGER NOT NULL, block_x INTEGER NOT NULL, block_y INTEGER NOT NULL, block_z INTEGER NOT NULL, type INTEGER NOT NULL, PRIMARY KEY(block_x, block_y, block_z));"
        };

        for (String instruction : sql) {
            direct.executeUpdate(instruction);
        }
    }

    protected PreparedStatement getCachedStatement(String query) {
        PreparedStatement stmt = statementCache.get(query);
        if (stmt == null) {
            try {
                stmt = conn.prepareStatement(query);
                statementCache.put(query, stmt);
            } catch (Exception e) {
                System.out.println("Wustendorf: Database error:");
                e.printStackTrace();
            }
        }

        return stmt;
    }

    protected void execute(String query, Object... params) {
        try {
            PreparedStatement stmt = getCachedStatement(query);
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

    protected ResultSet getResults(String query, Object... params) throws SQLException {
        PreparedStatement stmt = getCachedStatement(query);
        for (int i=0; i<params.length; i++) {
            stmt.setObject(i+1, params[i]);
        }

        return stmt.executeQuery();
    }

    protected int getInt(String query, Object... params) {
        try {
            ResultSet results = getResults(query, params);

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

    protected List<List<Integer>> getIntMatrix(int len, String query, Object... params) {
        try {
            ResultSet results = getResults(query, params);

            List<List<Integer>> matrix = new ArrayList<List<Integer>>();

            while (results.next()) {
                List<Integer> row = new ArrayList<Integer>();

                for (int i=0; i<len; i++) {
                    row.add(results.getInt(i+1));
                }

                matrix.add(row);
            }


            return matrix;
        } catch (Exception e) {
            if (conn != null) {
                System.out.println("Wustendorf: Database error:");
                e.printStackTrace();
            }
            return null;
        }
    }

    protected int[] getIntArray(int len, String query, Object... params) {
        try {
            ResultSet results = getResults(query, params);

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

    protected Map<String, Integer> getIntMap(String query, Object... params) {
        try {
            ResultSet results = getResults(query, params);

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

    public int getMarkerID(int x, int y, int z) {
        return getInt("SELECT marker_id FROM Markers WHERE marker_x=? AND marker_y=? AND marker_z=?;", x, y, z);
    }

    public List<List<Integer>> getMarkersInPhase(int phase) {
        return getIntMatrix(3, "SELECT marker_x, marker_y, marker_z FROM Markers WHERE (marker_id % 100)=?;", phase);
    }

    public int addMarker(int x, int y, int z) {
        int marker = getMarkerID(x, y, z);
        if (marker != -1) {
            return -1;
        }

        execute("INSERT INTO Markers VALUES (DEFAULT, ?, ?, ?, DEFAULT);", x, y, z);

        return getMarkerID(x, y, z);
    }

    public void removeMarker(int x, int y, int z) {
        int marker = getMarkerID(x, y, z);

        if (marker != -1) {
            execute("DELETE FROM Tags WHERE marker_id=?;", marker);
            execute("DELETE FROM Markers WHERE marker_id=?;", marker);
            execute("DELETE FROM Rooms WHERE marker_id=?;", marker);
            execute("DELETE FROM ImportantBlocks WHERE marker_id=?;", marker);
        }
    }

    public void addRoom(int marker, int room_id, int x_min, int x_max, int y_min, int y_max, int z_min, int z_max, int x_origin, int y_origin, int z_origin) {
        execute("INSERT INTO Rooms VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                marker, room_id, x_min, x_max, y_min, y_max, z_min, z_max,
                x_origin, y_origin, z_origin);
    }

    public int[] roomAt(int x, int y, int z) {
        return getIntArray(2, "SELECT marker_id, room_id FROM Rooms WHERE ? BETWEEN x_min AND x_max AND ? BETWEEN y_min AND y_max AND ? between z_min AND z_max;", x, y, z);
    }

    public void addImportantBlock(int marker, int room_id, int x, int y, int z, int type) {
        execute("INSERT INTO ImportantBlocks VALUES (?, ?, ?, ?, ?, ?);",
                marker, room_id, x, y, z, type);
    }

    public int getRange(int x, int y, int z) {
        return getInt("SELECT range FROM Markers WHERE marker_x=? AND marker_y=? AND marker_z=?;", x, y, z);
    }

    public void setRange(int value, int x, int y, int z) {
        execute("UPDATE Markers SET range=? WHERE marker_x=? AND marker_y=? AND marker_z=?;", value, x, y, z);
    }

    public void clearTag(String tag, int x, int y, int z) {
        int marker = getMarkerID(x, y, z);
        if (marker != -1) {
            execute("DELETE FROM Tags WHERE marker_id=? AND tag_name=?;", marker, tag);
        }
    }

    public void setTag(int value, String tag, int x, int y, int z) {
        int marker = getMarkerID(x, y, z);
        if (marker != -1) {
            execute("DELETE FROM Tags WHERE marker_id=? AND tag_name=?;", marker, tag);
            execute("INSERT INTO Tags VALUES (?, ?, ?);", marker, tag, value);
        }
    }

    public int getMarkerCount() {
        return getInt("SELECT COUNT(*) FROM Markers;");
    }

    public int[] getMarker(int index) {
        return getIntArray(3, "SELECT marker_x, marker_y, marker_z FROM Markers LIMIT 1 OFFSET ?;", index);
    }

    public int getTag(String tag, int x, int y, int z) {
        return getInt("SELECT value FROM Tags NATURAL JOIN Markers WHERE tag_name=? AND marker_x=? AND marker_y=? AND marker_z=?;", tag, x, y, z);
    }

    public Map<String, Integer> getAllTags(int x, int y, int z) {
        return getIntMap("SELECT tag_name, value FROM Tags NATURAL JOIN Markers WHERE marker_x=? AND marker_y=? AND marker_z=?;", x, y, z);
    }

    public Map<String, Integer> getMatchingTags(String match, int x, int y, int z) {
        return getIntMap("SELECT tag_name, value FROM Tags NATURAL JOIN Markers WHERE tag_name LIKE ? AND marker_x=? AND marker_y=? AND marker_z=?;", match, x, y, z);
    }

    public List<List<Integer>> getImportantBlocks(int x, int y, int z) {
        return getIntMatrix(3, "SELECT block_x, block_y, block_z from ImportantBlocks NATURAL JOIN Markers WHERE marker_x=? AND marker_y=? AND marker_z=?", x, y, z);
    }

    public List<List<Integer>> getMarkersWithTag(String match) {
        return getIntMatrix(5, "SELECT marker_x, marker_y, marker_z, range, value FROM Tags NATURAL JOIN Markers WHERE tag_name=?;", match);
    }

    public int getStrongestInRange(String tag, int x, int z) {
        return getInt("SELECT MAX(value) FROM Tags NATURAL JOIN Markers WHERE tag_name=? AND ABS(marker_x-?)<=range AND ABS(marker_z-?)<=range;", tag, x, z);
    }

    public void close() {
        try {
            conn.close();
        } catch (Exception e) {
            System.out.println("Wustendorf: Error while closing database:");
            e.printStackTrace();
        }
    }
}
