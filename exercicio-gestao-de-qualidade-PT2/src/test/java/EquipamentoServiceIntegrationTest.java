import org.example.database.Conexao;
import org.example.model.Equipamento;
import org.example.service.equipamento.EquipamentoService;
import org.example.service.equipamento.EquipamentoServiceImpl;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Teste de Integração - EquipamentoService com Banco Real (Teste)")
public class EquipamentoServiceIntegrationTest {

    private EquipamentoService equipamentoService;

    // SQL para criar a tabela (fornecido por você)
    private static final String SQL_CREATE_TABLE =
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


    private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS Equipamento;";


    private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE Equipamento;";


    @BeforeAll
    static void setupDatabase() throws Exception {
        // 1. Conecta ao banco de TESTE
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            // 2. Destrói a tabela (caso exista de um teste anterior falho)
            stmt.execute(SQL_DROP_TABLE);

            // 3. Cria a tabela
            stmt.execute(SQL_CREATE_TABLE);

            System.out.println("Tabela 'produto' criada no banco de teste.");

        } catch (Exception e) {
            System.err.println("Erro ao configurar o banco de teste (BeforeAll)");
            e.printStackTrace();
            throw e; // Falha o setup se não conseguir criar a tabela
        }
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        // 4. Destrói a tabela ao final de TODOS os testes
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_DROP_TABLE);
            System.out.println("Tabela 'produto' destruída.");

        } catch (Exception e) {
            System.err.println("Erro ao limpar o banco de teste (AfterAll)");
            e.printStackTrace();
        }
    }

    @BeforeEach
    void setupTest() throws Exception {
        // 5. Limpa os dados da tabela ANTES de cada teste
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_TRUNCATE_TABLE);

        } catch (Exception e) {
            System.err.println("Erro ao limpar a tabela (BeforeEach)");
            e.printStackTrace();
        }

        // 6. Instancia o Service
        // Isso fará com que o Service crie seu Repositório,
        // que por sua vez usará ConexaoBanco.conectar()
        equipamentoService = new EquipamentoServiceImpl();
    }

    @Test
    @DisplayName("Deve cadastrar um equipamento e salvá-lo no banco")
    void deveCadastrarEquipamento() throws SQLException {

        var equipamento = new Equipamento(
                "FRESA",
                "CÓDIGOTESTE",
                "SETORTESTE"
        );
        Equipamento equipamentoNovo = equipamentoService.criarEquipamento(equipamento);

        assertNotNull(equipamentoNovo);

        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT  nome
                            ,numeroDeSerie
                            ,areaSetor
                            ,statusOperacional
                     FROM Equipamento WHERE id =
                     """ + equipamentoNovo.getId())) {

            assertTrue(rs.next());
            assertEquals("FRESA", rs.getString("nome"));
            assertEquals("CÓDIGOTESTE", rs.getString("numeroDeSerie"));
            assertEquals("SETORTESTE", rs.getString("areaSetor"));
            assertEquals("OPERACIONAL", rs.getString("statusOperacional"));
        }
    }

    @Test
    @DisplayName("Deve retornar equipamento por id")
    void deveBuscarEquipamentoPorId() throws SQLException {
        // 1. ARRANGE: Preparar o cenário (Inserir o dado e pegar o ID)
        String insertQuery = """
                INSERT INTO Equipamento
                (nome, numeroDeSerie, areaSetor, statusOperacional)
                VALUES
                ('TESTE', 'CÓDIGOTESTE', 'SETORTESTE', 'OPERACIONAL')
                """;

        Long idGerado = 0L;

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(insertQuery,
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    idGerado = generatedKeys.getLong(1);
                }
            }
        }
        Equipamento equipamentoEncontrado = equipamentoService.buscarEquipamentoPorId(idGerado);

        assertNotNull(equipamentoEncontrado, "O equipamento não deveria ser nulo");
        assertEquals(idGerado, equipamentoEncontrado.getId());
        assertEquals("TESTE", equipamentoEncontrado.getNome());
        assertEquals("SETORTESTE", equipamentoEncontrado.getAreaSetor());
        assertEquals("OPERACIONAL", equipamentoEncontrado.getStatusOperacional());
    }

    @Test
    @DisplayName("Deve retornar exception quando id nao existe")
    void deveRetornarExcepitonQuandoIdNaoExiste() throws SQLException {
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            equipamentoService.buscarEquipamentoPorId(999L);
        });

        assertEquals("Equipamento não encontrado!", e.getMessage());
    }
}
