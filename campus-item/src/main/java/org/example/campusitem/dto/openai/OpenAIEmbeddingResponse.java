package org.example.campusitem.dto.openai;

import lombok.Data;
import java.util.List;

@Data
public class OpenAIEmbeddingResponse {
    private List<EmbeddingData> data;
    private Usage usage;

    @Data
    public static class EmbeddingData {
        private List<Double> embedding; // 1536维的向量数据
        private Integer index;
    }

    @Data
    public static class Usage {
        private Integer prompt_tokens;
        private Integer total_tokens;
    }
}
