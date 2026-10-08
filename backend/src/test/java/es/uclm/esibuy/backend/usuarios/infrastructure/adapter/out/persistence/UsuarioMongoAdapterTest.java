package es.uclm.esibuy.backend.usuarios.infrastructure.adapter.out.persistence;

import es.uclm.esibuy.backend.MongoTestcontainersConfig;
import es.uclm.esibuy.backend.usuarios.application.port.in.ListarUsuariosQuery;
import es.uclm.esibuy.backend.usuarios.domain.model.Email;
import es.uclm.esibuy.backend.usuarios.domain.model.Rol;
import es.uclm.esibuy.backend.usuarios.domain.model.Usuario;
import es.uclm.esibuy.backend.usuarios.domain.model.UsuarioPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test de integración del adaptador de MongoDB (HU1 y HU19).
// Usa un MongoDB real en Docker (Testcontainers): Docker Desktop debe estar abierto.
@DataMongoTest
@Import(MongoTestcontainersConfig.class)
class UsuarioMongoAdapterTest {

    @Autowired
    private UsuarioMongoRepository usuarioMongoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private UsuarioMongoAdapter adapter;

    @BeforeEach
    void preparar() {
        usuarioMongoRepository.deleteAll();
        adapter = new UsuarioMongoAdapter(
                usuarioMongoRepository,
                new UsuarioDocumentMapper(),
                mongoTemplate
        );
    }

    @Test
    @DisplayName("HU1-CP01: guardar persiste el usuario en la colección usuarios")
    void guardarPersisteElUsuarioEnLaColeccionUsuarios() {
        adapter.guardar(Usuario.nuevoCliente(new Email("juan@uclm.es"), "hash-simulado"));

        List<UsuarioDocument> guardados = usuarioMongoRepository.findAll();
        assertEquals(1, guardados.size());
        assertEquals("juan@uclm.es", guardados.get(0).getEmail());
        assertEquals("hash-simulado", guardados.get(0).getPasswordHash());
        assertEquals("CLIENTE", guardados.get(0).getRol());
    }

    @Test
    @DisplayName("HU1-CP03: existePorCorreo devuelve true si el email ya está guardado")
    void existePorCorreoDevuelveTrueSiEstaGuardado() {
        adapter.guardar(Usuario.nuevoCliente(new Email("juan@uclm.es"), "hash-simulado"));

        assertTrue(adapter.existePorCorreo("juan@uclm.es"));
    }

    @Test
    @DisplayName("HU1-CP03: existePorCorreo devuelve false si el email no está guardado")
    void existePorCorreoDevuelveFalseSiNoEstaGuardado() {
        assertFalse(adapter.existePorCorreo("juan@uclm.es"));
    }

    @Test
    @DisplayName("HU19-CP03: el adaptador busca por los campos visibles del usuario")
    void listarBuscaEnEmailNombreYApellidos() {
        usuarioMongoRepository.saveAll(List.of(
                new UsuarioDocument("1", "ana@esibuy.test", "hash-1", "CLIENTE",
                        "Ana", "García", "ESTANDAR", false),
                new UsuarioDocument("2", "bruno@esibuy.test", "hash-2", "VENDEDOR",
                        "Bruno", "López", null, false)
        ));

        UsuarioPage resultado = adapter.listar(
                new ListarUsuariosQuery("García", null, null, 0, 10)
        );

        assertEquals(1, resultado.totalElementos());
        assertEquals("ana@esibuy.test", resultado.usuarios().get(0).getEmail().valor());
    }

    @Test
    @DisplayName("HU19-CP04: el adaptador aplica el filtro por rol")
    void listarFiltraPorRol() {
        usuarioMongoRepository.saveAll(List.of(
                new UsuarioDocument("1", "ana@esibuy.test", "hash-1", "CLIENTE",
                        "Ana", "García", "ESTANDAR", false),
                new UsuarioDocument("2", "bruno@esibuy.test", "hash-2", "VENDEDOR",
                        "Bruno", "López", null, false)
        ));

        UsuarioPage resultado = adapter.listar(
                new ListarUsuariosQuery(null, Rol.VENDEDOR, null, 0, 10)
        );

        assertEquals(1, resultado.totalElementos());
        assertEquals(Rol.VENDEDOR, resultado.usuarios().get(0).getRol());
    }

    @Test
    @DisplayName("HU19-CP05: el adaptador aplica el filtro por estado de bloqueo")
    void listarFiltraPorBloqueo() {
        usuarioMongoRepository.saveAll(List.of(
                new UsuarioDocument("1", "ana@esibuy.test", "hash-1", "CLIENTE",
                        "Ana", "García", "ESTANDAR", false),
                new UsuarioDocument("2", "bruno@esibuy.test", "hash-2", "VENDEDOR",
                        "Bruno", "López", null, true)
        ));

        UsuarioPage resultado = adapter.listar(
                new ListarUsuariosQuery(null, null, true, 0, 10)
        );

        assertEquals(1, resultado.totalElementos());
        assertTrue(resultado.usuarios().get(0).estaBloqueado());
    }

    @Test
    @DisplayName("HU19-CP06: el adaptador devuelve la página solicitada y el total completo")
    void listarAplicaPaginacionYConservaElTotal() {
        usuarioMongoRepository.saveAll(List.of(
                new UsuarioDocument("1", "ana@esibuy.test", "hash-1", "CLIENTE",
                        "Ana", "García", "ESTANDAR", false),
                new UsuarioDocument("2", "bruno@esibuy.test", "hash-2", "VENDEDOR",
                        "Bruno", "López", null, false),
                new UsuarioDocument("3", "carla@esibuy.test", "hash-3", "CLIENTE",
                        "Carla", "Martín", "PREMIUM", false)
        ));

        UsuarioPage resultado = adapter.listar(
                new ListarUsuariosQuery(null, null, null, 1, 1)
        );

        assertEquals(1, resultado.pagina());
        assertEquals(1, resultado.tamano());
        assertEquals(1, resultado.usuarios().size());
        assertEquals(3, resultado.totalElementos());
    }

    @Test
    @DisplayName("HU19-CP07: la búsqueda trata los caracteres especiales como texto literal")
    void listarEscapaCaracteresEspecialesDeLaBusqueda() {
        usuarioMongoRepository.save(new UsuarioDocument(
                "1", "ana+vip@esibuy.test", "hash-1", "CLIENTE",
                "Ana", "García", "ESTANDAR", false
        ));

        UsuarioPage resultado = adapter.listar(
                new ListarUsuariosQuery("ana+", null, null, 0, 10)
        );

        assertEquals(1, resultado.totalElementos());
        assertEquals("ana+vip@esibuy.test", resultado.usuarios().get(0).getEmail().valor());
    }
}
