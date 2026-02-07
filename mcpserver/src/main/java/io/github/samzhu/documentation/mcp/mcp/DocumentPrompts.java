package io.github.samzhu.documentation.mcp.mcp;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP Prompts：文件搜尋與解釋提示範本
 * <p>
 * 提供引導 AI 搜尋文件並整理結果的提示範本。
 * </p>
 */
@Component
public class DocumentPrompts {

    /**
     * 搜尋文件提示範本
     * <p>
     * 引導 AI 搜尋文件並整理結果。
     * </p>
     */
    @McpPrompt(
            name = "search-docs",
            description = "搜尋技術文件並整理結果，提供結構化的搜尋摘要"
    )
    public GetPromptResult searchDocs(
            @McpArg(name = "query", description = "搜尋主題或問題", required = true) String query,
            @McpArg(name = "libraryName", description = "限定搜尋的文件庫名稱") String libraryName) {

        String libraryHint = (libraryName != null && !libraryName.isBlank())
                ? "在 %s 文件庫中".formatted(libraryName) : "在所有文件庫中";

        String prompt = """
                請%s搜尋以下主題的技術文件：「%s」

                請使用 search_documents 工具進行搜尋，然後：
                1. 整理搜尋結果，列出最相關的文件
                2. 摘要每份文件的重點內容
                3. 如有程式碼範例，請一併提供
                4. 標註文件來源（文件庫名稱、版本、路徑）
                """.formatted(libraryHint, query);

        return new GetPromptResult(
                "搜尋「%s」的相關技術文件".formatted(query),
                List.of(new PromptMessage(Role.USER, new TextContent(prompt)))
        );
    }

    /**
     * 根據文件解釋技術概念
     * <p>
     * 引導 AI 根據文件庫中的文件解釋特定技術概念。
     * </p>
     */
    @McpPrompt(
            name = "explain-with-docs",
            description = "根據文件庫中的文件解釋技術概念，提供有依據的技術說明"
    )
    public GetPromptResult explainWithDocs(
            @McpArg(name = "topic", description = "要解釋的技術主題", required = true) String topic,
            @McpArg(name = "libraryName", description = "參考的文件庫名稱", required = true) String libraryName) {

        String prompt = """
                請根據 %s 文件庫中的官方文件，解釋以下技術概念：「%s」

                請按以下步驟進行：
                1. 使用 search_documents 工具搜尋相關文件
                2. 如需要更詳細的內容，使用 get_document 工具取得完整文件
                3. 基於文件內容提供解釋，包含：
                   - 概念定義與用途
                   - 使用方式與配置說明
                   - 程式碼範例（來自文件）
                   - 注意事項與最佳實踐
                4. 標註所有引用的文件來源
                """.formatted(libraryName, topic);

        return new GetPromptResult(
                "根據 %s 文件解釋「%s」".formatted(libraryName, topic),
                List.of(new PromptMessage(Role.USER, new TextContent(prompt)))
        );
    }
}
