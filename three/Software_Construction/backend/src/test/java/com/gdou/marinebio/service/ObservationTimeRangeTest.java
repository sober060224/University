package com.gdou.marinebio.service;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.repository.EcosystemRepository;
import com.gdou.marinebio.repository.ObservationRepository;
import com.gdou.marinebio.repository.ObservationSpeciesRepository;
import com.gdou.marinebio.repository.SpeciesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 观测检索的时间范围校验。
 *
 * <p>为什么这条值得一个测试：from &gt; to 时 SQL 的 {@code between} 恒假，接口会
 * 返回一个漂亮的分页结果（total=0、records=[]）。前端据此显示「暂无观测记录」，
 * 用户以为数据被删了，而真相是两个日期框填反了。静默的空结果是这类输入错误里
 * 最难自查的一种，所以要在服务层直接拒掉，并且用测试钉住「拒掉而非返回空」。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObservationTimeRangeTest {

    @Mock
    private ObservationRepository observationRepository;
    @Mock
    private ObservationSpeciesRepository observationSpeciesRepository;
    @Mock
    private SpeciesRepository speciesRepository;
    @Mock
    private EcosystemRepository ecosystemRepository;

    @InjectMocks
    private ObservationService observationService;

    private final Pageable pageable = Pageable.ofSize(10);

    @Test
    @DisplayName("开始时间晚于结束时间：明确报错，而不是返回一个空页")
    void fromAfterToIsRejected() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 2, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 1, 0, 0);

        assertThatThrownBy(() -> observationService.search(null, null, null, null,
                null, from, to, pageable))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("开始时间不能晚于结束时间");

        // 根本不该打到数据库：参数非法时连查询都不发
        verify(observationRepository, never()).search(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("起止时间相同是合法的（表示当天一整天），不能误拒")
    void equalBoundsAreAccepted() {
        LocalDateTime same = LocalDateTime.of(2026, 5, 1, 0, 0);
        lenient().when(observationRepository.search(any(), any(), any(), any(),
                        eq(same), eq(same), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        assertThatCode(() -> observationService.search(null, null, null, null,
                null, same, same, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("只给一端时间不触发校验（单边开区间是正常用法）")
    void singleSidedRangeIsFine() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        lenient().when(observationRepository.search(any(), any(), any(), any(),
                        any(), any(), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        assertThatCode(() -> observationService.search(null, null, null, null,
                null, from, null, pageable))
                .doesNotThrowAnyException();
        assertThatCode(() -> observationService.search(null, null, null, null,
                null, null, from, pageable))
                .doesNotThrowAnyException();
    }
}
