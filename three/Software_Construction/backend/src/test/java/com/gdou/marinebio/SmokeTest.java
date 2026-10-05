package com.gdou.marinebio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端冒烟测试。
 *
 * <p>按最小可运行检查的原则，这里只保留一条链路：管理员登录 → 新增物种 → 新建观测记录（关联该物种）
 * → 读取综合数据看板。中间任意一步的数据都会在方法末尾删除，不污染演示用的种子数据。
 *
 * <p>前置条件：本地 MySQL 已启动，且已导入 database.sql。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("登录 → 新增物种 → 新建观测记录 → 数据看板")
    void mainFlowWorks() throws Exception {
        // 1. 登录，取得会话
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 2. 新增物种
        String speciesName = "冒烟测试物种";
        String speciesBody = objectMapper.writeValueAsString(Map.of(
                "chineseName", speciesName,
                "scientificName", "Smokeus testus",
                "phylum", "脊索动物门",
                "className", "辐鳍鱼纲",
                "protectionLevel", "一般保护",
                "isPublic", true));
        MvcResult created = mockMvc.perform(post("/api/species")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(speciesBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        int speciesId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asInt();
        assertThat(speciesId).isGreaterThan(0);

        // 3. 新建观测记录，并关联刚创建的物种
        String observationBody = objectMapper.writeValueAsString(Map.of(
                "ecosystemId", 1,
                "observeTime", "2026-05-01T09:30:00",
                "locationName", "麻章珍珠湾",
                "longitude", 110.42,
                "latitude", 21.15,
                "waterTemp", 26.5,
                "salinity", 30.2,
                "species", java.util.List.of(Map.of("speciesId", speciesId, "count", 5, "behavior", "索饵"))));
        MvcResult observation = mockMvc.perform(post("/api/observations")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(observationBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        int observationId = objectMapper.readTree(observation.getResponse().getContentAsString())
                .path("data").path("id").asInt();
        assertThat(observationId).isGreaterThan(0);

        // 4. 综合数据看板：统计口径应包含刚写入的两条记录
        mockMvc.perform(get("/api/stats/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overview.speciesTotal").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.data.overview.observationTotal").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(28)));

        // 5. 清理，避免污染演示数据
        mockMvc.perform(delete("/api/observations/" + observationId).session(session).with(csrf()))
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(delete("/api/species/" + speciesId).session(session).with(csrf()))
                .andExpect(jsonPath("$.success").value(true));

        JsonNode tail = objectMapper.readTree(mockMvc.perform(get("/api/species?keyword=" + speciesName).session(session))
                .andReturn().getResponse().getContentAsString());
        assertThat(tail.path("data").path("total").asInt()).isZero();
    }
}
