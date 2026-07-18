package valkyrie.driver.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步选项
 *
 * @author Luo Tiansheng
 * @since 2026/7/19
 */
public class SyncOptions
{
        public enum CaseStrategy { LOWERCASE, UPPERCASE, KEEP }

        public enum ExistingDataPolicy { TRUNCATE, SKIP, FAIL }

        private CaseStrategy caseStrategy = CaseStrategy.LOWERCASE;
        private boolean includeStructure = true;
        private boolean includeData = false;
        private ExistingDataPolicy existingDataPolicy = ExistingDataPolicy.FAIL;
        private int pageSize = 5000;
        private int batchSize = 1000;
        private Map<String, String> typeOverrides = new HashMap<>();

        public CaseStrategy getCaseStrategy() { return caseStrategy; }
        public void setCaseStrategy(CaseStrategy caseStrategy) { this.caseStrategy = caseStrategy; }

        public boolean isIncludeStructure() { return includeStructure; }
        public void setIncludeStructure(boolean includeStructure) { this.includeStructure = includeStructure; }

        public boolean isIncludeData() { return includeData; }
        public void setIncludeData(boolean includeData) { this.includeData = includeData; }

        public ExistingDataPolicy getExistingDataPolicy() { return existingDataPolicy; }
        public void setExistingDataPolicy(ExistingDataPolicy existingDataPolicy) { this.existingDataPolicy = existingDataPolicy; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public Map<String, String> getTypeOverrides() { return typeOverrides; }
        public void setTypeOverrides(Map<String, String> typeOverrides) { this.typeOverrides = typeOverrides; }

        public String normalizeCase(String identifier)
        {
                if (identifier == null) return null;
                return switch (caseStrategy) {
                        case LOWERCASE -> identifier.toLowerCase();
                        case UPPERCASE -> identifier.toUpperCase();
                        case KEEP -> identifier;
                };
        }
}
