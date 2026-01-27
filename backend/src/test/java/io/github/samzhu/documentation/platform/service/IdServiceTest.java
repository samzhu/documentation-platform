package io.github.samzhu.documentation.platform.service;

import com.github.f4b6a3.tsid.TsidFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdService 單元測試
 * <p>
 * 測試 TSID 生成服務的格式、長度、唯一性等特性。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdService 單元測試")
class IdServiceTest {

    private IdService idService;

    @BeforeEach
    void setUp() {
        // 使用預設配置建立 TsidFactory
        TsidFactory tsidFactory = TsidFactory.builder().build();
        idService = new IdService(tsidFactory);
    }

    @Test
    @DisplayName("應生成 13 字元的 TSID")
    void shouldGenerate13CharacterId_whenCalled() {
        // When
        String id = idService.generateId();

        // Then
        assertThat(id).hasSize(13);
    }

    @Test
    @DisplayName("應生成 Crockford Base32 格式的 ID")
    void shouldGenerateCrockfordBase32Format_whenCalled() {
        // When
        String id = idService.generateId();

        // Then
        // Crockford Base32: 0-9, A-H, J-N, P-T, V-Z（不包含 I、L、O、U）
        assertThat(id).matches("^[0-9A-HJ-NP-TV-Z]{13}$");
    }

    @Test
    @DisplayName("應生成唯一的 ID（多次呼叫）")
    void shouldGenerateUniqueIds_whenCalledMultipleTimes() {
        // Given
        int iterations = 1000;
        Set<String> ids = new HashSet<>();

        // When
        for (int i = 0; i < iterations; i++) {
            ids.add(idService.generateId());
        }

        // Then
        assertThat(ids).hasSize(iterations);
    }

    @Test
    @DisplayName("應生成時間排序的 ID")
    void shouldGenerateTimeSortedIds_whenCalledSequentially() throws InterruptedException {
        // When
        String id1 = idService.generateId();
        Thread.sleep(5); // 確保時間戳不同
        String id2 = idService.generateId();

        // Then
        // TSID 具有時間排序特性，後生成的 ID 應該在字典序上較大
        assertThat(id2).isGreaterThan(id1);
    }

    @Test
    @DisplayName("應在高併發下生成唯一的 ID")
    void shouldGenerateUniqueIds_whenCalledConcurrently() throws InterruptedException {
        // Given
        int threadCount = 10;
        int iterationsPerThread = 100;
        Set<String> ids = new HashSet<>();
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    synchronized (ids) {
                        ids.add(idService.generateId());
                    }
                }
            });
            threads[i].start();
        }

        // 等待所有執行緒完成
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        assertThat(ids).hasSize(threadCount * iterationsPerThread);
    }

    @Test
    @DisplayName("應生成不包含特殊字元的 ID")
    void shouldGenerateIdWithoutSpecialCharacters_whenCalled() {
        // When
        String id = idService.generateId();

        // Then
        assertThat(id)
                .doesNotContain("I", "L", "O", "U") // Crockford Base32 排除的字元
                .doesNotContain("-", "_", " "); // 不應包含分隔符或空格
    }
}
