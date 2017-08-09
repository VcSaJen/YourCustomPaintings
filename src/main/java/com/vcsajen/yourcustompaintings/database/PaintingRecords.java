package com.vcsajen.yourcustompaintings.database;

import com.vcsajen.yourcustompaintings.exceptions.PaintingAlreadyExistsException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Note: Use ONLY from main thread
 * Created by VcSaJen on 07.08.2017 19:45.
 */
public class PaintingRecords {
    private SqlService sql;
    private String databasePath;
    private int pageLen = 10;

    private javax.sql.DataSource getDataSource(String jdbcUrl) throws SQLException {
        if (sql == null) {
            sql = Sponge.getServiceManager().provide(SqlService.class).orElseThrow(() -> new IllegalStateException("No SqlService in Sponge! This should never happen"));
        }
        return sql.getDataSource(jdbcUrl);
    }

    private javax.sql.DataSource getDataSource() throws SQLException {
        return getDataSource("jdbc:h2:"+databasePath);
    }

    public List<PaintingRecord> getAllPaintingsByUserPaged(UUID userId, int page)
    {
        List<PaintingRecord> result = new ArrayList<PaintingRecord>(pageLen);
        try {
            try (Connection conn = getDataSource().getConnection()) {
                PreparedStatement sqlState = conn.prepareStatement("SELECT PLAYER,NAME,MAPS_X,MAPS_Y,START_MAP_ID FROM PAINTINGS WHERE PLAYER = ? LIMIT ? OFFSET ?");
                sqlState.setObject(1, userId);
                sqlState.setInt(2, pageLen);
                sqlState.setInt(3, (page-1)*pageLen);
                sqlState.execute();
                ResultSet resultSet = sqlState.getResultSet();

                while (resultSet.next()) { //selects first row as current
                    result.add(packResultSetIntoRecord(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            return result;
        }
    }

    public List<PaintingRecord> getAllPaintingsPaged(int page)
    {
        List<PaintingRecord> result = new ArrayList<PaintingRecord>(pageLen);
        try {
            try (Connection conn = getDataSource().getConnection()) {
                PreparedStatement sqlState = conn.prepareStatement("SELECT PLAYER,NAME,MAPS_X,MAPS_Y,START_MAP_ID FROM PAINTINGS LIMIT ? OFFSET ?");
                sqlState.setInt(1, pageLen);
                sqlState.setInt(2, (page-1)*pageLen);
                sqlState.execute();
                ResultSet resultSet = sqlState.getResultSet();

                while (resultSet.next()) { //selects first row as current
                    result.add(packResultSetIntoRecord(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            return result;
        }
    }

    public int getAllPaintingsByUserCount(UUID userId)
    {
        try {
            try (Connection conn = getDataSource().getConnection()) {
                PreparedStatement sqlState = conn.prepareStatement("SELECT count(*) FROM PAINTINGS WHERE PLAYER = ?");
                sqlState.setObject(1, userId);
                sqlState.execute();
                ResultSet resultSet = sqlState.getResultSet();
                resultSet.next(); //selects first row as current
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public int getAllPaintingsCount()
    {
        try {
            try (Connection conn = getDataSource().getConnection()) {
                PreparedStatement sqlState = conn.prepareStatement("SELECT count(*) FROM PAINTINGS");
                sqlState.execute();
                ResultSet resultSet = sqlState.getResultSet();
                resultSet.next(); //selects first row as current
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    @Nullable
    public PaintingRecord getPainting(UUID userId, String name)
    {
        try {
            try (Connection conn = getDataSource().getConnection()) {
                PreparedStatement sqlState = conn.prepareStatement("SELECT PLAYER,NAME,MAPS_X,MAPS_Y,START_MAP_ID FROM PAINTINGS WHERE PLAYER = ? AND NAME = ?");
                sqlState.setObject(1, userId);
                sqlState.setString(2, name);
                sqlState.execute();
                ResultSet resultSet = sqlState.getResultSet();
                if (resultSet.next()) { //selects first row as current
                    return packResultSetIntoRecord(resultSet);
                } else return null;
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private PaintingRecord packResultSetIntoRecord(ResultSet resultSet) throws SQLException {
        return new PaintingRecord(
                (UUID)resultSet.getObject("PLAYER"),
                resultSet.getString("NAME"),
                resultSet.getInt("MAPS_X"),
                resultSet.getInt("MAPS_Y"),
                resultSet.getInt("START_MAP_ID"));
    }

    public boolean isPaintingExists(UUID userId, String name)
    {
        try {
            try (Connection conn = getDataSource().getConnection()) {
                PreparedStatement sqlState = conn.prepareStatement("SELECT count(1) FROM PAINTINGS WHERE PLAYER = ? AND NAME = ?");
                sqlState.setObject(1, userId);
                sqlState.setString(2, name);
                sqlState.execute();
                ResultSet resultSet = sqlState.getResultSet();
                resultSet.next(); //selects first row as current
                return resultSet.getInt(1)>0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public void addPainting(PaintingRecord painting) throws SQLException, PaintingAlreadyExistsException {
        try (Connection conn = getDataSource().getConnection()) {
            if (isPaintingExists(painting.getOwner(), painting.getName()))
                throw new PaintingAlreadyExistsException();
            PreparedStatement sqlState = conn.prepareStatement("INSERT INTO PAINTINGS (PLAYER,NAME,MAPS_X,MAPS_Y,START_MAP_ID) VALUES (?, ?, ?, ?, ?)");
            sqlState.setObject(1, painting.getOwner());
            sqlState.setString(2, painting.getName());
            sqlState.setInt(3, painting.getMapsX());
            sqlState.setInt(4, painting.getMapsY());
            sqlState.setInt(5, painting.getStartMapId());
            sqlState.executeUpdate();
        }
    }

    private void ensureTablesExist() throws SQLException {
        try (Connection conn = getDataSource().getConnection()) {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS PAINTINGS(ID INT PRIMARY KEY AUTO_INCREMENT, PLAYER UUID, NAME VARCHAR_IGNORECASE(255), "+
                    "MAPS_X INT, MAPS_Y INT, START_MAP_ID INT)").executeUpdate();
        }
    }

    public PaintingRecords(Path databasePath, int pageLen) throws SQLException {
        this.databasePath = databasePath.toString();
        this.pageLen = pageLen;
        ensureTablesExist();
    }
}
