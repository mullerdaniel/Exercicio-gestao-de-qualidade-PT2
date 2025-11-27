import org.example.database.Conexao;
import org.example.model.Falha;
import org.example.service.equipamento.EquipamentoService;
import org.example.service.equipamento.EquipamentoServiceImpl;
import org.example.service.falha.FalhaService;
import org.example.service.falha.FalhaServiceImpl;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Teste de Integração - FalhaService com Banco Real (Teste)")
public class FalhaServiceIntegrationTest {

    private FalhaService falhaService;
    private EquipamentoService equipamentoService;

    /** SQL — Criação da tabela Falha */
    private static final String SQL_CREATE_TABLE_FALHA =
            """
            CREATE TABLE IF NOT EXISTS Falha (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                equipamentoId BIGINT NOT NULL,
                dataHoraOcorrencia DATETIME NOT NULL,
                descricao TEXT NOT NULL,
                criticidade VARCHAR(50) NOT NULL,
                status VARCHAR(50) NOT NULL,
                tempoParadaHoras DECIMAL(10,2) DEFAULT 0.00,
            
                CONSTRAINT chk_criticidade_falha CHECK (criticidade IN ('BAIXA','MEDIA','ALTA','CRITICA')),
                CONSTRAINT chk_status_falha CHECK (status IN ('ABERTA','EM_ANDAMENTO','RESOLVIDA'))
            );
            """;

    private static final String SQL_DROP_TABLE_FALHA = "DROP TABLE IF EXISTS Falha;";
    private static final String SQL_TRUNCATE_FALHA = "TRUNCATE TABLE Falha;";

    /** Também precisamos da tabela Equipamento pois há FK lógica */
    private static final String SQL_CREATE_TABLE_EQUIP =
            """
            CREATE TABLE IF NOT EXISTS Equipamento (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nome VARCHAR(255) NOT NULL,
                numeroDeSerie VARCHAR(100) NOT NULL UNIQUE,
                areaSetor VARCHAR(100) NOT NULL,
                statusOperacional VARCHAR(50) NOT NULL,
                CONSTRAINT chk_status_equip CHECK (statusOperacional IN ('OPERACIONAL', 'EM_MANUTENCAO', 'INATIVO'))
            );
            """;

    private static final String SQL_DROP_TABLE_EQUIP = "DROP TABLE IF EXISTS Equipamento;";
    private static final String SQL_TRUNCATE_EQUIP = "TRUNCATE TABLE Equipamento;";

    // -----------------------------------------------------------------------------------

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_DROP_TABLE_FALHA);
            stmt.execute(SQL_DROP_TABLE_EQUIP);

            stmt.execute(SQL_CREATE_TABLE_EQUIP);
            stmt.execute(SQL_CREATE_TABLE_FALHA);

            System.out.println("Tabelas 'Equipamento' e 'Falha' criadas.");
        }
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_DROP_TABLE_FALHA);
            stmt.execute(SQL_DROP_TABLE_EQUIP);

            System.out.println("Tabelas destruídas.");
        }
    }

    @BeforeEach
    void setupTest() throws Exception {
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_TRUNCATE_FALHA);
            stmt.execute(SQL_TRUNCATE_EQUIP);
        }

        // serviços reais
        equipamentoService = new EquipamentoServiceImpl();
        falhaService = new FalhaServiceImpl(); // alunos criam
    }

    // -----------------------------------------------------------------------------------

    @Test
    @DisplayName("Deve registrar falha (ABERTA) e atualizar equipamento para EM_MANUTENCAO se criticidade for CRITICA")
    void deveRegistrarFalhaCritica() throws SQLException {

        // ARRANGE — criar equipamento
        Long equipId;
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     INSERT INTO Equipamento
                     (nome, numeroDeSerie, areaSetor, statusOperacional)
                     VALUES ('Torno', 'SER123', 'USINAGEM', 'OPERACIONAL');
                     """,
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                equipId = keys.getLong(1);
            }
        }

        // falha crítica
        Falha falha = new Falha();
        falha.setEquipamentoId(equipId);
        falha.setDescricao("Motor travado");
        falha.setCriticidade("CRITICA");
        falha.setDataHoraOcorrencia(LocalDateTime.now());
        falha.setTempoParadaHoras(BigDecimal.ZERO);

        // ACT
        Falha falhaSalva = falhaService.registrarNovaFalha(falha);

        // ASSERT
        assertNotNull(falhaSalva.getId());
        assertEquals("ABERTA", falhaSalva.getStatus());

        // Verificar se equipamento foi atualizado para EM_MANUTENCAO
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT statusOperacional FROM Equipamento WHERE id = ?")) {

            stmt.setLong(1, equipId);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                assertEquals("EM_MANUTENCAO", rs.getString("statusOperacional"));
            }
        }
    }

    // -----------------------------------------------------------------------------------

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao registrar falha com equipamento inexistente")
    void deveLancarExcecaoQuandoEquipamentoNaoExiste() {

        Falha falha = new Falha();
        falha.setEquipamentoId(999L);
        falha.setDescricao("Falha sem equipamento");
        falha.setCriticidade("BAIXA");
        falha.setDataHoraOcorrencia(LocalDateTime.now());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> falhaService.registrarNovaFalha(falha)
        );

        assertEquals("Equipamento não encontrado!", ex.getMessage());
    }

    // -----------------------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar todas as falhas críticas ABERTAS")
    void deveBuscarFalhasCriticasAbertas() throws SQLException {

        Long equipId;

        // criar equipamento
        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     INSERT INTO Equipamento (nome, numeroDeSerie, areaSetor, statusOperacional)
                     VALUES ('Compressor', 'SERXYZ', 'AR', 'OPERACIONAL');
                     """,
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                equipId = keys.getLong(1);
            }
        }

        // inserir várias falhas
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {
            String sql = String.format("""
                INSERT INTO Falha (equipamentoId, dataHoraOcorrencia, descricao,
                                   criticidade, status, tempoParadaHoras)
                VALUES
                (%d, NOW(), 'CRÍTICA ABERTA 1', 'CRITICA', 'ABERTA', 2.0),
                (%d, NOW(), 'CRÍTICA ABERTA 2', 'CRITICA', 'ABERTA', 1.5),
                (%d, NOW(), 'CRÍTICA FECHADA', 'CRITICA', 'RESOLVIDA', 5.0),
                (%d, NOW(), 'BAIXA ABERTA', 'BAIXA', 'ABERTA', 0.5);
                """, equipId, equipId, equipId, equipId);

            stmt.execute(sql);
        }

        // ACT
        var lista = falhaService.buscarFalhasCriticasAbertas();

        // ASSERT
        assertEquals(2, lista.size());

        assertTrue(lista.stream().allMatch(f ->
                f.getCriticidade().equals("CRITICA")
                        && f.getStatus().equals("ABERTA")
        ));
    }
}
