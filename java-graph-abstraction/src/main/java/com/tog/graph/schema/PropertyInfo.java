package com.tog.graph.schema;

import java.util.*;

/**
 * 属性信息
 */
public class PropertyInfo {
    
    private final String name;
    private final long frequency; // 有多少个节点/关系包含这个属性
    private final Map<String, Long> valueTypes = new HashMap<>(); // 值类型分布
    private List<String> sampleValues = new ArrayList<>(); // 样本值
    
    public PropertyInfo(String name, long frequency) {
        this.name = name;
        this.frequency = frequency;
    }
    
    public String getName() { return name; }
    public long getFrequency() { return frequency; }
    
    public Map<String, Long> getValueTypes() { return valueTypes; }
    
    public void addValueType(String type, long count) {
        valueTypes.put(type, count);
    }
    
    /**
     * 获取主要的值类型
     */
    public String getPrimaryType() {
        return valueTypes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("STRING");
    }
    
    public List<String> getSampleValues() { return sampleValues; }
    public void setSampleValues(List<String> sampleValues) { this.sampleValues = sampleValues; }
    
    /**
     * 检查是否适合用于搜索
     */
    public boolean isSearchable() {
        String primaryType = getPrimaryType();
        return "STRING".equals(primaryType) && frequency > 0;
    }
    
    /**
     * 检查是否适合用于过滤
     */
    public boolean isFilterable() {
        String primaryType = getPrimaryType();
        return Arrays.asList("STRING", "INTEGER", "FLOAT", "BOOLEAN").contains(primaryType);
    }
    
    /**
     * 获取属性的选择性（唯一值的估计比例）
     */
    public double getSelectivity() {
        if (sampleValues.isEmpty()) return 0.5; // 默认估计
        
        Set<String> uniqueValues = new HashSet<>(sampleValues);
        return (double) uniqueValues.size() / sampleValues.size();
    }
    
    @Override
    public String toString() {
        return String.format("Property{name='%s', frequency=%d, type='%s', samples=%d}", 
                           name, frequency, getPrimaryType(), sampleValues.size());
    }
}