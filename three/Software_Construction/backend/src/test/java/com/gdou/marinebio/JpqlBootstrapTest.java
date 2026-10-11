package com.gdou.marinebio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 在没有数据库的情况下验证「所有 JPA 查询都能被 Hibernate 解析」。
 *
 * <p>为什么需要这一条：Spring Data 在装配仓储 Bean 时会解析每个 {@code @Query} 里的
 * JPQL——表名、实体属性名、函数、参数名任一拼错都会让上下文启动失败。这类错误
 * 编译期完全不报，SmokeTest 又必须连上真库才跑得起来，于是「本机没 MySQL 就无法验证
 * 查询写错了」成了一段真空区。而我这几轮新增了好几条 JPQL（门类下拉加了 isPublic 参数、
 * 物种与生态系统的反向实体链接），全都落在这段真空里。
 *
 * <p>办法是把元数据访问关掉、把方言钉死：
 * <ul>
 *   <li>{@code hibernate.boot.allow_jdbc_metadata_access=false} 让 Hibernate 不再试图
 *       从连接里读库信息；</li>
 *   <li>{@code spring.jpa.database-platform} 显式给出方言，替代它本来要问数据库拿的那个值；</li>
 *   <li>{@code ddl-auto=none} 跳过 schema 校验——校验本身要连库，这里不是被测对象。</li>
 * </ul>
 * 这样上下文可以完全不碰数据库就装配完成，而 JPQL 解析这一步照样发生。
 *
 * <p>注意这些开关<b>只写在这个测试的 properties 里</b>，没有动 application.yml：
 * 生产配置必须保持 {@code ddl-auto=validate}，否则表结构与实体对不上就没人报警了。
 *
 * <p>它验证的是"能否解析"，不是"结果是否正确"。真正的端到端仍需启动 MySQL 跑 SmokeTest。
 */
@SpringBootTest(properties = {
        // 指向一个不存在的端口：本测试的意义就在于「压根没连上也能装配成功」
        "spring.datasource.url=jdbc:mysql://127.0.0.1:1/no_such_db",
        "spring.datasource.username=nobody",
        "spring.datasource.password=nobody",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "ai.api-key=sk-请填写"
})
class JpqlBootstrapTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("数据库不可达时上下文仍能装配完成，说明每条 JPQL 都通过了 Hibernate 解析")
    void allQueriesParseWithoutADatabase() {
        // 走到这里就意味着所有 Repository Bean 都创建成功了——Spring Data 会在建 Bean 时
        // 解析 @Query 文本，任何一条写错都会让上面这个 @Autowired 直接抛异常、本测试红掉。
        assertThat(context.getBean("speciesRepository")).isNotNull();
        assertThat(context.getBean("ecosystemRepository")).isNotNull();
        assertThat(context.getBean("observationRepository")).isNotNull();
        assertThat(context.getBean("observationSpeciesRepository")).isNotNull();
        assertThat(context.getBean("aiRecordRepository")).isNotNull();
        assertThat(context.getBean("chatMessageRepository")).isNotNull();
        assertThat(context.getBean("operationLogRepository")).isNotNull();
        assertThat(context.getBean("userRepository")).isNotNull();

        // 数据源只是"配置好了"，并没有被连接过
        assertThat(dataSource).isNotNull();
        assertThatCode(() -> {
            try (var ignored = dataSource.getConnection()) {
                // 如果真的连上了，说明环境状态和这个测试的前提不符，应当报警而不是悄悄通过
                throw new AssertionError("意外的成功：数据库竟然可达，本测试的「无数据库」前提已不成立");
            } catch (java.sql.SQLException expected) {
                // 预期路径：连不上
            }
        }).doesNotThrowAnyException();
    }
}
