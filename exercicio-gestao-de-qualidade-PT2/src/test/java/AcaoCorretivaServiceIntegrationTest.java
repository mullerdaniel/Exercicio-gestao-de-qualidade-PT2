import org.example.database.Conexao;
import org.example.model.AcaoCorretiva;
import org.example.service.acaocorretiva.AcaoCorretivaService;
import org.example.service.acaocorretiva.AcaoCorretivaServiceImpl;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Teste de Integração - AcaoCorretivaService (Banco Real)")
public class AcaoCorretivaServiceIntegrationTest {
    private AcaoCorretivaService acaoService;

    // ---------------------------
    //  SQL DAS TABELAS
    // ---------------------------
    private static final String SQL_CREATE_EQUIP =
            """
            CREATE TABLE IF NOT EXISTS Equipamento (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nome VARCHAR(255) NOT NULL,
                numeroDeSerie VARCHAR(100) NOT NULL UNIQUE,
                areaSetor VARCHAR(100) NOT NULL,
                statusOperacional VARCHAR(50) NOT NULL,
                CONSTRAINT chk_status_equip CHECK (
                    statusOperacional IN ('OPERACIONAL','EM_MANUTENCAO','INATIVO')
                )
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

    private static final String SQL_DROP1 =
            """
            DROP TABLE IF EXISTS AcaoCorretiva;
            """;

    private static final String SQL_DROP2 =
            """
            DROP TABLE IF EXISTS Falha;
            """;

    private static final String SQL_DROP3 =
            """
            DROP TABLE IF EXISTS Equipamento;
            """;

    private static final String SQL_TRUNCATE1 = "TRUNCATE TABLE AcaoCorretiva;";

    private static final String SQL_TRUNCATE2 = "TRUNCATE TABLE Falha;";

    private static final String SQL_TRUNCATE3 = "TRUNCATE TABLE Equipamento;";

    // ---------------------------
    //  CICLO DO BANCO
    // ---------------------------
    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_DROP1);
            stmt.execute(SQL_DROP2);
            stmt.execute(SQL_DROP3);
            stmt.execute(SQL_CREATE_EQUIP);
            stmt.execute(SQL_CREATE_FALHA);
            stmt.execute(SQL_CREATE_ACAO);
        }
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_DROP1);
            stmt.execute(SQL_DROP2);
            stmt.execute(SQL_DROP3);
        }
    }

    @BeforeEach
    void setupEach() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute(SQL_TRUNCATE1); // AcaoCorretiva
            stmt.execute(SQL_TRUNCATE2); // Falha
            stmt.execute(SQL_TRUNCATE3); // Equipamento

            // Reabilita FK
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        acaoService = new AcaoCorretivaServiceImpl();
    }


    // -------------------------------------------------
    //  TESTE 1 - Falha inexistente → Exception
    // -------------------------------------------------
    @Test
    @DisplayName("Deve lançar exceção ao registrar ação corretiva de falha inexistente")
    void deveLancarExceptionQuandoFalhaNaoExiste() {

        AcaoCorretiva acao = new AcaoCorretiva(
                999L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Técnico A",
                "Ajuste geral"
        );

        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> acaoService.registrarConclusaoDeAcao(acao)
        );

        assertEquals("Falha não encontrada!", e.getMessage());
    }


    // -------------------------------------------------
    // TESTE 2 - Registrar ação e resolver falha
    // -------------------------------------------------
    @Test
    @DisplayName("Deve registrar ação corretiva e atualizar falha para RESOLVIDA")
    void deveRegistrarAcaoECorrigirFalha() throws Exception {

        // 1. Criar equipamento
        Long equipId;
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Equipamento (nome,numeroDeSerie,areaSetor,statusOperacional) VALUES ('E1','S1','A1','OPERACIONAL')",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            equipId = rs.getLong(1);
        }

        // 2. Criar falha
        Long falhaId;
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     INSERT INTO Falha (equipamentoId,dataHoraOcorrencia,descricao,criticidade,status,tempoParadaHoras)
                     VALUES (?, NOW(), 'Falha média', 'MEDIA', 'ABERTA', 1.0)
                     """,
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, equipId);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            falhaId = rs.getLong(1);
        }

        // 3. Registrar ação corretiva
        AcaoCorretiva acao = new AcaoCorretiva(
                falhaId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Tecnico X",
                "Troca de peça"
        );

        AcaoCorretiva salvo = acaoService.registrarConclusaoDeAcao(acao);

        assertNotNull(salvo.getId());

        // 4. Verificar falha → deve estar RESOLVIDA
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT status FROM Falha WHERE id = ?")) {

            stmt.setLong(1, falhaId);
            ResultSet rs = stmt.executeQuery();
            rs.next();

            assertEquals("RESOLVIDA", rs.getString("status"));
        }
    }


    // -------------------------------------------------
    // TESTE 3 - Se falha CRÍTICA → equipamento volta para OPERACIONAL
    // -------------------------------------------------
    @Test
    @DisplayName("Deve atualizar equipamento para OPERACIONAL quando falha crítica é resolvida")
    void deveAtualizarEquipamentoQuandoFalhaCriticaResolvida() throws Exception {

        // 1. Criar equipamento (colocamos ele em EM_MANUTENCAO)
        Long equipId;
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Equipamento (nome,numeroDeSerie,areaSetor,statusOperacional) VALUES ('E2','S2','A2','EM_MANUTENCAO')",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            equipId = rs.getLong(1);
        }

        // 2. Criar falha CRÍTICA ABERTA
        Long falhaId;
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     INSERT INTO Falha (equipamentoId,dataHoraOcorrencia,descricao,
                                        criticidade,status,tempoParadaHoras)
                     VALUES (?, NOW(), 'Falha crítica geral', 'CRITICA', 'ABERTA', 3.0)
                     """,
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, equipId);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            falhaId = rs.getLong(1);
        }

        // 3. Registrar a ação corretiva
        AcaoCorretiva acao = new AcaoCorretiva(
                falhaId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Técnico Z",
                "Reparo crítico"
        );

        acaoService.registrarConclusaoDeAcao(acao);

        // 4. Validar: Falha → RESOLVIDA
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT status FROM Falha WHERE id = ?")) {

            stmt.setLong(1, falhaId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            assertEquals("RESOLVIDA", rs.getString("status"));
        }

        // 5. Validar: Equipamento → OPERACIONAL
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT statusOperacional FROM Equipamento WHERE id = ?")) {

            stmt.setLong(1, equipId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            assertEquals("OPERACIONAL", rs.getString("statusOperacional"));
        }
    }
}
