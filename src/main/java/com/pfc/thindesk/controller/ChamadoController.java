package com.pfc.thindesk.controller;

import com.pfc.thindesk.api.ChamadoRequest;
import com.pfc.thindesk.api.ChamadoResponse;
import com.pfc.thindesk.api.ResolverRequest;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> criarChamado(
            @RequestBody ChamadoRequest request, Principal principal) {
        request.validar();
        Chamado chamado = new Chamado();
        chamado.setTitulo(request.getTitulo().trim());
        chamado.setDescricao(request.getDescricao().trim());
        chamado.setTipo(request.getTipo() == null ? null : request.getTipo().trim());
        chamado.setUsuario(principal.getName());
        chamado.setStatus("ABERTO");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChamadoResponse.de(chamadoService.criarChamado(chamado)));
    }

    @GetMapping("/usuario/{id}")
    public List<ChamadoResponse> listarPorUsuario(@PathVariable String id, Principal principal) {
        exigirDonoOuTecnico(id, principal);
        return chamadoService.buscarPorUsuario(id).stream()
                .map(ChamadoResponse::de).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscarPorId(@PathVariable String id, Principal principal) {
        Chamado chamado = chamadoService.buscarPorId(id);
        exigirDonoOuTecnico(chamado.getUsuario(), principal);
        return ChamadoResponse.de(chamado);
    }

    @PutMapping("/{id}/resolver")
    public ChamadoResponse resolver(@PathVariable String id,
            @RequestBody ResolverRequest request, Principal principal) {
        request.validar();
        if (!ehTecnico(principal)) {
            throw new AccessDeniedException("Apenas técnicos podem resolver chamados.");
        }
        return ChamadoResponse.de(chamadoService.resolverChamado(
                id, principal.getName(), request.getSolucao().trim()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> erroValidacao(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> chamadoNaoEncontrado(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> chamadoJaResolvido(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    private void exigirDonoOuTecnico(String usuarioId, Principal principal) {
        if (!usuarioId.equals(principal.getName()) && !ehTecnico(principal)) {
            throw new AccessDeniedException("Sem acesso a este chamado.");
        }
    }

    private boolean ehTecnico(Principal principal) {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TECHNICIAN"));
    }
}
