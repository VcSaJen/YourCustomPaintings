package com.vcsajen.yourcustompaintings.database;

import java.util.UUID;

/**
 * Created by VcSaJen on 07.08.2017 19:45.
 */
public class PaintingRecord {
    private UUID owner;
    private String name;
    private int mapsX;
    private int mapsY;
    private int startMapId;

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public int getMapsX() {
        return mapsX;
    }

    public int getMapsY() {
        return mapsY;
    }

    public int getStartMapId() {
        return startMapId;
    }

    /*public int getEndMapId() {
        return startMapId+getLengthMapId()-1;
    }*/

    public int getLengthMapId() {
        return mapsX*mapsY;
    }

    public PaintingRecord(UUID owner, String name, int mapsX, int mapsY, int startMapId) {
        this.owner = owner;
        this.name = name;
        this.mapsX = mapsX;
        this.mapsY = mapsY;
        this.startMapId = startMapId;
    }
}
