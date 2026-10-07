package es.uclm.esibuy.backend.usuarios.application.service;

import es.uclm.esibuy.backend.usuarios.application.port.in.ListadoUsuariosUseCase;
import es.uclm.esibuy.backend.usuarios.application.port.in.ListarUsuariosQuery;
import es.uclm.esibuy.backend.usuarios.application.port.out.UsuarioRepositoryPort;
import es.uclm.esibuy.backend.usuarios.domain.model.EstadoUsuario;
import es.uclm.esibuy.backend.usuarios.domain.model.Rol;
import es.uclm.esibuy.backend.usuarios.domain.model.Usuario;
import es.uclm.esibuy.backend.usuarios.domain.model.UsuarioPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListadoUsuariosServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private ListadoUsuariosUseCase listadoUsuarios;

    @BeforeEach
    void setUp() {
        // En la fase Red de TDD, esta clase ListadoUsuariosService aún no existe
        // o sus métodos devuelven null/tiran excepciones.
        listadoUsuarios = new ListadoUsuariosService(usuarioRepository);
    }

    @Test
    void listar_delegaLaConsultaIntactaAlRepositorio() {
    ListarUsuariosQuery query = new ListarUsuariosQuery(
            "Ana", Rol.CLIENTE, EstadoUsuario.ACTIVO, 0, 10
    );
    UsuarioPage paginaEsperada = new UsuarioPage(List.of(), 0, 10, 0);

    when(usuarioRepository.listar(query)).thenReturn(paginaEsperada);

    UsuarioPage resultado = listadoUsuarios.listar(query);

    verify(usuarioRepository).listar(query);
    assertEquals(paginaEsperada, resultado);
}

@Test
void listar_devuelveLaPaginaProporcionadaPorElRepositorio() {
    ListarUsuariosQuery query = new ListarUsuariosQuery(null, null, null, 1, 20);
    Usuario usuario = new Usuario(
            "Ana",
            "ana@esibuy.test",
            Rol.CLIENTE,
            EstadoUsuario.ACTIVO,
            LocalDateTime.of(2026, 10, 1, 10, 0)
    );
    UsuarioPage paginaEsperada = new UsuarioPage(List.of(usuario), 1, 20, 1);

    when(usuarioRepository.listar(query)).thenReturn(paginaEsperada);

    UsuarioPage resultado = listadoUsuarios.listar(query);

    verify(usuarioRepository).listar(query);
    assertEquals(paginaEsperada, resultado);
}
}