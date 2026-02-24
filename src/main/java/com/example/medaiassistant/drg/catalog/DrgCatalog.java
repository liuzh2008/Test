package com.example.medaiassistant.drg.catalog;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DRG目录不可变快照
 * 
 * 提供线程安全的并发读取能力，一旦构建就不可修改。
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-23
 */
public class DrgCatalog {
    private final String version;
    private final List<DrgParsedRecord> drgRecords;

    public DrgCatalog(String version, List<DrgParsedRecord> drgRecords) {
        this.version = version;
        this.drgRecords = Collections.unmodifiableList(drgRecords);
    }

    public String getVersion() {
        return version;
    }

    public List<DrgParsedRecord> getDrgRecords() {
        return drgRecords;
    }

    public int getRecordCount() {
        return drgRecords.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrgCatalog that = (DrgCatalog) o;
        return Objects.equals(version, that.version) && 
               Objects.equals(drgRecords, that.drgRecords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, drgRecords);
    }

    @Override
    public String toString() {
        return "DrgCatalog{" +
                "version='" + version + '\'' +
                ", recordCount=" + drgRecords.size() +
                '}';
    }
}
