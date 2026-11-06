package fi.metropolia.tempconverter.dao;

import fi.metropolia.tempconverter.db.DBConnection;
import fi.metropolia.tempconverter.model.TemperatureUnit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TemperatureUnitDAO {

    private static final String FALLBACK_LANG = "en";

    private static final String SELECT_LOCALIZED =
            "SELECT u.unit_id, u.unit_key, u.symbol, "
                    + "       COALESCE(t.name, f.name)               AS name, "
                    + "       COALESCE(t.description, f.description) AS description "
                    + "FROM temperature_units u "
                    + "LEFT JOIN unit_translation t ON t.unit_id = u.unit_id AND t.lang = ? "
                    + "LEFT JOIN unit_translation f ON f.unit_id = u.unit_id AND f.lang = ? ";

    public List<TemperatureUnit> findAllLocalized(String lang) throws SQLException {
        List<TemperatureUnit> units = new ArrayList<>();
        String sql = SELECT_LOCALIZED + "ORDER BY u.unit_id";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, lang);
            ps.setString(2, FALLBACK_LANG);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    units.add(mapRow(rs));
                }
            }
        }
        return units;
    }

    public TemperatureUnit findByIdLocalized(int unitId, String lang) throws SQLException {
        String sql = SELECT_LOCALIZED + "WHERE u.unit_id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, lang);
            ps.setString(2, FALLBACK_LANG);
            ps.setInt(3, unitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public TemperatureUnit findByKeyLocalized(String unitKey, String lang) throws SQLException {
        String sql = SELECT_LOCALIZED + "WHERE u.unit_key = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, lang);
            ps.setString(2, FALLBACK_LANG);
            ps.setString(3, unitKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int insert(String unitKey, String symbol) throws SQLException {
        String sql = "INSERT INTO temperature_units (unit_key, symbol) VALUES (?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, unitKey);
            ps.setString(2, symbol);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean addTranslation(int unitId, String lang, String name, String description)
            throws SQLException {
        String sql = "INSERT INTO unit_translation (unit_id, lang, name, description) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, unitId);
            ps.setString(2, lang);
            ps.setString(3, name);
            ps.setString(4, description);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int unitId) throws SQLException {
        String sql = "DELETE FROM temperature_units WHERE unit_id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, unitId);
            return ps.executeUpdate() > 0;
        }
    }

    private TemperatureUnit mapRow(ResultSet rs) throws SQLException {
        return new TemperatureUnit(
                rs.getInt("unit_id"),
                rs.getString("unit_key"),
                rs.getString("symbol"),
                rs.getString("name"),
                rs.getString("description"));
    }
}