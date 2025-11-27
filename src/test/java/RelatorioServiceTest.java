import org.example.database.Conexao;
import org.example.dto.EquipamentoContagemFalhasDTO;
import org.example.dto.FalhaDetalhadaDTO;
import org.example.dto.RelatorioParadaDTO;
import org.example.model.Equipamento;
import org.example.service.relatorioservice.RelatorioService;
import org.example.service.relatorioservice.RelatorioServiceImpl;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RelatorioServiceTest {

    private static RelatorioService service;

    // ---------------------------
    //  SQL: CREATE TABLES
    // ---------------------------
    private static final String SQL_CREATE_EQUIP =
            """
            CREATE TABLE IF NOT EXISTS Equipamento (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nome VARCHAR(255) NOT NULL,
                numeroDeSerie VARCHAR(100) NOT NULL UNIQUE,
                areaSetor VARCHAR(100) NOT NULL,
                statusOperacional VARCHAR(50) NOT NULL,
            
                -- Garante que o status só possa ter valores pré-definidos
                CONSTRAINT chk_status_equipamento CHECK (statusOperacional IN ('OPERACIONAL', 'EM_MANUTENCAO', 'INATIVO'))
                    );
            """;

    private static final String SQL_CREATE_FALHA =
            """
            CREATE TABLE IF NOT EXISTS Falha (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                equipamentoId BIGINT NOT NULL,
                dataHoraOcorrencia DATETIME NOT NULL,
                descricao TEXT NOT NULL,
                criticidade VARCHAR(50) NOT NULL,
                status VARCHAR(50) NOT NULL,
                tempoParadaHoras DECIMAL(10,2),
            
                CONSTRAINT chk_criticidade_falha CHECK (
                    criticidade IN ('BAIXA','MEDIA','ALTA','CRITICA')
                ),
                CONSTRAINT chk_status_falha CHECK (
                    status IN ('ABERTA','EM_ANDAMENTO','RESOLVIDA')
                ),
                
                CONSTRAINT fk_falha_equip FOREIGN KEY (equipamentoId)
                REFERENCES Equipamento(id)
                ON DELETE RESTRICT
            );
            """;

    private static final String SQL_CREATE_ACAO =
            """
            CREATE TABLE IF NOT EXISTS AcaoCorretiva (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                falhaId BIGINT NOT NULL,
                dataHoraInicio DATETIME NOT NULL,
                dataHoraFim DATETIME NOT NULL,
                responsavel VARCHAR(255) NOT NULL,
                descricaoAcao TEXT NOT NULL,

                CONSTRAINT fk_acao_falha FOREIGN KEY (falhaId)
                REFERENCES Falha(id)
                ON DELETE RESTRICT
            );
            """;




    // ---------------------------
    //  SQL: DROP
    // ---------------------------
    private static final String SQL_DROP_ACAO = "DROP TABLE IF EXISTS AcaoCorretiva;";
    private static final String SQL_DROP_FALHA = "DROP TABLE IF EXISTS Falha;";
    private static final String SQL_DROP_EQUIP = "DROP TABLE IF EXISTS Equipamento;";

    // ---------------------------
    //  SQL TRUNCATE
    // ---------------------------
    private static final String SQL_TRUNCATE_ACAO = "TRUNCATE TABLE AcaoCorretiva;";
    private static final String SQL_TRUNCATE_FALHA = "TRUNCATE TABLE Falha;";
    private static final String SQL_TRUNCATE_EQUIP = "TRUNCATE TABLE Equipamento;";

    // ---------------------------
    //  SETUP DO BANCO
    // ---------------------------
    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            // Drop
            stmt.execute(SQL_DROP_ACAO);
            stmt.execute(SQL_DROP_FALHA);
            stmt.execute(SQL_DROP_EQUIP);

            // Create
            stmt.execute(SQL_CREATE_EQUIP);
            stmt.execute(SQL_CREATE_FALHA);
            stmt.execute(SQL_CREATE_ACAO);
        }

        service = new RelatorioServiceImpl();
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_DROP_ACAO);
            stmt.execute(SQL_DROP_FALHA);
            stmt.execute(SQL_DROP_EQUIP);
        }
    }

    @BeforeEach
    void resetTables() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute(SQL_TRUNCATE_ACAO);
            stmt.execute(SQL_TRUNCATE_FALHA);
            stmt.execute(SQL_TRUNCATE_EQUIP);

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        // Inserir dados a cada teste
        TestUtils.inserirEquipamentosFalhasEAcoes();
    }

    // ------------------------------ TESTE 1 ------------------------------
    @Test
    @Order(1)
    void deveGerarRelatorioTempoParada() throws SQLException {
        List<RelatorioParadaDTO> lista = service.gerarRelatorioTempoParada();

        assertNotNull(lista);
        assertEquals(3, lista.size(), "Devem existir dois equipamentos com falhas");

        RelatorioParadaDTO motor = lista.stream()
                .filter(x -> x.getNomeEquipamento().equals("Motor Principal"))
                .findFirst()
                .orElseThrow();

        assertEquals(1.5, motor.getTotalHorasParadas());

    }


    // ------------------------------ TESTE 2 ------------------------------
    @Test
    @Order(2)
    void deveBuscarEquipamentosSemFalhas() throws SQLException{
        LocalDate inicio = LocalDate.now().minusDays(10);
        LocalDate fim = LocalDate.now().plusDays(10);

        List<Equipamento> lista = service.buscarEquipamentosSemFalhasPorPeriodo(inicio, fim);

        assertNotNull(lista);
        assertTrue(lista.size() >= 0);
    }

    // ------------------------------ TESTE 3 ------------------------------
    @Test
    @Order(3)
    void deveBuscarDetalhesCompletosDaFalha() throws SQLException{
        long falhaExistente = 1;

        Optional<FalhaDetalhadaDTO> detalhes = service.buscarDetalhesCompletosFalha(falhaExistente);

        assertTrue(detalhes.isPresent());
        assertNotNull(detalhes.get().getFalha());
        assertNotNull(detalhes.get().getEquipamento());
    }

    // ------------------------------ TESTE 4 ------------------------------
    @Test
    @Order(4)
    void deveLancarErroSeIdFalhaForInvalido() {
        assertThrows(RuntimeException.class, () -> service.buscarDetalhesCompletosFalha(0));
        assertThrows(RuntimeException.class, () -> service.buscarDetalhesCompletosFalha(-10));
    }

    // ------------------------------ TESTE 5 ------------------------------
    @Test
    @Order(5)
    void deveGerarRelatorioManutencaoPreventiva() throws SQLException{
        List<EquipamentoContagemFalhasDTO> lista =
                service.gerarRelatorioManutencaoPreventiva(1);

        assertNotNull(lista);
        assertTrue(lista.size() > 0);
        assertTrue(lista.get(0).getTotalFalhas() >= 1);
    }

    // ------------------------------ TESTE 6 ------------------------------
    @Test
    @Order(6)
    void deveLancarErroSeContagemMinimaInvalida() {
        assertThrows(RuntimeException.class, () -> service.gerarRelatorioManutencaoPreventiva(0));
        assertThrows(RuntimeException.class, () -> service.gerarRelatorioManutencaoPreventiva(-5));
    }

}
