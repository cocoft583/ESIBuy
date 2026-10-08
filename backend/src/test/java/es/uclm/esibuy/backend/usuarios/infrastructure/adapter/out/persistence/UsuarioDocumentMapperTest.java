package es.uclm.esibuy.backend.usuarios.infrastructure.adapter.out.persistence;

import es.uclm.esibuy.backend.usuarios.domain.model.Email;
import es.uclm.esibuy.backend.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Test unitario del mapper dominio -> documento de MongoDB (HU1, AB#216).
// PENDIENTE DE REVISAR CON EL EQUIPO: IDs de caso propuestos.
// El mapper mantiene separados el modelo de dominio y el documento de MongoDB, incluyendo
// los datos necesarios para reconstruir el usuario al consultar el listado.
class UsuarioDocumentMapperTest {

    @Test
    @DisplayName("HU1-CP02: el documento conserva el email, el hash de la contraseña y el rol")
    void convierteUsuarioEnDocumento() {
        Usuario usuario = Usuario.nuevoCliente(new Email("juan@uclm.es"), "hash-simulado");

        UsuarioDocument documento = new UsuarioDocumentMapper().aDocumento(usuario);

        assertEquals("juan@uclm.es", documento.getEmail());
        assertEquals("hash-simulado", documento.getPasswordHash());
        assertEquals("CLIENTE", documento.getRol());
    }
}
