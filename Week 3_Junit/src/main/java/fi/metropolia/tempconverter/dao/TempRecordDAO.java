package fi.metropolia.tempconverter.dao;

import fi.metropolia.tempconverter.db.DBConnection;
import fi.metropolia.tempconverter.model.TempRecord;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TempRecordDAO {

    private static final String FALLBACK_LANG = "en";

    private static final String SELECT_WITH_NAMES =
            "SELECT r.record_id, r.input_value, r.converted_value, "
                    + "       r.from_unit_id, r.to_unit_id, r.lang, r.created_at, "
                    + "       COALESCE(ft.name, ff.name) AS from_name, "
                    + "       COALESCE(tt.name, tf.name) AS to_name "
                    + "FROM temp_records r "
                    + "LEFT JOIN unit_translation ft ON ft.unit_id = r.from_unit_id AND ft.lang = ? "
                    + "LEFT JOIN unit_translation ff ON ff.unit_id = r.from_unit_id AND ff.lang = ? "
                    + "LEFT JOIN unit_translation tt ON tt.unit_id = r.to_unit_id   AND tt.lang = ? "
                    + "LEFT JOIN unit_translation tf ON tf.unit_id = r.to_unit_id   AND tf.lang = ? ";

    public int insert(TempRecord record) throws SQLException {
        String sql = "INSERT INTO temp_records "
                + "(input_value, converted_value, from_unit_id, to_unit_id, lang) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, record.getInputValue());
            ps.setDouble(2, record.getConvertedValue());
            ps.setInt(3, record.getFromUnitId());
            ps.setInt(4, record.getToUnitId());
            ps.setString(5, record.getLang());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    record.setRecordId(keys.getInt(1));
                    return record.getRecordId();
                }
            }
        }
        return -1;
    }

    /** The last 5 conversions, unit names in the selected language. */
    public List<TempRecord> findLatest(int limit, String lang) throws SQLException {
        List<TempRecord> records = new ArrayList<>();
        String sql = SELECT_WITH_NAMES + "ORDER BY r.record_id DESC LIMIT ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            bindLangs(ps, lang);
            ps.setInt(5, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
            }
        }
        return records;
    }

    public TempRecord findById(int recordId, String lang) throws SQLException {
        String sql = SELECT_WITH_NAMES + "WHERE r.record_id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            bindLangs(ps, lang);
            ps.setInt(5, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean delete(int recordId) throws SQLException {
        String sql = "DELETE FROM temp_records WHERE record_id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, recordId);
            return ps.executeUpdate() > 0;
        }
    }

    public int deleteAll() throws SQLException {
        try (Statement st = DBConnection.getConnection().createStatement()) {
            return st.executeUpdate("DELETE FROM temp_records");
        }
    }

    public int count() throws SQLException {
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM temp_records")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private void bindLangs(PreparedStatement ps, String lang) throws SQLException {
        ps.setString(1, lang);
        ps.setString(2, FALLBACK_LANG);
        ps.setString(3, lang);
        ps.setString(4, FALLBACK_LANG);
    }

    private TempRecord mapRow(ResultSet rs) throws SQLException {
        TempRecord r = new TempRecord();
        r.setRecordId(rs.getInt("record_id"));
        r.setInputValue(rs.getDouble("input_value"));
        r.setConvertedValue(rs.getDouble("converted_value"));
        r.setFromUnitId(rs.getInt("from_unit_id"));
        r.setToUnitId(rs.getInt("to_unit_id"));
        r.setLang(rs.getString("lang"));
        r.setFromUnitName(rs.getString("from_name"));
        r.setToUnitName(rs.getString("to_name"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            r.setCreatedAt(ts.toLocalDateTime());
        }
        return r;
    }
}