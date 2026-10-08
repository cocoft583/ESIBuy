package es.uclm.esibuy.backend.usuarios.infrastructure.adapter.out.persistence;

import es.uclm.esibuy.backend.usuarios.domain.model.Usuario;
import es.uclm.esibuy.backend.usuarios.domain.model.Email;
import es.uclm.esibuy.backend.usuarios.domain.model.Rol;
import es.uclm.esibuy.backend.usuarios.domain.model.TipoCliente;

/**
 * Convierte el {@code Usuario} del dominio en el documento que se guarda en MongoDB.
 *
 * <p>Es el único lugar donde se traduce entre el dominio y el documento de MongoDB.
 */
public class UsuarioDocumentMapper {

    public UsuarioDocument aDocumento(Usuario usuario) {
        TipoCliente tipoCliente = usuario.getTipoCliente();
        return new UsuarioDocument(
                usuario.getId(),
                usuario.getEmail().valor(),
                usuario.getContrasenaHash(),
                usuario.getRol().name(),
                usuario.getNombre(),
                usuario.getApellidos(),
                tipoCliente == null ? null : tipoCliente.name(),
                usuario.estaBloqueado()
        );
    }

    /**
     * Reconstruye el agregado de dominio sin filtrar anotaciones de MongoDB hacia el núcleo.
     */
    public Usuario aDominio(UsuarioDocument documento) {
        Usuario usuario = new Usuario(
                documento.getId(),
                documento.getNombre(),
                documento.getApellidos(),
                new Email(documento.getEmail()),
                documento.getPasswordHash(),
                Rol.valueOf(documento.getRol()),
                documento.getTipoCliente() == null
                        ? null
                        : TipoCliente.valueOf(documento.getTipoCliente())
        );
        if (documento.isBloqueado()) {
            usuario.bloquear();
        }
        return usuario;
    }
}
