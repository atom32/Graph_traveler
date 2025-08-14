package com.tog.graph.demo;

import com.tog.graph.config.GraphConfig;
import com.tog.graph.core.GraphDatabase;
import com.tog.graph.neo4j.Neo4jGraphDatabase;
import com.tog.graph.schema.GraphSchemaAnalyzer;
import com.tog.graph.schema.GraphSchema;
import com.tog.graph.schema.SearchStrategy;
import com.tog.graph.schema.NodeTypeInfo;
import com.tog.graph.schema.RelationshipTypeInfo;

import java.util.Scanner;

/**
 * Schema分析演示程序
 * 展示在搜索之前如何分析数据库结构
 */
public class SchemaAnalysisDemo {
    
    private final GraphConfig config;
    private final GraphDatabase database;
    private final GraphSchemaAnalyzer schemaAnalyzer;
    
    public SchemaAnalysisDemo() {
        this.config = new GraphConfig();
        this.database = new Neo4jGraphDatabase();
        this.schemaAnalyzer = new GraphSchemaAnalyzer(database);
    }
    
    public static void main(String[] args) {
        SchemaAnalysisDemo demo = new SchemaAnalysisDemo();
        demo.run();
    }
    
    public void run() {
        System.out.println("🔍 图数据库Schema分析演示");
        System.out.println("=====================================");
        
        try {
            // 连接数据库
            ((Neo4jGraphDatabase) database).connect(config.getUri(), config.getUsername(), config.getPassword());
            System.out.println("✅ 数据库连接成功");
            
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                System.out.println("\n请选择操作:");
                System.out.println("1. 分析数据库Schema");
                System.out.println("2. 查看Schema摘要");
                System.out.println("3. 分析搜索策略");
                System.out.println("4. 查看节点类型详情");
                System.out.println("5. 查看关系类型详情");
                System.out.println("6. 退出");
                System.out.print("请输入选项 (1-6): ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        analyzeSchema();
                        break;
                    case "2":
                        showSchemaSummary();
                        break;
                    case "3":
                        analyzeSearchStrategy(scanner);
                        break;
                    case "4":
                        showNodeTypeDetails(scanner);
                        break;
                    case "5":
                        showRelationshipTypeDetails(scanner);
                        break;
                    case "6":
                        System.out.println("👋 再见！");
                        return;
                    default:
                        System.out.println("❌ 无效选项，请重新选择");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 程序运行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                database.close();
                System.out.println("✅ 数据库连接已关闭");
            } catch (Exception e) {
                System.err.println("❌ 关闭数据库连接时出错: " + e.getMessage());
            }
        }
    }
    
