//package andryukha
//
//import andryukha.persistence.VisitRepository
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Tag
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.context.TestConfiguration
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
//import org.springframework.context.ApplicationContextInitializer
//import org.springframework.context.ConfigurableApplicationContext
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Import
//import org.springframework.context.annotation.Primary
//import org.springframework.http.MediaType
//import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.context.ContextConfiguration
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
//import java.time.Clock
//import java.time.Instant
//import java.time.ZoneOffset
//
///** Runs only against the disposable test-postgres service from docker/docker-compose-local.yml. */
//@Tag("integration")
//@SpringBootTest(properties = [
//    "spring.datasource.url=\${TEST_DB_URL}",
//    "spring.datasource.username=\${TEST_DB_USER}",
//    "spring.datasource.password=\${TEST_DB_PASSWORD}",
//    "spring.security.user.name=test", "spring.security.user.password=test-password",
//    "management.server.port=0",
//])
//@ActiveProfiles("local")
//@AutoConfigureMockMvc
//@ContextConfiguration(initializers = [TestDatabaseGuard::class])
//@Import(FixedClockConfig::class)
//class VisitApiIntegrationTest {
//    @Autowired lateinit var mvc: MockMvc
//    @Autowired lateinit var repository: VisitRepository
//
//    @BeforeEach fun clearTestDatabase() { repository.deleteAll() }
//
//    private fun row(id: String = "v1", date: String = "2026-02-01", km: Int = 100000) =
//        """{"externalId":"$id","customerId":"c1","vehicleId":"car1","visitedOn":"$date","odometerKm":$km,"services":["MAINTENANCE"]}"""
//    private fun import(vararg rows: String) = mvc.perform(post("/api/v1/imports/visits")
//        .with(httpBasic("test", "test-password")).contentType(MediaType.APPLICATION_JSON)
//        .content("""{"visits":[${rows.joinToString(",") }]}"""))
//
//    @Test fun `repeated import updates instead of creating duplicates`() {
//        import(row()).andExpect(status().isOk).andExpect(jsonPath("$.inserted").value(1))
//        import(row()).andExpect(status().isOk).andExpect(jsonPath("$.unchanged").value(1))
//        import(row(km = 110000)).andExpect(status().isOk).andExpect(jsonPath("$.updated").value(1))
//        assertThat(repository.count()).isEqualTo(1)
//        assertThat(repository.findAll().single().odometerKm).isEqualTo(110000)
//    }
//    @Test fun `invalid batch is rejected atomically`() {
//        import(row(), row(id = "v2", date = "2030-01-01")).andExpect(status().isBadRequest)
//        assertThat(repository.count()).isZero()
//    }
//    @Test fun `duplicate identifiers inside batch are rejected`() {
//        import(row(), row()).andExpect(status().isBadRequest)
//        assertThat(repository.count()).isZero()
//    }
//    @Test fun `bean validation rejects invalid odometer and blank ids`() {
//        import(row(km = -1)).andExpect(status().isBadRequest)
//        import(row(id = " ")).andExpect(status().isBadRequest)
//        assertThat(repository.count()).isZero()
//    }
//    @Test fun `API requires authentication`() {
//        mvc.perform(get("/api/v1/visits")).andExpect(status().isUnauthorized)
//    }
//    @Test fun `Telegram status is disabled until configured`() {
//        mvc.perform(get("/api/v1/integrations/telegram/status").with(httpBasic("test", "test-password")))
//            .andExpect(status().isOk).andExpect(jsonPath("$.enabled").value(false))
//        mvc.perform(post("/api/v1/integrations/telegram/test-message").with(httpBasic("test", "test-password"))
//            .contentType(MediaType.APPLICATION_JSON).content("""{"chatId":1,"text":"test"}"""))
//            .andExpect(status().isServiceUnavailable)
//    }
//    @Test fun `imported data produces a recommendation through HTTP`() {
//        import(row(), row("v2", "2026-05-02", 110000), row("v3", "2026-07-31", 120000))
//            .andExpect(status().isOk)
//        mvc.perform(get("/api/v1/recommendations?asOf=2026-10-29").with(httpBasic("test", "test-password")))
//            .andExpect(status().isOk).andExpect(jsonPath("$[0].status").value("READY"))
//            .andExpect(jsonPath("$[0].dueOn").value("2026-10-29"))
//    }
//    @Test fun `cancelling a source visit removes its contribution`() {
//        import(row(), row("v2", "2026-05-02", 110000), row("v3", "2026-07-31", 120000))
//            .andExpect(status().isOk)
//        import(row("v3", "2026-07-31", 120000).dropLast(1) + ",\"status\":\"CANCELLED\"}")
//            .andExpect(status().isOk)
//        mvc.perform(get("/api/v1/recommendations").with(httpBasic("test", "test-password")))
//            .andExpect(status().isOk).andExpect(jsonPath("$[0].status").value("INSUFFICIENT_HISTORY"))
//    }
//    @Test fun `pagination and future preview are validated`() {
//        mvc.perform(get("/api/v1/visits?size=1000").with(httpBasic("test", "test-password")))
//            .andExpect(status().isBadRequest)
//        mvc.perform(get("/api/v1/recommendations?asOf=2030-01-01").with(httpBasic("test", "test-password")))
//            .andExpect(status().isBadRequest)
//    }
//    @Test fun `unknown input fields fail instead of silently losing data`() {
//        import(row().dropLast(1) + ",\"mileageTypo\":500}").andExpect(status().isBadRequest)
//    }
//}
//
//class TestDatabaseGuard : ApplicationContextInitializer<ConfigurableApplicationContext> {
//    override fun initialize(context: ConfigurableApplicationContext) {
//        require(context.environment.getRequiredProperty("spring.datasource.url").endsWith("/andryukha_test")) {
//            "Integration tests may only use the dedicated andryukha_test database."
//        }
//    }
//}
//
//@TestConfiguration
//class FixedClockConfig {
//    @Bean @Primary fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC)
//}
