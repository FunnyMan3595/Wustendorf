package wustendorf;

import java.util.*;

public class DBMarker {
    public final WustendorfDB db;
    public final int x;
    public final int y;
    public final int z;

    public Integer id = null;
    public Integer range = null;
    public Map<String, Integer> tags = new HashMap<String, Integer>();

    public DBMarker(WustendorfDB worldDB, int x, int y, int z) {
        this.db = worldDB;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean exists() {
        return exists(true);
    }

    public boolean exists(boolean cached) {
        return getID(cached, false) != null;
    }

    public Integer ensureExists() {
        return getID(false, true);
    }

    public Integer getID() {
        return getID(true, false);
    }

    public Integer getID(boolean cached, boolean create) {
        // If we have a cached value and it was requested, use it.
        if (cached && id != null) {
            return id;
        }

        // Get the current value from the database.
        id = db.getMarkerID(x, y, z);

        // If there is no current value and creation is requested, do it.
        if (create && id == null) {
            id = db.addMarker(x, y, z);
            tags.clear();
        }
        return id;
    }

    public void delete() {
        if (exists()) {
            db.removeMarker(id);
        }

        id = null;
        range = null;
        tags.clear();
    }

    public Integer getTag(String name) {
        return getTag(name, true);
    }

    public Integer getTag(String name, boolean cached) {
        // If we don't exist, we have no tags.
        if (!exists()) {
            return null;
        }

        // Forward the pseudo-tag "range" to its actual value.
        if (name == "range") {
            return getRange(cached);
        }

        // If we have a cached value and it was requested, use it.
        Integer value = null;
        if (cached) {
            value = tags.get(name);
            if (value != null) {
                return value;
            }
        }

        // Get the current value from the database.
        value = db.getTag(getID(), name);

        // Cache the value.
        tags.put(name, value);

        return value;
    }

    public Integer getRange() {
        return getRange(true);
    }

    public Integer getRange(boolean cached) {
        // If we don't exist, we have no range.
        if (!exists()) {
            return null;
        }
        // If we have a cached value and it was requested, use it.
        if (cached && range != null) {
            return range;
        }

        // Get the current value from the database.
        range = db.getRange(getID());

        return range;
    }

    public void setTag(String tag, Integer value) {
        // Forward the pseudo-tag "range" to its actual location.
        if (tag.equals("range")) {
            if (value != null) {
                setRange(value);
            }

            return;
        }

        // null value -> Delete it.
        if (value == null) {
            removeTag(tag);
        }

        ensureExists();
        // Update the cache.
        tags.put(tag, value);
        db.setTag(id, tag, value);
    }

    public void removeTag(String name) {
        if (exists()) {
            db.clearTag(id, name);
        }

        // Update the cache.
        tags.remove(name);
    }

    public void setRange(int value) {
        if (!exists()) {
            // Since range is part of the basic marker data, we can avoid an
            // extra query by creating and setting range at the same time.
            id = db.addMarkerWithRange(x, y, z, range);
        } else {
            db.setRange(id, value);
        }
    }

    public Map<String, Integer> getAllTags() {
        if (exists()) {
            tags = db.getAllTags(id);
            return tags;
        }

        // No marker, no tags.
        tags.clear();
        return null;
    }

    public Map<String, Integer> getMatchingTags(String match) {
        if (exists()) {
            Map<String, Integer> newTags = db.getMatchingTags(id, match);
            // Store these tags in the cache.
            tags.putAll(newTags);
            return newTags;
        }

        // No marker, no tags.
        tags.clear();
        return null;
    }
}