    private void analyzeSchema() {
        System.out.println("🔄 正在分析数据库Schema...");
        
        try {
            long startTime = System.currentTimeMillis();
            GraphSchema schema = schemaAnalyzer.analyzeSchema();
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Schema分析完成！");
            System.out.println("⏱️ 分析耗时: " + (endTime - startTime) + "ms");
            System.out.println();
            System.out.println(schema.getSummary());
            
            // 显示索引建议
            if (!schema.getIndexSuggestions().isEmpty()) {
                System.out.println("\n💡 索引建议:");
                for (String suggestion : schema.getIndexSuggestions()) {
                    System.out.println("  " + suggestion);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Schema分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showSchemaSummary() {
        try {
            GraphSchema schema = schemaAnalyzer.analyzeSchema();
            System.out.println("\n📊 Schema摘要:");
            System.out.println(schema.getSummary());
            
        } catch (Exception e) {
            System.err.println("❌ 获取Schema摘要失败: " + e.getMessage());
        }
    }
    
    private void analyzeSearchStrategy(Scanner scanner) {
        System.out.print("请输入要分析的搜索问题: ");
        String question = scanner.nextLine().trim();
        
        if (question.isEmpty()) {
            System.out.println("❌ 问题不能为空");
            return;
        }
        
        try {
            System.out.println("🤔 分析搜索策略: " + question);
            
            GraphSchema schema = schemaAnalyzer.analyzeSchema();
            SearchStrategy strategy = schemaAnalyzer.recommendSearchStrategy(question, schema);
            
            System.out.println("\n📋 推荐的搜索策略:");
            System.out.println("置信度: " + String.format("%.3f", strategy.getConfidenceScore()));
            
            if (!strategy.getRelevantNodeTypes().isEmpty()) {
                System.out.println("\n🎯 相关节点类型:");
                strategy.getRelevantNodeTypes().forEach((nodeType, relevance) -> {
                    System.out.println("  - " + nodeType + " (相关度: " + String.format("%.3f", relevance) + ")");
                });
            }
            
            if (!strategy.getRelevantRelationshipTypes().isEmpty()) {
                System.out.println("\n🔗 相关关系类型:");
                strategy.getRelevantRelationshipTypes().forEach((relType, relevance) -> {
                    System.out.println("  - " + relType + " (相关度: " + String.format("%.3f", relevance) + ")");
                });
            }
            
            if (!strategy.getSearchProperties().isEmpty()) {
                System.out.println("\n🔍 推荐搜索属性:");
                strategy.getSearchProperties().forEach((nodeType, properties) -> {
                    System.out.println("  " + nodeType + ":");
                    properties.forEach((property, relevance) -> {
                        System.out.println("    - " + property + " (相关度: " + String.format("%.3f", relevance) + ")");
                    });
                });
            }
            
            // 显示生成的Cypher查询
            System.out.println("\n💻 生成的Cypher查询:");
            for (String query : strategy.generateCypherQueries(question)) {
                System.out.println("  " + query);
            }
            
            if (!strategy.isEffective()) {
                System.out.println("\n⚠️ 注意: 当前策略可能不够有效，建议使用通用搜索");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 搜索策略分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showNodeTypeDetails(Scanner scanner) {
        try {
            GraphSchema schema = schemaAnalyzer.analyzeSchema();
            
            System.out.println("\n📋 可用的节点类型:");
            int index = 1;
            for (NodeTypeInfo nodeType : schema.getNodeTypes()) {
                System.out.println("  " + index + ". " + nodeType.getLabel() + " (" + nodeType.getCount() + " 个节点)");
                index++;
            }
            
            System.out.print("请选择要查看的节点类型编号: ");
            String input = scanner.nextLine().trim();
            
            try {
                int choice = Integer.parseInt(input);
                NodeTypeInfo[] nodeTypes = schema.getNodeTypes().toArray(new NodeTypeInfo[0]);
                
                if (choice >= 1 && choice <= nodeTypes.length) {
                    NodeTypeInfo selectedType = nodeTypes[choice - 1];
                    showNodeTypeDetail(selectedType);
                } else {
                    System.out.println("❌ 无效的选择");
                }
                
            } catch (NumberFormatException e) {
                System.out.println("❌ 请输入有效的数字");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 获取节点类型详情失败: " + e.getMessage());
        }
    }
    
    private void showNodeTypeDetail(NodeTypeInfo nodeType) {
        System.out.println("\n🏷️ 节点类型详情: " + nodeType.getLabel());
        System.out.println("节点数量: " + nodeType.getCount());
        System.out.println("属性数量: " + nodeType.getProperties().size());
        
        if (!nodeType.getProperties().isEmpty()) {
            System.out.println("\n📝 属性列表:");
            for (var property : nodeType.getMostCommonProperties(10)) {
                System.out.println("  - " + property.getName() + 
                                 " (频率: " + property.getFrequency() + 
                                 ", 类型: " + property.getPrimaryType() + ")");
                
                if (!property.getSampleValues().isEmpty()) {
                    System.out.println("    样本值: " + property.getSampleValues().subList(0, 
                                     Math.min(3, property.getSampleValues().size())));
                }
            }
        }
        
        System.out.println("\n🔍 可搜索属性:");
        for (var property : nodeType.getSearchableProperties()) {
            System.out.println("  - " + property.getName() + " (选择性: " + 
                             String.format("%.3f", property.getSelectivity()) + ")");
        }
    }
    
    private void showRelationshipTypeDetails(Scanner scanner) {
        try {
            GraphSchema schema = schemaAnalyzer.analyzeSchema();
            
            System.out.println("\n📋 可用的关系类型:");
            int index = 1;
            for (RelationshipTypeInfo relType : schema.getRelationshipTypes()) {
                System.out.println("  " + index + ". " + relType.getType() + " (" + relType.getTotalCount() + " 个关系)");
                index++;
            }
            
            System.out.print("请选择要查看的关系类型编号: ");
            String input = scanner.nextLine().trim();
            
            try {
                int choice = Integer.parseInt(input);
                RelationshipTypeInfo[] relTypes = schema.getRelationshipTypes().toArray(new RelationshipTypeInfo[0]);
                
                if (choice >= 1 && choice <= relTypes.length) {
                    RelationshipTypeInfo selectedType = relTypes[choice - 1];
                    showRelationshipTypeDetail(selectedType);
                } else {
                    System.out.println("❌ 无效的选择");
                }
                
            } catch (NumberFormatException e) {
                System.out.println("❌ 请输入有效的数字");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 获取关系类型详情失败: " + e.getMessage());
        }
    }
    
    private void showRelationshipTypeDetail(RelationshipTypeInfo relType) {
        System.out.println("\n🔗 关系类型详情: " + relType.getType());
        System.out.println("关系总数: " + relType.getTotalCount());
        System.out.println("连接模式数: " + relType.getPatterns().size());
        
        if (!relType.getPatterns().isEmpty()) {
            System.out.println("\n🎯 连接模式:");
            for (var pattern : relType.getMostCommonPatterns(5)) {
                System.out.println("  - (" + pattern.getSourceLabel() + ")-[:" + relType.getType() + 
                                 "]->(" + pattern.getTargetLabel() + ") [" + pattern.getCount() + " 个]");
            }
        }
        
        if (!relType.getProperties().isEmpty()) {
            System.out.println("\n📝 关系属性:");
            for (var property : relType.getProperties()) {
                System.out.println("  - " + property.getName() + 
                                 " (频率: " + property.getFrequency() + 
                                 ", 类型: " + property.getPrimaryType() + ")");
            }
        }
    }
}