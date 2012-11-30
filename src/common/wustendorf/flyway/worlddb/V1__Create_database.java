package wustendorf.flyway.worlddb;

import com.googlecode.flyway.core.api.migration.jdbc.JdbcMigration;
import java.sql.*;

public class V1__Create_database implements JdbcMigration {
    public void migrate(Connection conn) throws Exception {
        Statement direct = conn.createStatement();

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
}
