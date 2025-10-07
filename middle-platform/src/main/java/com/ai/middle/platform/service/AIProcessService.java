package com.ai.middle.platform.service;

/**
 * AI处理服务接口
 */
public interface AIProcessService {

    /**
     * OCR文字识别
     *
     * @param filePath 文件路径
     * @param fileType 文件类型
     * @return OCR识别内容
     */
    String performOcr(String filePath, String fileType);

    /**
     * 文本向量化
     *
     * @param fileId    文件ID
     * @param content   OCR内容
     * @param chunkSize 分块大小
     * @param overlap   分块重叠
     */
    void performVectorization(String fileId, String content, Integer chunkSize, Integer overlap);

    /**
     * 生成问答对
     *
     * @param fileId   文件ID
     * @param content  OCR内容
     * @param maxPairs 最大问答对数量
     */
    void generateQaPairs(String fileId, String content, Integer maxPairs);

    /**
     * 生成知识图谱
     *
     * @param fileId  文件ID
     * @param content OCR内容
     */
    void generateKnowledgeGraph(String fileId, String content);

    /**
     * 生成文件标签
     *
     * @param fileId   文件ID
     * @param fileName 文件名称
     * @param content  文件内容（会截取前500字）
     * @return 标签列表（约5个）
     */
    java.util.List<String> generateTags(String fileId, String fileName, String content);
}
