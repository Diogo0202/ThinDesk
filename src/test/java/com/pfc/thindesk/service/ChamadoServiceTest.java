package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.bson.Document;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ChamadoService chamadoService;

    private Chamado chamado;

    @BeforeEach
    void prepararChamado() {
        chamado = new Chamado();
        chamado.setId("ch-1");
        chamado.setTitulo("Impressora indisponível");
        chamado.setDescricao("Impressora sem conexão");
        chamado.setTipo("Hardware");
        chamado.setUsuario("ana");
    }

    @Test
    void abrirChamadoDefineEstadoEDataNoServidor() {
        LocalDateTime antes = LocalDateTime.now();
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chamado criado = chamadoService.criarChamado(chamado);

        LocalDateTime depois = LocalDateTime.now();
        assertEquals("ABERTO", criado.getStatus());
        assertEquals("Impressora indisponível", criado.getTitulo());
        assertEquals("ana", criado.getUsuario());
        assertNotNull(criado.getDataAbertura());
        assertFalse(criado.getDataAbertura().isBefore(antes));
        assertFalse(criado.getDataAbertura().isAfter(depois));
        assertNull(criado.getDataResolucao());
        assertNull(criado.getSolucao());
        verify(chamadoRepository).save(chamado);
    }

    @Test
    void aberturaRejeitaCamposObrigatoriosVaziosSemPersistir() {
        chamado.setDescricao("  ");

        assertThrows(IllegalArgumentException.class, () -> chamadoService.criarChamado(chamado));

        verifyNoInteractions(chamadoRepository);
    }

    @Test
    void consultaListaTodosEPesquisaPorSolicitante() {
        Chamado outro = new Chamado();
        outro.setId("ch-2");
        when(chamadoRepository.findAll()).thenReturn(List.of(chamado, outro));
        when(chamadoRepository.findByUsuario("ana")).thenReturn(List.of(chamado));

        assertEquals(2, chamadoService.listarChamados().size());
        assertEquals(List.of(chamado), chamadoService.buscarPorUsuario("ana"));

        verify(chamadoRepository).findAll();
        verify(chamadoRepository).findByUsuario("ana");
    }

    @Test
    void consultaPorIdRetornaChamadoExistente() {
        when(chamadoRepository.findById("ch-1")).thenReturn(Optional.of(chamado));

        assertSame(chamado, chamadoService.buscarPorId("ch-1"));
    }

    @Test
    void consultaPorIdInexistenteFalha() {
        when(chamadoRepository.findById("ausente")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> chamadoService.buscarPorId("ausente"));
    }

    @Test
    void resolverGravaTecnicoSolucaoEstadoEData() {
        prepararAtualizacaoMongo();
        LocalDateTime antes = LocalDateTime.now();

        Chamado resolvido = chamadoService.resolverChamado("ch-1", "tec-1", "Cabo substituído");

        LocalDateTime depois = LocalDateTime.now();
        assertEquals("RESOLVIDO", resolvido.getStatus());
        assertEquals("tec-1", resolvido.getTecnico());
        assertEquals("Cabo substituído", resolvido.getSolucao());
        assertNotNull(resolvido.getDataResolucao());
        assertFalse(resolvido.getDataResolucao().isBefore(antes));
        assertFalse(resolvido.getDataResolucao().isAfter(depois));
        verify(chamadoRepository, never()).save(any());
    }

    @Test
    void resolverExigeTecnicoESolucao() {
        assertThrows(IllegalArgumentException.class,
                () -> chamadoService.resolverChamado("ch-1", " ", "Concluído"));
        assertThrows(IllegalArgumentException.class,
                () -> chamadoService.resolverChamado("ch-1", "tec-1", " "));

        verifyNoInteractions(chamadoRepository);
    }

    @Test
    void chamadoResolvidoNaoPodeSerResolvidoNovamente() {
        chamado.setStatus("RESOLVIDO");
        when(chamadoRepository.findById("ch-1")).thenReturn(Optional.of(chamado));

        assertThrows(IllegalStateException.class,
                () -> chamadoService.resolverChamado("ch-1", "tec-1", "Reaberto"));

        verify(chamadoRepository, never()).save(any());
    }

    @Test
    void chamadoResolvidoNaoPodeSerEditado() {
        chamado.setStatus("RESOLVIDO");
        when(chamadoRepository.findById("ch-1")).thenReturn(Optional.of(chamado));

        assertThrows(IllegalStateException.class,
                () -> chamadoService.atualizarChamado("ch-1", chamado));

        verify(chamadoRepository, never()).save(any());
    }

    @Test
    void editarChamadoAbertoPreservaSolicitanteEstadoETimestamps() {
        LocalDateTime abertura = LocalDateTime.of(2026, 10, 1, 9, 0);
        chamado.setStatus("ABERTO");
        chamado.setDataAbertura(abertura);
        Chamado entrada = new Chamado();
        entrada.setDescricao("Descrição revisada");
        entrada.setTitulo("Título atualizado");
        entrada.setTipo("Software");
        entrada.setUsuario("intruso");
        entrada.setStatus("RESOLVIDO");
        entrada.setTecnico("tec-2");
        prepararAtualizacaoMongo();

        Chamado atualizado = chamadoService.atualizarChamado("ch-1", entrada);

        assertEquals("Descrição revisada", atualizado.getDescricao());
        assertEquals("Título atualizado", atualizado.getTitulo());
        assertEquals("Software", atualizado.getTipo());
        assertEquals("ana", atualizado.getUsuario());
        assertEquals("ABERTO", atualizado.getStatus());
        assertEquals(abertura, atualizado.getDataAbertura());
        assertNull(atualizado.getDataResolucao());
        assertNull(atualizado.getSolucao());
    }

    private void prepararAtualizacaoMongo() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chamado.class))).thenAnswer(invocation -> {
            Query filtro = invocation.getArgument(0);
            assertEquals("ch-1", filtro.getQueryObject().getString("_id"));
            assertTrue(((Document) filtro.getQueryObject().get("status")).containsKey("$not"));
            assertTrue(invocation.<FindAndModifyOptions>getArgument(2).isReturnNew());
            Update alteracoes = invocation.getArgument(1);
            Document campos = (Document) alteracoes.getUpdateObject().get("$set");
            if (campos.containsKey("descricao")) chamado.setDescricao(campos.getString("descricao"));
            if (campos.containsKey("titulo")) chamado.setTitulo(campos.getString("titulo"));
            if (campos.containsKey("tipo")) chamado.setTipo(campos.getString("tipo"));
            if (campos.containsKey("tecnico")) chamado.setTecnico(campos.getString("tecnico"));
            if (campos.containsKey("solucao")) chamado.setSolucao(campos.getString("solucao"));
            if (campos.containsKey("status")) chamado.setStatus(campos.getString("status"));
            if (campos.containsKey("dataResolucao")) chamado.setDataResolucao(campos.get("dataResolucao", LocalDateTime.class));
            assertFalse(campos.containsKey("usuario"));
            assertFalse(campos.containsKey("dataAbertura"));
            return chamado;
        });
    }
}
