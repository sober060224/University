package com.gdou.marinebio.entity;

/**
 * 智能服务调用类型，对应模块五的五项能力。
 * 同时作为 ai_records.type 的取值，落库便于后续统计各功能的调用量与成功率。
 */
public enum AiType {
    /** 图像智能识别与物种鉴定 */
    IDENTIFY,
    /** 文本辅助分类与补全 */
    COMPLETE,
    /** 物种描述多语言翻译 */
    TRANSLATE,
    /** 观测记录智能标签与异常检测 */
    TAG,
    /** 智能问答与科研助手 */
    QA
}
